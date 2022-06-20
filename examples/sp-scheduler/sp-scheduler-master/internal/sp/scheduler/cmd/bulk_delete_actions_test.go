// Copyright (c) 2020. Sailpoint Technologies, Inc. All rights reserved.
package cmd

import (
	"context"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler/mocks"
	"github.com/stretchr/testify/assert"
)

type Payload string

func TestBulkDeleteActions(t *testing.T) {
	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()

	mockRepo := mocks.NewMockActionRepo(mockCtrl)

	// Test data
	const tenantID = "b5e85c44-65c6-451d-bf6f-494a1d272cbf"
	meta := map[string]interface{}{
		"testKey": Payload("testVal"),
	}

	// Create the actions
	bulkDeleteActions, err := NewBulkDeleteActions(tenantID, meta)

	if err != nil {
		t.Fatalf("error creating action: %v", err)
	}

	// Set mock expectations
	mockRepo.
		EXPECT().
		DeleteAllByTenantIDAndMeta(gomock.Any(), gomock.Eq(scheduler.TenantID(tenantID)), gomock.Eq(meta)).
		Return(int64(1), nil).
		Times(1)

	// Handle the actions
	count, err := bulkDeleteActions.Handle(context.Background(), mockRepo)
	assert.Equal(t, int64(1), count)
	assert.Nil(t, err)
}
