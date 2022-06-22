// Copyright (c) 2020. Sailpoint Technologies, Inc. All rights reserved.
package infra

import (
	"context"

	"github.com/sailpoint/atlas-go/atlas/event"
	"github.com/sailpoint/atlas-go/atlas/log"
	topics "github.com/sailpoint/saas-kafka-artifacts"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler/cmd"
)

// bindEventHandlers binds service methods to events on kafka.
func (s *SchedulerService) bindEventHandlers() *event.Router {
	r := event.NewRouterWithDefaultMiddleware()
	r.OnTopicAndEventType(topics.IdnTopic.ORG_LIFECYCLE, "ORG_DELETED", s.onOrgDeleted())

	return r
}

// onOrgDeleted is an event handler invoked when an org is deleted to purge all data
// related to the org that was deleted.
func (s *SchedulerService) onOrgDeleted() event.Handler {
	return event.HandlerFunc(func(ctx context.Context, topic event.Topic, e *event.Event) error {
		cmd, err := cmd.NewDeleteAllByTenantID(e.Headers[event.HeaderKeyTenantID])
		if err != nil {
			return err
		}

		count, err := s.app.DeleteAllByTenantID(ctx, *cmd)
		if err != nil {
			return err
		}

		if count > 0 {
			log.Infof(ctx, "deleted %d scheduled actions after org was deleted", count)
		}
		return nil
	})
}
