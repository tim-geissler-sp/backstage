/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.service;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import com.sailpoint.iris.client.TopicDescriptor;
import com.sailpoint.notification.sender.common.event.util.debug.NotificationDebugService;

import javax.inject.Singleton;

/**
 * Debug service during Notification Template phase.
 */
@Singleton
public class NotificationTemplateDebugService extends NotificationDebugService {

	public final static String REDIS_TEMPLATE_DEBUG_KEY = "REDIS_TEMPLATE_DEBUG_KEY";

	@Inject
	public NotificationTemplateDebugService(Provider<EventService> eventService,
											Provider<RedisPool> redisPoolProvider) {
		super(eventService, redisPoolProvider);
	}

	/**
	 * Publishes any json event to Kafka.
	 *
	 * @param topicDescriptor topic descriptor
	 * @param eventType event type
	 * @param eventContext event context as string
	 */
	public String publishKafkaEvent(TopicDescriptor topicDescriptor, String eventType, String eventContext) {
		return super.publishKafkaEvent(topicDescriptor, eventType, eventContext, REDIS_TEMPLATE_DEBUG_KEY);
	}

}
