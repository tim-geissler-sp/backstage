// Copyright (c) 2020. Sailpoint Technologies, Inc. All rights reserved.
package cmd

import (
	"context"
	"fmt"

	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler"
)

// BulkDeleteActions is a command that deletes all schedules for a specified tenant and meta query.
type BulkDeleteActions struct {
	tenantID scheduler.TenantID
	meta     map[string]interface{}
}

// NewBulkDeleteActions constructs a new command, returning an error if validation fails.
func NewBulkDeleteActions(tenantID string, meta map[string]interface{}) (*BulkDeleteActions, error) {
	if tenantID == "" {
		return nil, fmt.Errorf("tenantID is required: %w", scheduler.ErrInvalidInput)
	}

	if len(meta) == 0 {
		return nil, fmt.Errorf("meta must not be empty: %w", scheduler.ErrInvalidInput)
	}

	cmd := &BulkDeleteActions{}
	cmd.tenantID = scheduler.TenantID(tenantID)
	cmd.meta = meta

	return cmd, nil
}

// Handle deletes all of the matched schedules.
func (cmd *BulkDeleteActions) Handle(ctx context.Context, repo scheduler.ActionRepo) (int64, error) {
	return repo.DeleteAllByTenantIDAndMeta(ctx, cmd.tenantID, cmd.meta)
}
