// Copyright (c) 2020. Sailpoint Technologies, Inc. All rights reserved.
package infra

import (
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
	"strings"
	"time"

	"github.com/gorhill/cronexpr"
	"github.com/jackc/pgx/v4"
	"github.com/jackc/pgx/v4/pgxpool"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promauto"
	"github.com/sailpoint/atlas-go/atlas/event"
	"github.com/sailpoint/atlas-go/atlas/log"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler/util"
	"go.uber.org/zap"
)

var triggeredActionDelayHistogram = promauto.NewHistogram(prometheus.HistogramOpts{
	Name: "triggered_action_delay",
	Help: "A histogram for delay between scheduled action deadline and actual time action is scheduled.",
})

var actionDbDuration = promauto.NewHistogramVec(prometheus.HistogramOpts{
	Name: "action_db_duration",
	Help: "A histogram to measure latency of database calls.",
},
	[]string{"op"},
)

var publishFailedCounter = promauto.NewCounter(prometheus.CounterOpts{
	Name: "event_publish_failed",
})

const actionColumns = `
	id,
	created,
	deadline,
	tenant_id,
	event_topic,
	event_type,
	event_header_json,
	event_content_json,
	meta,
	retry,
	cron_string,
	timezone_location,
	timezone_offset
`

const countSQL = `
	SELECT
		count(id)
	FROM
		sps_action
`

var listByTenantSQL = addColumns(`
	SELECT
		$columns
	FROM
		sps_action
	WHERE
		tenant_id = $1
	LIMIT
		$2
	OFFSET
		$3
`)

var listByMetaSQL = addColumns(`
	SELECT
		$columns
	FROM
		sps_action
	WHERE
		tenant_id = $1
	AND
		meta @> $2
	LIMIT
		$3
	OFFSET
		$4
`)

var selectSQL = addColumns(`
	SELECT
		$columns
	FROM
		sps_action
	WHERE
		id=$1
`)

var insertSQL = addColumns(`
	INSERT INTO sps_action(
		$columns
	) VALUES (
		$1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13
	)
`)

const updateDeadlineAndRetrySQL = `
	UPDATE 
		sps_action
	SET
		deadline = $2, retry = $3
	WHERE
		id=$1
`

const deleteByTenantSQL = `
	DELETE FROM
		sps_action
	WHERE
		tenant_id = $1
`

const deleteSQL = `
	DELETE FROM
		sps_action
	WHERE
		id = $1
`

const deleteByMetaSQL = `
	DELETE FROM
		sps_action
	WHERE
		tenant_id = $1
	AND
		meta @> $2
`

var selectPastDeadlineSQL = addColumns(`
	SELECT
		$columns
	FROM
		sps_action
	WHERE
		deadline < $1
	AND
		retry <= $3
	FOR UPDATE SKIP LOCKED
	LIMIT $2
`)

// actionRepo is an implementation of scheduler.ActionRepo that uses a SQL database backend.
type actionRepo struct {
	pool *pgxpool.Pool
}

// scannable is an interface for types that can be scanned from.
type scannable interface {
	Scan(dest ...interface{}) error
}

// NewActionRepo constructs a new action repo.
func NewActionRepo(pool *pgxpool.Pool) scheduler.ActionRepo {
	r := &actionRepo{}
	r.pool = pool
	return r
}

// FindByID gets the action with the specified ID.
func (r *actionRepo) FindByID(ctx context.Context, id scheduler.ActionID) (*scheduler.Action, error) {

	a, err := scanAction(r.pool.QueryRow(ctx, selectSQL, id))

	if err == pgx.ErrNoRows {
		return nil, nil
	}

	if err != nil {
		return nil, err
	}

	return a, nil
}

// Save persists a new action to the database.
func (r *actionRepo) Save(ctx context.Context, a *scheduler.Action) error {
	timeStart := time.Now()
	defer func() {
		delay := time.Since(timeStart).Milliseconds()
		actionDbDuration.WithLabelValues("save").Observe(float64(delay))
	}()

	headerJSON, err := json.Marshal(&a.Event.Headers)
	if err != nil {
		return err
	}

	contentJSON, err := json.Marshal(&a.Event.Content)
	if err != nil {
		return err
	}

	metaJSON, err := json.Marshal(&a.Meta)
	if err != nil {
		return err
	}

	_, err = r.pool.Exec(ctx, insertSQL,
		a.ID, time.Now().UTC(), a.Deadline.UTC(), a.TenantID, a.Event.Topic, a.Event.Type, headerJSON, contentJSON, metaJSON, a.Retry, a.CronString, a.TimezoneLocation, a.TimezoneOffset)

	if err != nil {
		return err
	}

	return nil
}

// DeleteByID deletes the specified action.
func (r *actionRepo) DeleteByID(ctx context.Context, id scheduler.ActionID) (bool, error) {
	timeStart := time.Now()
	defer func() {
		delay := time.Since(timeStart).Milliseconds()
		actionDbDuration.WithLabelValues("deleteById").Observe(float64(delay))
	}()

	res, err := r.pool.Exec(ctx, deleteSQL, id)
	if err != nil {
		return false, err
	}

	count := res.RowsAffected()

	return count > 0, nil
}

// CountAll returns the total number of actions in the database.
func (r *actionRepo) CountAll(ctx context.Context) (int64, error) {
	timeStart := time.Now()
	defer func() {
		delay := time.Since(timeStart).Milliseconds()
		actionDbDuration.WithLabelValues("countAll").Observe(float64(delay))
	}()

	var count int64
	if err := r.pool.QueryRow(ctx, countSQL).Scan(&count); err != nil {
		return 0, err
	}

	return count, nil
}

// DeleteAllByTenantID deletes all actions related to the specified tenant.
func (r *actionRepo) DeleteAllByTenantID(ctx context.Context, tenantID scheduler.TenantID) (int64, error) {
	timeStart := time.Now()
	defer func() {
		delay := time.Since(timeStart).Milliseconds()
		actionDbDuration.WithLabelValues("deleteAllByTenantId").Observe(float64(delay))
	}()

	res, err := r.pool.Exec(ctx, deleteByTenantSQL, tenantID)
	if err != nil {
		return 0, err
	}

	count := res.RowsAffected()

	return count, nil
}

// TriggerActionsPastDeadline reads all actions that are past their deadline, publishes their corresponding events to
// an eventPublisher implementation, and either updates or deletes them depending on whether they're cron actions
// or one-off actions
func (r *actionRepo) TriggerActionsPastDeadline(ctx context.Context, deadline time.Time, max int64, retryLimit int, eventPublisher scheduler.EventPublisher) (int64, error) {
	timeStart := time.Now()

	tx, err := r.pool.BeginTx(ctx, pgx.TxOptions{
		IsoLevel: pgx.ReadCommitted,
	})
	if err != nil {
		log.Error(ctx, "failed to begin tx", err)
		return 0, err
	}

	defer func() {
		err := tx.Rollback(ctx)
		if err != nil && err != pgx.ErrTxClosed {
			log.Errorf(ctx, "failed to rollback tx", err)
		}
	}()

	rows, err := tx.Query(ctx, selectPastDeadlineSQL, deadline.UTC(), max, retryLimit)
	if err != nil {
		return 0, err
	}
	defer rows.Close()

	actions := make([]*scheduler.Action, 0)
	for rows.Next() {
		a, scanActionErr := scanAction(rows)
		if scanActionErr != nil {
			return 0, scanActionErr
		}

		actions = append(actions, a)
	}

	if len(actions) == 0 {
		return 0, nil
	}

	// Setup a batch for batch write
	b := &pgx.Batch{}

	if len(actions) > 0 {
		// Publish events
		failedActions, _ := eventPublisher.BulkPublish(ctx, actions)
		publishTime := time.Now()
		failedActionsMap := make(map[scheduler.ActionID]*scheduler.Action)

		if len(failedActions) > 0 {
			for actionIndex := range failedActions {
				action := failedActions[actionIndex]
				failedActionsMap[action.ID] = &action
			}
			log.Errorf(ctx, "error publishing %d events to kafka: %v", len(failedActions), err)
			publishFailedCounter.Add(float64(len(failedActions)))
		}

		for _, action := range actions {
			tenantCtx := log.WithFields(ctx,
				zap.String("pod", action.Event.Headers[event.HeaderKeyPod]),
				zap.String("org", action.Event.Headers[event.HeaderKeyOrg]),
				zap.String("request_id", action.Event.Headers[event.HeaderKeyRequestID]),
			)
			// if the publish did not fail, queue up the next action if it is cron, delete the action if not cron
			// else go for retry
			if _, ok := failedActionsMap[action.ID]; !ok {
				triggeredActionDelayHistogram.Observe(float64(publishTime.Sub(action.Deadline).Milliseconds()))
				if action.IsCron() {
					nextTime, err := evaluateNextTime(action)
					if err != nil {
						log.Error(tenantCtx, err)
						continue
					}
					log.Infof(tenantCtx, "queueing up next cron action for actionID: %v with cron: %v. next schedule: %v", action.ID, action.CronString, nextTime)
					b.Queue(updateDeadlineAndRetrySQL, action.ID, nextTime, 0)
				} else {
					b.Queue(deleteSQL, action.ID)
				}
			} else {
				// retry special case - if action is cron and at retry limit then queue up the action for next cron time
				if action.IsCron() && action.Retry >= retryLimit {
					nextTime, err := evaluateNextTime(action)
					if err != nil {
						log.Error(tenantCtx, err)
						continue
					}
					log.Infof(tenantCtx, "out of retries for actionID: %v. Scheduling next interval.", action.ID)
					b.Queue(updateDeadlineAndRetrySQL, action.ID, nextTime, 0)
					continue
				}

				// increment retry
				log.Infof(tenantCtx, "incrementing retry for actionID: %v to %d", action.ID, action.Retry+1)
				b.Queue(updateDeadlineAndRetrySQL, action.ID, action.Deadline, action.Retry+1)
			}
		}

		batchResults := tx.SendBatch(ctx, b)
		err = nil
		for err == nil {
			_, err = batchResults.Exec()
			if err != nil {
				if err.Error() == "no result" {
					break
				}
				log.Errorf(ctx, "failed to insert row %s", err)
				return 0, err
			}
		}

		err := batchResults.Close()
		if err != nil {
			log.Errorf(ctx, "failed to close batch result", err)
			return 0, err
		}

		if err := tx.Commit(ctx); err != nil {
			log.Errorf(ctx, "error committing transaction %v", err)
			return 0, err
		}

		totalTimeSeconds := time.Since(timeStart).Milliseconds()
		numSuccessfulActions := len(actions) - len(failedActions)
		log.Infof(ctx, "successfully Triggered Actions: %v | reinserted Failed Actions: %v | total time: %vms",
			numSuccessfulActions, len(failedActions), totalTimeSeconds)
		return int64(numSuccessfulActions), nil
	}

	return 0, nil
}

// DeleteAllByTenantIDAndMeta deletes all actions for the specified tenant that match the meta query.
func (r *actionRepo) DeleteAllByTenantIDAndMeta(ctx context.Context, tenantID scheduler.TenantID, meta map[string]interface{}) (int64, error) {
	metaJSON, err := json.Marshal(meta)
	if err != nil {
		return 0, err
	}

	res, err := r.pool.Exec(ctx, deleteByMetaSQL, tenantID, metaJSON)
	if err != nil {
		return 0, err
	}

	count := res.RowsAffected()

	return count, nil
}

// FindAllByTenantID finds all actions for the specified tenant.
func (r *actionRepo) FindAllByTenantID(ctx context.Context, tenantID scheduler.TenantID, limit int, offset int) ([]*scheduler.Action, error) {
	timeStart := time.Now()
	defer func() {
		delay := time.Since(timeStart).Milliseconds()
		actionDbDuration.WithLabelValues("findAllByTenantId").Observe(float64(delay))
	}()

	return r.queryActions(ctx, listByTenantSQL, tenantID, limit, offset)
}

// FindAllByTenantIDAndMeta finds all actions for the specified tenant that match the meta query.
func (r *actionRepo) FindAllByTenantIDAndMeta(ctx context.Context, tenantID scheduler.TenantID, meta map[string]interface{}, limit int, offset int) ([]*scheduler.Action, error) {
	timeStart := time.Now()
	defer func() {
		delay := time.Since(timeStart).Milliseconds()
		actionDbDuration.WithLabelValues("findAllByTenantIdAndMeta").Observe(float64(delay))
	}()

	metaJSON, err := json.Marshal(meta)
	if err != nil {
		return nil, err
	}

	return r.queryActions(ctx, listByMetaSQL, tenantID, metaJSON, limit, offset)
}

// queryActions runs the specified SQL query and maps the resulting rows into actions.
func (r *actionRepo) queryActions(ctx context.Context, query string, args ...interface{}) ([]*scheduler.Action, error) {
	rows, err := r.pool.Query(ctx, query, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	return scanActions(rows)
}

// scanAction scans a new action from a scannable.
func scanAction(s scannable) (*scheduler.Action, error) {
	var headerJSON sql.NullString
	var contentJSON sql.NullString
	var metaJSON sql.NullString
	var cronSTRING sql.NullString
	var timezoneLocation sql.NullString
	var timezoneOffset sql.NullString

	a := scheduler.Action{}
	if err := s.Scan(&a.ID, &a.Created, &a.Deadline, &a.TenantID, &a.Event.Topic, &a.Event.Type, &headerJSON, &contentJSON, &metaJSON, &a.Retry, &cronSTRING, &timezoneLocation, &timezoneOffset); err != nil {
		return nil, err
	}

	if headerJSON.Valid {
		if err := json.Unmarshal([]byte(headerJSON.String), &a.Event.Headers); err != nil {
			return nil, err
		}
	}

	if contentJSON.Valid {
		if err := json.Unmarshal([]byte(contentJSON.String), &a.Event.Content); err != nil {
			return nil, err
		}
	}

	if metaJSON.Valid {
		if err := json.Unmarshal([]byte(metaJSON.String), &a.Meta); err != nil {
			return nil, err
		}
	}

	if cronSTRING.Valid {
		a.CronString = cronSTRING.String
	}

	if timezoneOffset.Valid {
		a.TimezoneOffset = timezoneOffset.String
	}

	if timezoneLocation.Valid {
		a.TimezoneLocation = timezoneLocation.String
	}

	return &a, nil
}

// scanActions scans a slice of actions from SQL rows.
func scanActions(rows pgx.Rows) ([]*scheduler.Action, error) {
	actions := make([]*scheduler.Action, 0)

	for rows.Next() {
		a, err := scanAction(rows)
		if err != nil {
			return nil, err
		}

		actions = append(actions, a)
	}

	return actions, nil
}

// addColumns adds the action columns to the specified query. The query text $columns is replaced
// with the action columns csv.
func addColumns(query string) string {
	return strings.ReplaceAll(query, "$columns", actionColumns)
}

func evaluateNextTime(action *scheduler.Action) (*time.Time, error) {
	expr, err := cronexpr.Parse(action.CronString)
	if err != nil {
		return nil, fmt.Errorf("invalid cron expression found in db: %s", action.CronString)
	}

	nextTime, err := util.GetNextCronValueUsingTimezone(time.Now().UTC(), action.TimezoneLocation, action.TimezoneOffset, expr)
	if err != nil {
		return nil, fmt.Errorf("unable to calculate next scheduled run: %s | timezone location: %s |  offset: %s", action.CronString, action.TimezoneLocation, action.TimezoneOffset)
	}
	return &nextTime, nil
}
