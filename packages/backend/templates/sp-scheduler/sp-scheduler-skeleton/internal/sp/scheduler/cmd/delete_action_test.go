// Copyright (c) 2020. Sailpoint Technologies, Inc. All rights reserved.
package cmd

import (
	"context"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/google/uuid"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler/mocks"
)

func TestDeleteAction(t *testing.T) {
	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()

	mockRepo := mocks.NewMockActionRepo(mockCtrl)

	// Test data
	actionID := "b20c761d-bfad-4ab6-bd86-e4e9a7a250bb"
	const tenantID = "b5e85c44-65c6-451d-bf6f-494a1d272cbf"

	deleteAction, _ := NewDeleteAction(tenantID, actionID)

	// To delete an action, Delete Action command needs to find the action from the repo
	// Create an action that will be returned from the mock repo
	action := &scheduler.Action{
		ID:       scheduler.ActionID(uuid.MustParse(actionID)),
		TenantID: scheduler.TenantID(tenantID),
	}

	// Set mock expectations
	mockRepo.
		EXPECT().
		FindByID(gomock.Any(), gomock.Any()).
		Return(action, nil).
		Times(1)

	mockRepo.
		EXPECT().
		DeleteByID(gomock.Any(), gomock.Eq(scheduler.ActionID(uuid.MustParse(actionID)))).
		Return(true, nil).
		Times(1)

	// Handle the action
	res, _ := deleteAction.Handle(context.Background(), mockRepo)

	// Assertions
	if res != true {
		t.Error("incorrect result from deleteAction.Handle")
	}
}
