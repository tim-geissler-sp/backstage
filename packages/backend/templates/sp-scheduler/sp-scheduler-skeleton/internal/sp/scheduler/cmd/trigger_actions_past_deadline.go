// Copyright (c) 2020. Sailpoint Technologies, Inc. All rights reserved.
package cmd

import (
	"context"
	"fmt"
	"time"

	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler"
)

// TriggerActionsPastDeadline is a command that scans for batches of actions that have exceeded their deadline
// and publishes the corresponding events.
type TriggerActionsPastDeadline struct {
	batchSize  int64
	retryLimit int
}

// NewTriggerActionsPastDeadline constructs a new command, returning an error if validation fails.
func NewTriggerActionsPastDeadline(batchSize int64, retryLimit int) (*TriggerActionsPastDeadline, error) {
	if batchSize <= 0 {
		return nil, fmt.Errorf("batchSize must a positive integer: %w", scheduler.ErrInvalidInput)
	}

	cmd := &TriggerActionsPastDeadline{}
	cmd.batchSize = batchSize
	cmd.retryLimit = retryLimit

	return cmd, nil
}

// Handle scans through actions past their deadline and publishes the corresponding batches of events. The total number of
// actions dispatched is returned.
func (cmd *TriggerActionsPastDeadline) Handle(ctx context.Context, repo scheduler.ActionRepo, eventPublisher scheduler.EventPublisher) (int64, error) {
	count := int64(0)

	for {
		batchCount, err := repo.TriggerActionsPastDeadline(ctx, time.Now().UTC(), cmd.batchSize, cmd.retryLimit, eventPublisher)
		if err != nil {
			return count, err
		}

		if batchCount == 0 {
			break
		}

		count += batchCount
	}

	return count, nil
}
