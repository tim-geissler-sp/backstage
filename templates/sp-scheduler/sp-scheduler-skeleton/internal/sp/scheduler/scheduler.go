// Copyright (c) 2020. Sailpoint Technologies, Inc. All rights reserved.
package scheduler

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/sailpoint/atlas-go/atlas"
	"github.com/sailpoint/atlas-go/atlas/event"

	topics "github.com/sailpoint/saas-kafka-artifacts"
)

var (
	// ErrInvalidInput is an error thrown when input is incorrect or out of range.
	// Generally thrown when parsing commands.
	ErrInvalidInput = errors.New("invalid input")
)

// ActionID is unique identifier for an action (eg. "1c1f45db-91ca-4eae-905c-81071b1a8414")
type ActionID uuid.UUID

func (a ActionID) String() string {
	return uuid.UUID(a).String()
}

// TenantID is a unique identifier for a tenant (eg. "1c1f45db-91ca-4eae-905c-81071b1a8414")
type TenantID string

// Pod is a name of a shard of tenants. (eg. "dev01-useast1")
type Pod string

// Org is a human-readable, url-safe name for a tenant (eg. "acme")
type Org string

// Action is type that represents an event to be scheduled at a future date.
type Action struct {
	ID               ActionID               `json:"id"`
	TenantID         TenantID               `json:"-"`
	Created          time.Time              `json:"created"`
	Deadline         time.Time              `json:"deadline"`
	CronString       string                 `json:"cronString"`
	Event            Event                  `json:"event"`
	Meta             map[string]interface{} `json:"meta"`
	Retry            int                    `json:"retry"`
	TimezoneLocation string                 `json:"timezoneLocation"`
	TimezoneOffset   string                 `json:"timezoneOffset"`
}

// Event is a type associated with an action and fired when the action passes it's deadline.
type Event struct {
	Topic   string                 `json:"topic"`
	Type    string                 `json:"type"`
	Content map[string]interface{} `json:"content"`
	Headers map[string]string      `json:"headers"`
}

// ActionRepo is an interface for access to actions.
type ActionRepo interface {
	FindAllByTenantID(ctx context.Context, tenantID TenantID, limit int, offset int) ([]*Action, error)
	FindAllByTenantIDAndMeta(ctx context.Context, tenantID TenantID, meta map[string]interface{}, limit int, offset int) ([]*Action, error)
	FindByID(ctx context.Context, id ActionID) (*Action, error)
	Save(ctx context.Context, a *Action) error
	DeleteByID(ctx context.Context, id ActionID) (bool, error)
	TriggerActionsPastDeadline(ctx context.Context, deadline time.Time, max int64, retryLimit int, eventPublisher EventPublisher) (int64, error)
	CountAll(ctx context.Context) (int64, error)
	DeleteAllByTenantID(ctx context.Context, tenantID TenantID) (int64, error)
	DeleteAllByTenantIDAndMeta(ctx context.Context, tenantID TenantID, meta map[string]interface{}) (int64, error)
}

// EventPublisher is an interface for efficient publishing of action events.
type EventPublisher interface {
	BulkPublish(ctx context.Context, actions []*Action) ([]Action, error)
}

// NewAction constructs a new Action.
func NewAction(tenantID TenantID, deadline time.Time, timezoneLocation string, timezoneOffset string, cronString string, event Event, meta map[string]interface{}) *Action {
	a := &Action{}
	a.ID = ActionID(uuid.New())
	a.TenantID = tenantID
	a.Created = time.Now().UTC()
	a.Deadline = deadline.UTC()
	a.TimezoneLocation = timezoneLocation
	a.TimezoneOffset = timezoneOffset
	a.CronString = cronString
	a.Event = event
	a.Meta = meta
	a.Retry = 0

	return a
}

// IsCron returns true if Action is a cron Action.
func (a Action) IsCron() bool {
	return a.CronString != ""
}

// ValidateTopic validates topic specified in the scheduler event by searching for the corresponding topic descriptor in saas-kafka-artifacts.
// This method also returns the correct (and full) name of the topic depending on the scope.
func (e Event) ValidateTopic() (event.Topic, error) {
	td, err := topics.ParseTopicDescriptor(e.Topic)
	if err != nil {
		return nil, err
	}

	pod := e.Headers[event.HeaderKeyPod]
	org := e.Headers[event.HeaderKeyOrg]

	if td.Scope() == event.TopicScopePod {
		if pod == "" {
			return nil, fmt.Errorf("event is missing pod")
		}
		return event.NewPodTopic(td.Name(), atlas.Pod(pod)), nil
	} else if td.Scope() == event.TopicScopeOrg {
		if pod == "" || org == "" {
			return nil, fmt.Errorf("event is missing pod or org")
		}
		return event.NewOrgTopic(td.Name(), atlas.Pod(pod), atlas.Org(org)), nil
	} else if td.Scope() == event.TopicScopeGlobal {
		return event.NewGlobalTopic(td.Name()), nil
	}

	return nil, fmt.Errorf("unknown TopicScope enum value %d", td.Scope())
}
