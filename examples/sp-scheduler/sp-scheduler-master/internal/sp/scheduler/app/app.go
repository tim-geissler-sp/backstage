// Copyright (c) 2020. Sailpoint Technologies, Inc. All rights reserved.
package app

import (
	"context"

	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler/cmd"
)

// App is an interface that represents all of the functionality enabled by the application.
type App interface {
	ListActions(ctx context.Context, cmd cmd.ListActions) ([]*scheduler.Action, error)
	CreateAction(ctx context.Context, cmd cmd.CreateAction) (*scheduler.Action, error)
	DeleteAction(ctx context.Context, cmd cmd.DeleteAction) (bool, error)
	TriggerActionsPastDeadline(ctx context.Context, cmd cmd.TriggerActionsPastDeadline) (int64, error)
	DeleteAllByTenantID(ctx context.Context, cmd cmd.DeleteAllByTenantID) (int64, error)
	BulkDeleteActions(ctx context.Context, cmd cmd.BulkDeleteActions) (int64, error)
	CountActions(ctx context.Context) (int64, error)
}

// app is the private implementation of App
type app struct {
	actionRepo     scheduler.ActionRepo
	eventPublisher scheduler.EventPublisher
}

// NewApp constructs a new App
func NewApp(actionRepo scheduler.ActionRepo, eventPublisher scheduler.EventPublisher) App {
	app := &app{}
	app.actionRepo = actionRepo
	app.eventPublisher = eventPublisher

	return app
}

// CreateAction schedules a new action.
func (a *app) CreateAction(ctx context.Context, cmd cmd.CreateAction) (*scheduler.Action, error) {
	return cmd.Handle(ctx, a.actionRepo)
}

// DeleteAction deletes a previously-scheduled action.
func (a *app) DeleteAction(ctx context.Context, cmd cmd.DeleteAction) (bool, error) {
	return cmd.Handle(ctx, a.actionRepo)
}

// TriggerActionsPastDeadline fires events for actions that have passed their deadlines and deletes
// them from the database.
// Returns the count of actions that were fired.
func (a *app) TriggerActionsPastDeadline(ctx context.Context, cmd cmd.TriggerActionsPastDeadline) (int64, error) {
	return cmd.Handle(ctx, a.actionRepo, a.eventPublisher)
}

// DeleteAllByTenantID deletes all data related to the specified tenant.
func (a *app) DeleteAllByTenantID(ctx context.Context, cmd cmd.DeleteAllByTenantID) (int64, error) {
	return cmd.Handle(ctx, a.actionRepo)
}

// BulkDeleteActions deletes all actions for a tenant that match a meta query.
func (a *app) BulkDeleteActions(ctx context.Context, cmd cmd.BulkDeleteActions) (int64, error) {
	return cmd.Handle(ctx, a.actionRepo)
}

// ListActions lists all actions for a tenant that match a meta query.
func (a *app) ListActions(ctx context.Context, cmd cmd.ListActions) ([]*scheduler.Action, error) {
	return cmd.Handle(ctx, a.actionRepo)
}

// CountActions returns the count of all actions in the system.
func (a *app) CountActions(ctx context.Context) (int64, error) {
	return a.actionRepo.CountAll(ctx)
}
