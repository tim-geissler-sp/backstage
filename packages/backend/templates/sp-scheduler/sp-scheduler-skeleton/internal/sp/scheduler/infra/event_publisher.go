// Copyright (c) 2020. Sailpoint Technologies, Inc. All rights reserved.
package infra

import (
	"context"
	"errors"

	"github.com/sailpoint/atlas-go/atlas/event"
	"github.com/sailpoint/atlas-go/atlas/log"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler"
	"go.uber.org/zap"
)

// eventPublisher is a type that implements the required scheduler.EventPublisher interface using
// the atlas event publisher.
type eventPublisher struct {
	publisher event.Publisher
}

// NewPublisher constructs a new publisher using the atlas/event.Publisher implementation.
func NewPublisher(publisher event.Publisher) scheduler.EventPublisher {
	p := &eventPublisher{}
	p.publisher = publisher
	return p
}

// BulkPublish publishes a set of events. An error is returned if any event fails to
// send.
func (p *eventPublisher) BulkPublish(ctx context.Context, actions []*scheduler.Action) ([]scheduler.Action, error) {
	eventTopics := make([]event.EventAndTopic, 0, len(actions))
	failedEventTopics := make([]*event.FailedEventAndTopic, 0, len(actions))
	eventActionMap := make(map[string]*scheduler.Action)

	for _, action := range actions {
		tenantCtx := log.WithFields(ctx,
			zap.String("pod", action.Event.Headers[event.HeaderKeyPod]),
			zap.String("org", action.Event.Headers[event.HeaderKeyOrg]),
			zap.String("request_id", action.Event.Headers[event.HeaderKeyRequestID]),
		)
		log.Infof(tenantCtx, "publishing event of type '%s' to topic '%s'", action.Event.Type, action.Event.Topic)

		ae, newEventErr := event.NewEvent(action.Event.Type, action.Event.Content, action.Event.Headers)
		eventActionMap[ae.ID] = action

		topic, validateTopicErr := action.Event.ValidateTopic()

		et := event.EventAndTopic{}
		et.Topic = topic
		et.Event = ae

		if newEventErr != nil {
			failedEvtTopic := event.NewFailedFailedEventAndTopic(et, newEventErr)
			failedEventTopics = append(failedEventTopics, failedEvtTopic)
			continue
		} else {
			if validateTopicErr != nil {
				failedEvtTopic := event.NewFailedFailedEventAndTopic(et, validateTopicErr)
				failedEventTopics = append(failedEventTopics, failedEvtTopic)
				continue
			}
		}
		eventTopics = append(eventTopics, et)
	}

	log.Infof(ctx, "publishing %d event(s)", len(actions))
	failedEventsFromBulkPublish, err := p.publisher.BulkPublish(ctx, eventTopics)
	failedEventTopics = append(failedEventTopics, failedEventsFromBulkPublish...)

	failedActions := make([]scheduler.Action, 0, len(actions))
	for _, failedEvent := range failedEventTopics {
		failedActions = append(failedActions, *eventActionMap[failedEvent.EventAndTopic.Event.ID])
	}
	if len(failedActions) > 0 && err == nil {
		return failedActions, errors.New("one or more event failed to send")
	}

	return failedActions, err
}
