/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.context.service;

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
public class GlobalContextDebugService extends NotificationDebugService {

	public final static String REDIS_CONTEXT_DEBUG_KEY = "REDIS_CONTEXT_DEBUG_KEY";

	@Inject
	public GlobalContextDebugService(Provider<EventService> eventService,
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
		return super.publishKafkaEvent(topicDescriptor, eventType, eventContext, REDIS_CONTEXT_DEBUG_KEY);
	}

}
