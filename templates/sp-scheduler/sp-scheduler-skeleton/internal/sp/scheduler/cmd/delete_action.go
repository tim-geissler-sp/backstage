// Copyright (c) 2020. Sailpoint Technologies, Inc. All rights reserved.
package cmd

import (
	"context"
	"fmt"

	"github.com/google/uuid"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler"
)

// DeleteAction is a command to delete an action for the specified tenant and ID.
type DeleteAction struct {
	tenantID scheduler.TenantID
	id       scheduler.ActionID
}

// NewDeleteAction constructs a new command, returning an error if validation fails.
func NewDeleteAction(tenantID string, id string) (*DeleteAction, error) {
	if tenantID == "" {
		return nil, fmt.Errorf("tenantID is required: %w", scheduler.ErrInvalidInput)
	}

	actionID, err := uuid.Parse(id)
	if err != nil {
		return nil, fmt.Errorf("id is not a valid UUID: %w", scheduler.ErrInvalidInput)
	}

	cmd := &DeleteAction{}
	cmd.tenantID = scheduler.TenantID(tenantID)
	cmd.id = scheduler.ActionID(actionID)

	return cmd, nil
}

// Handle deletes the action, if the action is related to another tenant, the action is not deleted.
// Handle returns whether or not the action was successfully deleted.
func (cmd *DeleteAction) Handle(ctx context.Context, repo scheduler.ActionRepo) (bool, error) {
	a, err := repo.FindByID(ctx, cmd.id)
	if err != nil {
		return false, fmt.Errorf("load action: %s: %w", cmd.id, err)
	}

	if a == nil || a.TenantID != cmd.tenantID {
		return false, nil
	}

	return repo.DeleteByID(ctx, cmd.id)
}
