// Copyright (c) 2020. Sailpoint Technologies, Inc. All rights reserved.
package cmd

import (
	"context"
	"fmt"

	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler"
)

// DeleteAllByTenantID is command that deletes all data related to the specified tenant.
type DeleteAllByTenantID struct {
	tenantID scheduler.TenantID
}

// NewDeleteAllByTenantID constructs a new command, returning an error if validation fails.
func NewDeleteAllByTenantID(tenantID string) (*DeleteAllByTenantID, error) {
	if tenantID == "" {
		return nil, fmt.Errorf("tenantID is required: %w", scheduler.ErrInvalidInput)
	}

	cmd := &DeleteAllByTenantID{}
	cmd.tenantID = scheduler.TenantID(tenantID)

	return cmd, nil
}

// Handle deletes all data associated with the specified tenant.
func (cmd *DeleteAllByTenantID) Handle(ctx context.Context, repo scheduler.ActionRepo) (int64, error) {
	return repo.DeleteAllByTenantID(ctx, cmd.tenantID)
}
