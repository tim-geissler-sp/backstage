// Copyright (c) 2020. Sailpoint Technologies, Inc. All rights reserved.
package cmd

import (
	"context"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler/mocks"
	"github.com/stretchr/testify/assert"
)

func TestTriggerActionsPastDeadline(t *testing.T) {
	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()

	mockRepo := mocks.NewMockActionRepo(mockCtrl)

	// Test data
	var batchSize int64 = 2000
	var retryLimit int = 3

	triggerActionsPastDeadline, err := NewTriggerActionsPastDeadline(batchSize, retryLimit)
	assert.Nil(t, err)
	gomock.InOrder(
		mockRepo.
			EXPECT().
			TriggerActionsPastDeadline(gomock.Any(), gomock.Any(), batchSize, retryLimit, gomock.Any()).
			Return(int64(1), nil),
		mockRepo.
			EXPECT().
			TriggerActionsPastDeadline(gomock.Any(), gomock.Any(), batchSize, retryLimit, gomock.Any()).
			Return(int64(0), nil),
	)

	count, err := triggerActionsPastDeadline.Handle(context.Background(), mockRepo, nil)
	assert.Equal(t, int64(1), count)
	assert.Nil(t, err)
}
