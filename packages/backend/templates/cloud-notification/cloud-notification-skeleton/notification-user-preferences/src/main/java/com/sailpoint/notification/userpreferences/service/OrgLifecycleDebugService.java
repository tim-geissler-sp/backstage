/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.service;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.client.TopicDescriptor;
import com.sailpoint.notification.sender.common.event.util.debug.NotificationDebugService;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;

import javax.inject.Singleton;
import java.util.Map;

/**
 * Debug service during Interest Matcher phase.
 */
@Singleton
public class OrgLifecycleDebugService extends NotificationDebugService {

	public final static String REDIS_ORG_LIFECYCLE_DEBUG_KEY = "REDIS_ORG_LIFECYCLE_DEBUG_KEY";

	public final static String DEBUG_ORG = "deleted-org";

	/**
	 * Fon now using debug repository for karate test.
	 */
	UserPreferencesRepository _userPreferencesRepository;

	@Inject
	public OrgLifecycleDebugService(Provider<EventService> eventService,
									Provider<RedisPool> redisPoolProvider,
									UserPreferencesRepository userPreferencesRepository) {
		super(eventService, redisPoolProvider);
		_userPreferencesRepository = userPreferencesRepository;
	}

	/**
	 * Publishes any json event to Kafka.
	 *
	 * @param topicDescriptor The TopicDescriptor.
	 * @param eventType event type
	 * @param eventContext event context as string
	 */
	public String publishKafkaEvent(TopicDescriptor topicDescriptor, String eventType, String eventContext) {
		return super.publishKafkaEvent(topicDescriptor, eventType, eventContext, REDIS_ORG_LIFECYCLE_DEBUG_KEY);
	}

	@Override
	protected Map<String, String> getEventHeaders() {
		_headerMap.put(EventHeaders.ORG, DEBUG_ORG);
		return _headerMap;
	}

}
