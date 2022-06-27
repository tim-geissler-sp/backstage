//go:build integration
// +build integration

package infra

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/gorhill/cronexpr"
	"github.com/jackc/pgx/v4/pgxpool"
	"github.com/sailpoint/atlas-go/atlas/config"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler/cmd"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler/mocks"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type ActionRepoSuite struct {
	suite.Suite
	testDb    *pgxpool.Pool
	testEvent scheduler.Event
}

func (suite *ActionRepoSuite) SetupTest() {
	db, err := NewPGXPool(context.Background(), config.NewSource())
	if err != nil {
		suite.T().Fatalf("couldn't get db connection: %s", err)
	}
	suite.testDb = db

	suite.testEvent = scheduler.Event{
		Topic:   "workflow",
		Type:    "WORKFLOW_EXECUTED",
		Content: make(map[string]interface{}),
		Headers: map[string]string{
			"pod": "pod",
			"org": "org",
		},
	}
}

func (suite *ActionRepoSuite) TearDownTest() {
	// clear database after every test
	_, err := suite.testDb.Exec(context.Background(), "TRUNCATE TABLE sps_action")
	if err != nil {
		suite.T().Fatalf("error truncating table: %s", err)
	}
}

func TestActionRepoSuite(t *testing.T) {
	suite.Run(t, new(ActionRepoSuite))
}

func (suite *ActionRepoSuite) TestDeleteAllPastDeadline_HappyPath() {
	t := suite.T()
	mockCtrl := gomock.NewController(t)
	mockPublisher := mocks.NewMockEventPublisher(mockCtrl)

	ctx := context.Background()
	db := suite.testDb

	actionRepo := NewActionRepo(db)

	// create an action
	tenantID := "abcd-1234"
	deadline := time.Now()
	cronString := "* */1 * * *"
	event := suite.testEvent

	createAction, _ := cmd.NewCreateAction(tenantID, deadline, cronString, event, nil, "UTC", "")
	_, _ = createAction.Handle(context.Background(), actionRepo)

	mockPublisher.
		EXPECT().
		BulkPublish(gomock.Any(), gomock.Any()).
		Times(1)

	numInitialActions, _ := actionRepo.CountAll(ctx)
	assert.Equal(t, int64(1), numInitialActions)

	numActionsProcessed, _ := actionRepo.TriggerActionsPastDeadline(ctx, time.Now().UTC(), 10, 3, mockPublisher)
	assert.Equal(t, int64(1), numActionsProcessed)

	numAfterActions, _ := actionRepo.CountAll(ctx)
	assert.Equal(t, int64(1), numAfterActions)

}

func (suite *ActionRepoSuite) TestDeleteAllPastDeadline_PublishFails() {
	t := suite.T()
	mockCtrl := gomock.NewController(t)
	mockEventPublisher := mocks.NewMockEventPublisher(mockCtrl)

	ctx := context.Background()
	db := suite.testDb

	actionRepo := NewActionRepo(db)

	maxRetries := 3

	// create an action
	tenantID := "abcd-1234"
	deadline := time.Now()
	cronString := "10 */1 */1 * *"
	event := suite.testEvent

	createAction, _ := cmd.NewCreateAction(tenantID, deadline, cronString, event, nil, "UTC", "")
	createdAction, _ := createAction.Handle(context.Background(), actionRepo)

	actions := make([]*scheduler.Action, 0)
	actions = append(actions, createdAction)

	mockEventPublisher.
		EXPECT().
		BulkPublish(gomock.Any(), gomock.Any()).
		DoAndReturn(func(_ context.Context, _ []*scheduler.Action) ([]scheduler.Action, error) {
			failedActions := make([]scheduler.Action, len(actions))
			failedActions[0] = *actions[0]
			return failedActions, errors.New("one or more event failed to send")
		})

	numSuccessfulActions, processingError := actionRepo.TriggerActionsPastDeadline(ctx, time.Now().UTC(), 10, maxRetries, mockEventPublisher)
	assert.Equal(t, int64(0), numSuccessfulActions)
	assert.Nil(suite.T(), processingError)

	actions, _ = actionRepo.FindAllByTenantID(ctx, actions[0].TenantID, 3, 0)
	assert.Equal(t, 1, len(actions), "expected 1 action")
	assert.NotNil(t, actions[0])
	assert.Equal(t, 1, actions[0].Retry, "expected Retry=1")

	for i := 0; i < 3; i++ {
		// we have already failed once, and expectedRetry will be ahead by 1, so we should add 2
		expectedRetry := i + 2
		if expectedRetry > maxRetries {
			expectedRetry = 0
		}

		// Set expectations
		actions, _ = actionRepo.FindAllByTenantID(ctx, actions[0].TenantID, 3, 0)
		mockEventPublisher.
			EXPECT().
			BulkPublish(gomock.Any(), gomock.Any()).
			DoAndReturn(func(_ context.Context, _ []*scheduler.Action) ([]scheduler.Action, error) {
				failedActions := make([]scheduler.Action, len(actions))
				failedActions[0] = *actions[0]
				return failedActions, errors.New("one or more event failed to send")
			})

		// Run the Delete/Publish/Save routine, and fail
		_, _ = actionRepo.TriggerActionsPastDeadline(ctx, time.Now().UTC(), 10, maxRetries, mockEventPublisher)

		// Run through assert with saved actions
		actions, _ = actionRepo.FindAllByTenantID(ctx, actions[0].TenantID, 3, 0)
		assert.Equal(t, expectedRetry, actions[0].Retry, "expected Retry=%d, but was %d", expectedRetry, actions[0].Retry)
	}

	// Expect after maxRetries cron-Action is scheduled for next time based on period
	expr, _ := cronexpr.Parse(cronString)
	nextTime := expr.Next(time.Now()).UTC()
	actions, _ = actionRepo.FindAllByTenantID(ctx, actions[0].TenantID, 3, 0)
	assert.NotNil(t, actions[0])
	assert.Equal(t, 1, len(actions), "expected 1 action")
	assert.Equal(t, 0, actions[0].Retry, "expected Retry=0, but was %d", actions[0].Retry)
	assert.Equal(t, nextTime.UTC(), actions[0].Deadline.UTC(), "expected Deadline=%s was %s", nextTime.UTC(), actions[0].Deadline.UTC())
}
