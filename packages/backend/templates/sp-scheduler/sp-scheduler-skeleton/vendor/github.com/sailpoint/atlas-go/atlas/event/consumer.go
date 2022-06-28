// Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
package event

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"
	"time"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promauto"
	"github.com/sailpoint/atlas-go/atlas/metric"

	"github.com/confluentinc/confluent-kafka-go/kafka"
	"github.com/sailpoint/atlas-go/atlas"
	"github.com/sailpoint/atlas-go/atlas/log"
)

// eventProcessingLatency is a metric that times the latency between event publish and
// the consumer receiving the event.
var eventProcessingLatency = promauto.NewHistogramVec(prometheus.HistogramOpts{
	Name:    "kafka_event_processing_latency_seconds",
	Help:    "The amount of time between when an event was submitted and when it was processed",
	Buckets: []float64{0.1, 0.5, 1.0, 5.0, 15.0, 30.0, 60.0, 120.0, 180.0, 300.0, 600.0},
}, []string{"pod", "org", "name", "eventType", "groupId"})

// eventHandlerDuration is a metric that times the duration of a message handler invocation.
var eventHandlerDuration = promauto.NewHistogramVec(prometheus.HistogramOpts{
	Name:    "kafka_event_consumer_duration_seconds",
	Help:    "The amount of time a consumer takes to handle an event",
	Buckets: []float64{0.1, 0.5, 1.0, 5.0, 15.0, 30.0, 60.0, 120.0, 180.0, 300.0, 600.0},
}, []string{"pod", "org", "name", "eventType", "groupId"})

// eventHandlerSuccess is a metric that counts the number of events successfully handled.
var eventHandlerSuccess = promauto.NewCounterVec(prometheus.CounterOpts{
	Name: "kafka_event_processed_success",
	Help: "The number of events successfully handled",
}, []string{"pod", "org", "name", "eventType", "groupId"})

// eventHandlerFailed is a metric that counts the number of event handler failures.
var eventHandlerFailed = promauto.NewCounterVec(prometheus.CounterOpts{
	Name: "kafka_event_processed_failure",
	Help: "The number of events that were not successfully handled",
}, []string{"pod", "org", "name", "eventType", "groupId"})

// payloadDownloadDuration is a histogram timer metric that keeps track of the
// amount of time it takes to download a large event payload.
var payloadDownloadDuration = promauto.NewHistogramVec(prometheus.HistogramOpts{
	Name:    "kafka_s3_download_duration_seconds",
	Help:    "Duration of download large event from S3",
	Buckets: []float64{0.1, 0.5, 1.0, 5.0, 15.0, 30.0, 60.0, 120.0, 180.0, 300.0, 600.0},
}, []string{"topic", "eventType", "groupId"})

// StartConsumer runs a consumer process that process until a context is closed.
func StartConsumer(ctx context.Context, config ConsumerConfig, handler Handler, metricConfig metric.MetricsConfig) error {
	c, err := kafka.NewConsumer(&kafka.ConfigMap{
		BootstrapServersConfig:             config.BootstrapServers,
		GroupIdConfig:                      config.GroupID,
		MessageMaxBytesConfig:              config.MessageMaxBytes,
		MaxPartitionFetchBytesConfig:       config.MaxPartitionFetchBytes,
		MaxPollIntervalMsConfig:            config.MaxPollIntervalMs,
		SessionTimeoutMsConfig:             config.SessionTimeoutMs,
		HeartbeatIntervalMsConfig:          config.HeartbeatIntervalMs,
		AutoOffsetResetConfig:              config.AutoOffsetReset,
		PartitionAssignmentStrategyConfig:  config.PartitionAssignmentStrategy,
		GoApplicationRebalanceEnableConfig: true,
		EnableAutoCommitConfig:             true, // must always be true for atlas-go consumer
	})
	if err != nil {
		log.Errorf(ctx, "error starting kafka consumer: %e", err)
		return err
	}
	defer c.Close()

	downloader := newS3ExternalDownloader(downloaderConfig{bucket: config.ExternalBucket})

	topics := make([]string, 0)
	for _, t := range config.Topics {
		topics = append(topics, buildTopicRegexes(t, config.Pods)...)
	}

	log.Infof(ctx, "subscribing to topics: %v", topics)
	err = c.SubscribeTopics(topics, nil)
	if err != nil {
		log.Errorf(ctx, "error subscribing to topics: %e", err)
		return err
	}

	for {
		select {
		case <-ctx.Done():
			return ctx.Err()

		default:
			ev := c.Poll(10000)
			if ev == nil {
				continue
			}

			switch e := ev.(type) {
			case kafka.AssignedPartitions:
				log.Infof(ctx, "assigned partitions: %v", e)
				err := c.Assign(e.Partitions)
				if err != nil {
					log.Errorf(ctx, "error assigning partitions: %v", err)
					break
				}
			case kafka.RevokedPartitions:
				log.Infof(ctx, "revoked partitions: %v", e)
				err := c.Unassign()
				if err != nil {
					log.Errorf(ctx, "error revoking partitions: %v", err)
					break
				}
			case *kafka.Message:
				topic, err := ParseTopic(*e.TopicPartition.Topic)
				if err != nil {
					log.Errorf(ctx, "invalid topic: %v", err)
					break
				}

				var parsedEvent Event
				if err := json.Unmarshal(e.Value, &parsedEvent); err != nil {
					log.Errorf(ctx, "error parsing event: %v", err)
					break
				}

				for _, value := range e.Headers {

					// TODO: Skip event if targeted to other service
					if value.Key == HeaderKeyGroupID {
					}

					// If compact event, download and handle actual event from S3
					if value.Key == HeaderKeyIsCompactEvent {
						if isCompact, _ := strconv.ParseBool(string(value.Value)); isCompact {
							var s3ObjectKey string
							if err := json.Unmarshal([]byte(parsedEvent.ContentJSON), &s3ObjectKey); err != nil {
								log.Errorf(ctx, "error parsing large event s3 location %s: %v", parsedEvent.ContentJSON, err)
								break
							}

							downloadStart := time.Now()

							event, err := downloader.Download(ctx, s3ObjectKey)
							if err != nil {
								log.Errorf(ctx, "error download event %s from s3 bucket %s: %v", s3ObjectKey, config.ExternalBucket, err)
								break
							}

							downloadDuration := time.Since(downloadStart)

							if enabled, _ := metricConfig.IsNormalizedMetricEnabled(); enabled {
								payloadDownloadDuration.
									With(prometheus.Labels{
										"topic":     string(topic.Name()),
										"eventType": event.Type,
										"groupId":   event.Headers[HeaderKeyGroupID],
									}).
									Observe(float64(downloadDuration.Seconds()))
							}

							parsedEvent = *event
						}
					}
				}

				handlerStart := time.Now()
				err = handler.HandleEvent(ctx, topic, &parsedEvent)
				handlerDuration := time.Since(handlerStart)

				labels := prometheus.Labels{
					"pod":       parsedEvent.Headers[HeaderKeyPod],
					"org":       parsedEvent.Headers[HeaderKeyOrg],
					"name":      string(topic.Name()),
					"eventType": parsedEvent.Type,
					"groupId":   parsedEvent.Headers[HeaderKeyGroupID],
				}

				if err != nil {
					log.Errorf(ctx, "event handler failed: %v", err)

					if enabled, _ := metricConfig.IsNormalizedMetricEnabled(); enabled {
						eventHandlerFailed.With(labels).Inc()
					}
				} else {
					if enabled, _ := metricConfig.IsNormalizedMetricEnabled(); enabled {
						eventHandlerSuccess.With(labels).Inc()
					}
				}

				if enabled, _ := metricConfig.IsNormalizedMetricEnabled(); enabled {
					eventLatency := handlerStart.Sub(time.Time(parsedEvent.Timestamp))
					eventProcessingLatency.With(labels).Observe(float64(eventLatency.Seconds()))
					eventHandlerDuration.With(labels).Observe(float64(handlerDuration.Seconds()))
				}
			case kafka.PartitionEOF:
				log.Infof(ctx, "reached: %v", e)
			case kafka.Error:
				log.Errorf(ctx, "kafka error: %v", e.Error())
			}
		}
	}
}

// buildTopicRegexes takes a TopicDescriptor and set of pods and returns a list of regex strings
// suitable for passing to Kafka's consumer configuration.
func buildTopicRegexes(topic TopicDescriptor, pods []atlas.Pod) []string {
	var result []string

	switch topic.Scope() {
	case TopicScopeGlobal:
		result = append(result, string(topic.Name()))
	case TopicScopePod:
		for _, p := range pods {
			result = append(result, fmt.Sprintf("%s__%s", topic.Name(), string(p)))
		}
	case TopicScopeOrg:
		for _, p := range pods {
			result = append(result, fmt.Sprintf("^%s__%s__.+", topic.Name(), string(p)))
		}
	}

	return result
}
