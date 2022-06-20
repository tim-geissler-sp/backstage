// Copyright (c) 2020. Sailpoint Technologies, Inc. All rights reserved.
package infra

import (
	"context"
	"fmt"
	"net/url"
	"time"

	"github.com/jackc/pgx/v4/pgxpool"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promauto"
	"github.com/sailpoint/atlas-go/atlas/application"
	"github.com/sailpoint/atlas-go/atlas/config"
	"github.com/sailpoint/atlas-go/atlas/log"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler/app"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler/cmd"
	"golang.org/x/sync/errgroup"
)

const defaultMaxConnections int64 = 20

// SchedulerService is a type that owns application state for the microservice.
type SchedulerService struct {
	*application.Application
	app app.App
}

// NewSchedulerService constructs a new SchedulerService.
func NewSchedulerService() (*SchedulerService, error) {
	application, err := application.New("sp-scheduler")
	if err != nil {
		return nil, err
	}

	_, err = application.ConnectDB()
	if err != nil {
		return nil, err
	}

	pool, err := NewPGXPool(context.Background(), application.Config)
	if err != nil {
		log.Global().Sugar().Fatalf("failed to connect to db: %v", err)
	}

	s := &SchedulerService{}
	s.Application = application
	s.app = app.NewApp(NewActionRepo(pool), NewPublisher(application.EventPublisher))

	return s, nil
}

// NewPGXPool constructs a new pgx pool which is a connection pool with handles to postgres
func NewPGXPool(ctx context.Context, cfg config.Source) (*pgxpool.Pool, error) {
	host := config.GetString(cfg, "ATLAS_DB_HOST", "localhost")
	database := config.GetString(cfg, "ATLAS_DB_NAME", "postgres")
	user := config.GetString(cfg, "ATLAS_DB_USER", "postgres")
	password := config.GetString(cfg, "ATLAS_DB_PASSWORD", "2thecloud")
	maxConns := config.GetInt64(cfg, "MAX_DB_CONNECTIONS", defaultMaxConnections) // ideally this value is set from environment variable

	connString := fmt.Sprintf("postgres://%s:%s@%s/%s?sslmode=disable", url.PathEscape(user), url.PathEscape(password), host, database)
	config, err := pgxpool.ParseConfig(connString)
	if err != nil {
		panic(fmt.Sprintf("pgsql connection parse error [%s]: %s", connString, err.Error()))
	}
	config.MaxConns = int32(maxConns)
	return pgxpool.ConnectConfig(ctx, config)
}

// Run starts all of the microservice processes and waits until an OS interrupt to perform a clean shutdown.
func (s *SchedulerService) Run() error {
	ctx, done := context.WithCancel(context.Background())

	g, ctx := errgroup.WithContext(ctx)
	g.Go(func() error { return s.StartBeaconHeartbeat(ctx) })
	g.Go(func() error { return s.StartEventConsumer(ctx, s.bindEventHandlers()) })
	g.Go(func() error { return s.StartMetricsServer(ctx) })
	g.Go(func() error { return s.StartWebServer(ctx, s.buildRoutes()) })
	g.Go(func() error { return s.triggerActionsPastDeadline(ctx) })
	g.Go(func() error { return s.updateMetrics(ctx) })
	g.Go(func() error { return s.WaitForInterrupt(ctx, done) })

	if err := g.Wait(); err != nil && err != context.Canceled {
		return err
	}

	return nil
}

// updateMetrics runs periodic queries on the dataset and updates metrics gauges.
func (s *SchedulerService) updateMetrics(ctx context.Context) error {
	actionCountGauge := promauto.NewGauge(prometheus.GaugeOpts{
		Name: "action_count",
	})

	for {
		if actionCount, err := s.app.CountActions(ctx); err == nil {
			actionCountGauge.Set(float64(actionCount))
		} else {
			log.Warnf(ctx, "error counting actions: %v", err)
		}

		select {
		case <-ctx.Done():
			return ctx.Err()
		case <-time.After(5 * time.Minute):
		}
	}
}

// triggerActionsPastDeadline is a background task that periodically scans the database
// for actions that are past their deadlines and fires their respective events.
func (s *SchedulerService) triggerActionsPastDeadline(ctx context.Context) error {
	triggerCounts := promauto.NewCounter(prometheus.CounterOpts{
		Name: "triggered_action_count",
	})

	triggerFailureCount := promauto.NewCounter(prometheus.CounterOpts{
		Name: "triggered_action_error_count",
	})

	batchSize := config.GetInt64(s.Config, "SPS_BATCH_SIZE", 2000)
	interval := config.GetDuration(s.Config, "SPS_BATCH_INTERVAL", 10*time.Second)
	retryLimit := config.GetInt(s.Config, "SPS_RETRY_LIMIT", 3)

	for {
		timeStart := time.Now()
		cmd, err := cmd.NewTriggerActionsPastDeadline(batchSize, retryLimit)
		if err != nil {
			log.Errorf(ctx, "error building actions command: %v", err)
		}

		count, err := s.app.TriggerActionsPastDeadline(ctx, *cmd)
		if err != nil {
			triggerFailureCount.Inc()
			log.Errorf(ctx, "error triggering actions in background: %v", err)
		} else if count > 0 {
			triggerCounts.Add(float64(count))
			log.Infof(ctx, "triggered %d actions in %v ms", count, time.Since(timeStart).Milliseconds())
		}

		select {
		case <-ctx.Done():
			return ctx.Err()
		case <-time.After(interval):
		}
	}
}
