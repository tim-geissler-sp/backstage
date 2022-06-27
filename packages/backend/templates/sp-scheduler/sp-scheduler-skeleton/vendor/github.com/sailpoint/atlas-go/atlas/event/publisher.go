// Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
package event

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"strconv"
	"time"

	"github.com/sailpoint/atlas-go/atlas/metric"

	"github.com/confluentinc/confluent-kafka-go/kafka"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promauto"
	"github.com/sailpoint/atlas-go/atlas/log"
)

// eventPublishedCount is a counter metirc that keeps track of how many
// events have been published.
var eventPublishedCount = promauto.NewCounterVec(prometheus.CounterOpts{
	Name: "event_published_count",
}, []string{"topic", "type"})

// eventPublishedCountNormalized is a counter metric that keeps track of how many
// events have been published.
var eventPublishedCountNormalized = promauto.NewCounterVec(prometheus.CounterOpts{
	Name: "kafka_event_published",
	Help: "The number of events published",
}, []string{"topic", "eventType"})

// payloadUploadDuration is a histogram timer metric that keeps track of how
// long it takes to upload a large event payload.
var payloadUploadDuration = promauto.NewHistogramVec(prometheus.HistogramOpts{
	Name:    "kafka_s3_upload_duration_seconds",
	Help:    "Duration of upload large event to S3",
	Buckets: []float64{0.1, 0.5, 1.0, 5.0, 15.0, 30.0, 60.0, 120.0, 180.0, 300.0, 600.0},
}, []string{"topic", "eventType", "groupId"})

// payloadSize is a gauge metric that tracks the size of the payloads that
// are uploaded.
var payloadSize = promauto.NewGaugeVec(prometheus.GaugeOpts{
	Name: "kafka_s3_event_size",
	Help: "The size of large event payloads stored in S3",
}, []string{"topic", "eventType", "groupId"})

// Publisher is an interface that enables external event publication.
type Publisher interface {
	BulkPublish(ctx context.Context, events []EventAndTopic) ([]*FailedEventAndTopic, error)
	Publish(ctx context.Context, td TopicDescriptor, event *Event) error
	PublishToTopic(ctx context.Context, topic Topic, event *Event) error
}

// DefaultPublisher is a publisher implementation that pushes events
// Kafka.
type DefaultPublisher struct {
	p             *kafka.Producer
	uploader      *s3ExternalUploader
	metricsConfig metric.MetricsConfig
}

// NewPublisher constructs a new DefaultPublisher using the specified config.
func NewPublisher(config PublisherConfig, metricsConfig metric.MetricsConfig) (*DefaultPublisher, error) {
	p, err := kafka.NewProducer(&kafka.ConfigMap{
		BootstrapServersConfig: config.BootstrapServers,
		CompressionTypeConfig:  config.CompressionType,
		MessageMaxBytesConfig:  config.MessageMaxBytes,
	})
	if err != nil {
		return nil, fmt.Errorf("create publisher: %w", err)
	}

	uploaderConfig := uploaderConfig{
		bucket:          config.ExternalBucket,
		uploadThreshold: config.MessageMaxBytes - 100000, // arbitrary 100 KB padding for record metadata,
	}
	if uploaderConfig.uploadThreshold < 0 {
		uploaderConfig.uploadThreshold = 0
	}

	uploader := newS3ExternalUploader(uploaderConfig)

	publisher := &DefaultPublisher{
		p:             p,
		uploader:      uploader,
		metricsConfig: metricsConfig,
	}

	return publisher, nil
}

// BulkPublish publishes a batch of events to Kafka. If any event fails, it will be skipped with a warning log message
func (p *DefaultPublisher) BulkPublish(ctx context.Context, events []EventAndTopic) ([]*FailedEventAndTopic, error) {

	failedEvents := make([]*FailedEventAndTopic, 0, len(events))
	deliveries := make(chan kafka.Event)
	enqueuedEventCount := 0

	for _, et := range events {

		// If large event, upload actual event to S3 and publish compact event to Kafka
		if p.uploader.ShouldUpload(ctx, et.Event) {
			uploadStart := time.Now()

			uploadedEvent, err := p.uploader.Upload(ctx, et.Topic, et.Event)
			if err != nil {
				failedEvents = append(failedEvents, NewFailedFailedEventAndTopic(et, err))
				log.Warnf(ctx, "%v", err)
				continue
			}

			uploadDuration := time.Since(uploadStart)

			if enabled, _ := p.metricsConfig.IsNormalizedMetricEnabled(); enabled {
				labelValues := make(prometheus.Labels, 5)
				labelValues["topic"] = string(et.Topic.Name())
				labelValues["eventType"] = et.Event.Type
				labelValues["groupId"] = et.Event.Headers[HeaderKeyGroupID]

				payloadUploadDuration.With(labelValues).Observe(float64(uploadDuration.Seconds()))
				payloadSize.With(labelValues).Set(float64(uploadedEvent.Size))
			}

			s3ObjectKeyJsonBytes, err := json.Marshal(uploadedEvent.Location)
			if err != nil {
				failedEvents = append(failedEvents, NewFailedFailedEventAndTopic(et, err))
				log.Warnf(ctx, "failed to parse large event location %s to JSON: %v", uploadedEvent.Location, err)
				continue
			}

			et.Event.Headers[HeaderKeyIsCompactEvent] = strconv.FormatBool(true)
			et.Event = &Event{
				Headers:     et.Event.Headers,
				ID:          et.Event.ID,
				Timestamp:   et.Event.Timestamp,
				Type:        et.Event.Type,
				ContentJSON: string(s3ObjectKeyJsonBytes),
			}
		}

		topicID := string(et.Topic.ID())

		eventJSON, err := json.Marshal(et.Event)
		if err != nil {
			failedEvents = append(failedEvents, NewFailedFailedEventAndTopic(et, err))
			log.Warnf(ctx, "failed to parse event on topic %s: %v", topicID, err)
			continue
		}

		// TODO: derive partition from HeaderKeyPartition, if specified...
		msg := &kafka.Message{
			TopicPartition: kafka.TopicPartition{Topic: &topicID, Partition: kafka.PartitionAny},
			Value:          eventJSON,
			Headers:        getHeaders(et.Event),
		}

		if err := p.p.Produce(msg, deliveries); err != nil {
			failedEvents = append(failedEvents, NewFailedFailedEventAndTopic(et, err))
			log.Warnf(ctx, "failed to enqueue event on topic %s: %v", topicID, err)
			continue
		}

		enqueuedEventCount++

		if enabled, _ := p.metricsConfig.IsNormalizedMetricEnabled(); enabled {
			eventPublishedCountNormalized.WithLabelValues(string(et.Topic.Name()), et.Event.Type).Inc()
		}

		if enabled, _ := p.metricsConfig.IsDeprecatedMetricEnabled(); enabled {
			eventPublishedCount.WithLabelValues(string(et.Topic.Name()), et.Event.Type).Inc()
		}
	}

	for i := 0; i < enqueuedEventCount; i++ {
		select {
		case <-ctx.Done():
			return failedEvents, ctx.Err()
		case e := <-deliveries:
			m := e.(*kafka.Message)

			if m.TopicPartition.Error != nil {
				topicID := ""
				if m.TopicPartition.Topic != nil {
					topicID = *m.TopicPartition.Topic
				}

				log.Warnf(ctx, "failed to publish event to topic %s: %v", topicID, m.TopicPartition.Error)
				var failedEvent Event
				err := json.Unmarshal(m.Value, &failedEvent)
				if err != nil {
					log.Warnf(ctx, "could not unmarshal enqueued kafka msg from topic %s: %v", topicID, err)
					continue
				}
				fEvT := EventAndTopic{}
				fEvT.Event = &failedEvent
				fEvT.Topic, _ = ParseTopic(*m.TopicPartition.Topic)

				thisFailedEventAndTopic := NewFailedFailedEventAndTopic(fEvT, m.TopicPartition.Error)
				failedEvents = append(failedEvents, thisFailedEventAndTopic)
				continue

			}
		}
	}

	if len(failedEvents) > 0 {
		return failedEvents, errors.New("one or more event failed to send")
	}
	return nil, nil
}

// Publish sends a single event to an IDN Kafka topic
func (p *DefaultPublisher) Publish(ctx context.Context, td TopicDescriptor, et *Event) error {
	topic, err := NewTopic(ctx, td)
	if err != nil {
		return err
	}

	return p.PublishToTopic(ctx, topic, et)
}

// PublishToTopic sends a single event to Kafka.
func (p *DefaultPublisher) PublishToTopic(ctx context.Context, topic Topic, event *Event) error {
	et := EventAndTopic{
		Event: event,
		Topic: topic,
	}

	events := make([]EventAndTopic, 1, 1)
	events[0] = et

	_, err := p.BulkPublish(ctx, events)
	return err
}

// getHeaders returns the Event's groupId and isCompactEvent headers as native, Kafka headers
func getHeaders(event *Event) []kafka.Header {
	headers := make([]kafka.Header, 0, 2)

	if val, keyExists := event.Headers[HeaderKeyGroupID]; keyExists {
		headers = append(headers, kafka.Header{
			Key:   HeaderKeyGroupID,
			Value: []byte(val),
		})
	}

	if val, keyExists := event.Headers[HeaderKeyIsCompactEvent]; keyExists {
		headers = append(headers, kafka.Header{
			Key:   HeaderKeyIsCompactEvent,
			Value: []byte(val),
		})
	}

	return headers
}
