// Copyright (c) 2020. Sailpoint Technologies, Inc. All rights reserved.
package cmd

import (
	"context"
	"reflect"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler/mocks"
	"github.com/stretchr/testify/assert"
)

func TestHandleSuccess(t *testing.T) {
	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()

	mockRepo := mocks.NewMockActionRepo(mockCtrl)

	// Create some test data
	deadLine := time.Now()

	headers := make(map[string]string)
	headers["org"] = "nauto2"
	headers["pod"] = "echo"

	content := make(map[string]interface{})
	content["test"] = "value"

	event := scheduler.Event{
		Topic:   "identity",
		Type:    "SUNSET_IDENTITY",
		Content: content,
		Headers: headers,
	}

	const tenantID = "b5e85c44-65c6-451d-bf6f-494a1d272cbf"

	// Create a new CreateAction
	createAction, err := NewCreateAction(tenantID, deadLine, "", event, nil, "UTC", "")

	if err != nil {
		t.Fatalf("error creating action: %v", err)
	}

	// Set mock expectations
	mockRepo.
		EXPECT().
		Save(gomock.Any(), gomock.Any()).
		Times(1)

	// Handle the action
	action, _ := createAction.Handle(context.Background(), mockRepo)

	// Assertions
	if action.TenantID != tenantID {
		t.Error("action.TenantID not set correctly")
	}

	if reflect.DeepEqual(action.Deadline, deadLine) {
		t.Error("action.Deadline not set correctly")
	}

	if !reflect.DeepEqual(action.Event, event) {
		t.Error("action.Event not set correctly")
	}
}

func TestHandleSuccessWithCron(t *testing.T) {
	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()

	mockRepo := mocks.NewMockActionRepo(mockCtrl)

	// Create some test data
	deadLine := time.Now()

	headers := make(map[string]string)
	headers["org"] = "nauto2"
	headers["pod"] = "echo"

	content := make(map[string]interface{})
	content["test"] = "value"

	event := scheduler.Event{
		Topic:   "identity",
		Type:    "SUNSET_IDENTITY",
		Content: content,
		Headers: headers,
	}

	const tenantID = "b5e85c44-65c6-451d-bf6f-494a1d272cbf"

	// Create a new CreateAction
	createAction, err := NewCreateAction(tenantID, deadLine, "04 4 * * *", event, nil, "UTC", "")

	if err != nil {
		t.Fatalf("error creating action: %v", err)
	}

	// Set mock expectations
	mockRepo.
		EXPECT().
		Save(gomock.Any(), gomock.Any()).
		Times(1)

	// Handle the action
	action, _ := createAction.Handle(context.Background(), mockRepo)

	// Assertions
	if action.TenantID != tenantID {
		t.Error("action.TenantID not set correctly")
	}

	if reflect.DeepEqual(action.Deadline, deadLine) {
		t.Error("action.Deadline not set correctly")
	}

	if !reflect.DeepEqual(action.Event, event) {
		t.Error("action.Event not set correctly")
	}

}

func TestNewCreateAction(t *testing.T) {
	const tenantID = "b5e85c44-65c6-451d-bf6f-494a1d272cbf"

	tests := []struct {
		name                 string
		deadLine             time.Time
		cronExpression       string
		event                scheduler.Event
		expectedErr          bool
		expectedCreateAction *CreateAction
	}{
		{
			name:           "success: with deadline",
			deadLine:       time.Now(),
			cronExpression: "",
			event: scheduler.Event{
				Topic: "identity",
				Type:  "SUNSET_IDENTITY",
				Headers: map[string]string{
					"org": "bchan org",
					"pod": "echo",
				},
			},
			expectedErr: false,
		},
		{
			name:           "success: with cron",
			deadLine:       time.Time{},
			cronExpression: "04 4 * * *",
			event: scheduler.Event{
				Topic: "identity",
				Type:  "SUNSET_IDENTITY",
				Headers: map[string]string{
					"org": "bchan org",
					"pod": "echo",
				},
			},
			expectedErr: false,
		},
		{
			name:           "failure: both cron and deadline present, invalid cron",
			deadLine:       time.Now(),
			cronExpression: "04 4adfasd * * *",
			event: scheduler.Event{
				Topic: "identity",
				Type:  "SUNSET_IDENTITY",
				Headers: map[string]string{
					"org": "bchan org",
					"pod": "echo",
				},
			},
			expectedErr: true,
		},
		{
			name:           "failure: no deadline or cron",
			deadLine:       time.Time{},
			cronExpression: "",
			event: scheduler.Event{
				Topic: "identity",
				Type:  "SUNSET_IDENTITY",
				Headers: map[string]string{
					"org": "bchan org",
					"pod": "echo",
				},
			},
			expectedErr: true,
		},
		{
			name:           "failure: no topic",
			deadLine:       time.Time{},
			cronExpression: "04 4 * * *",
			event: scheduler.Event{
				Topic: "",
				Type:  "SUNSET_IDENTITY",
				Headers: map[string]string{
					"org": "bchan org",
					"pod": "echo",
				},
			},
			expectedErr: true,
		},
		{
			name:           "failure: no tyoe",
			deadLine:       time.Time{},
			cronExpression: "04 4 * * *",
			event: scheduler.Event{
				Topic: "SUNSET_IDENTITY",
				Type:  "",
				Headers: map[string]string{
					"org": "bchan org",
					"pod": "echo",
				},
			},
			expectedErr: true,
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(tt *testing.T) {
			action, err := NewCreateAction(tenantID, test.deadLine, test.cronExpression, test.event, nil, "UTC", "")
			assert.Equal(tt, test.expectedErr, err != nil)
			if action != nil {
				assert.Equal(tt, scheduler.TenantID(tenantID), action.tenantID)
				assert.Equal(tt, test.cronExpression, action.cronString)
				assert.Equal(tt, test.event, action.event)
			}
		})
	}

}
