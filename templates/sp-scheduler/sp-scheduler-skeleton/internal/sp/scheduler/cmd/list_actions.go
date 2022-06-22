// Copyright (c) 2020. Sailpoint Technologies, Inc. All rights reserved.
package cmd

import (
	"context"
	"fmt"

	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler"
)

// ListActions is a command to list the actions for a tenant and meta query.
type ListActions struct {
	tenantID scheduler.TenantID
	meta     map[string]interface{}
	limit    int
	offset   int
}

// NewListActions constructs a new command, returning an error if validation fails.
func NewListActions(tenantID string, meta map[string]interface{}, limit int, offset int) (*ListActions, error) {
	if tenantID == "" {
		return nil, fmt.Errorf("tenantID is required: %w", scheduler.ErrInvalidInput)
	}

	if limit <= 0 || limit > 250 {
		return nil, fmt.Errorf("invalid limit value: %d: %w", limit, scheduler.ErrInvalidInput)
	}
	if offset < 0 {
		return nil, fmt.Errorf("invalid offset value: %d: %w", offset, scheduler.ErrInvalidInput)
	}

	cmd := &ListActions{}
	cmd.tenantID = scheduler.TenantID(tenantID)
	cmd.meta = meta
	cmd.limit = limit
	cmd.offset = offset

	return cmd, nil
}

// Handle returns the actions that match the specified meta query.
func (cmd *ListActions) Handle(ctx context.Context, repo scheduler.ActionRepo) ([]*scheduler.Action, error) {
	if len(cmd.meta) == 0 {
		return repo.FindAllByTenantID(ctx, cmd.tenantID, cmd.limit, cmd.offset)
	}

	return repo.FindAllByTenantIDAndMeta(ctx, cmd.tenantID, cmd.meta, cmd.limit, cmd.offset)
}
