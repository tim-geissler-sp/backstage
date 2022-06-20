// Copyright (c) 2020. Sailpoint Technologies, Inc. All rights reserved.
package cmd

import (
	"context"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler/mocks"
	"github.com/stretchr/testify/assert"
)

func TestDeleteAllByTenantID(t *testing.T) {
	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()

	mockRepo := mocks.NewMockActionRepo(mockCtrl)

	tenantID := "b5e85c44-65c6-451d-bf6f-494a1d272cbf"

	deleteAllByTenantID, _ := NewDeleteAllByTenantID(tenantID)

	mockRepo.
		EXPECT().
		DeleteAllByTenantID(gomock.Any(), gomock.Any()).
		Return(int64(1), nil).
		Times(1)

	count, err := deleteAllByTenantID.Handle(context.Background(), mockRepo)
	assert.Equal(t, int64(1), count)
	assert.Nil(t, err)
}
