// Copyright (c) 2020. Sailpoint Technologies, Inc. All rights reserved.
package cmd

import (
	"context"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler/mocks"
)

func TestListActions(t *testing.T) {
	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()

	mockRepo := mocks.NewMockActionRepo(mockCtrl)

	// Test data
	tenantID := "b5e85c44-65c6-451d-bf6f-494a1d272cbf"
	meta := map[string]interface{}{
		"testKey": "testVal",
	}

	listActionsWithMeta, err := NewListActions(tenantID, meta, 1, 1)
	if err != nil {
		t.Fatalf("error creating action with meta: %v", err)
	}
	listActionsWithoutMeta, err := NewListActions(tenantID, nil, 1, 1)

	if err != nil {
		t.Fatalf("error creating action without meta: %v", err)
	}

	// To list actions, List Actions command needs to find the action from the repo
	// Create actions that will be returned from the mock repo
	action1 := &scheduler.Action{
		TenantID: scheduler.TenantID(tenantID),
	}
	action2 := new(scheduler.Action)
	*action2 = *action1
	actions := []*scheduler.Action{action1, action2}

	// Set mock expectations
	mockRepo.
		EXPECT().
		FindAllByTenantIDAndMeta(gomock.Any(), gomock.Eq(scheduler.TenantID(tenantID)), gomock.Eq(meta), 1, 1).
		Return(actions, nil).
		Times(1)

	mockRepo.
		EXPECT().
		FindAllByTenantID(gomock.Any(), gomock.Eq(scheduler.TenantID(tenantID)), 1, 1).
		Return(actions, nil).
		Times(1)

	// Handle the action
	res, _ := listActionsWithMeta.Handle(context.Background(), mockRepo)
	res2, _ := listActionsWithoutMeta.Handle(context.Background(), mockRepo)

	if len(res) != len(actions) || len(res2) != len(actions) {
		t.Error("Number of actions retrieved is incorrect")
	}
}
