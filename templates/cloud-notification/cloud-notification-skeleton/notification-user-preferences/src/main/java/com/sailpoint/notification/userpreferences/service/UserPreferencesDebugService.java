/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.service;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sailpoint.atlas.OrgData;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import com.sailpoint.iris.client.TopicDescriptor;
import com.sailpoint.notification.api.event.RecipientBuilder;
import com.sailpoint.notification.sender.common.event.util.debug.NotificationDebugService;
import com.sailpoint.notification.userpreferences.dto.UserPreferences;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;

import javax.inject.Singleton;
import java.util.List;
import java.util.UUID;

/**
 * Debug service during Interest Matcher phase.
 */
@Singleton
public class UserPreferencesDebugService extends NotificationDebugService {

	public final static String REDIS_USER_DEBUG_KEY = "REDIS_USER_DEBUG_KEY";

	/**
	 * Fon now using debug repository for karate test.
	 */
	UserPreferencesRepository _userPreferencesRepository;

	@Inject
	public UserPreferencesDebugService(Provider<EventService> eventService,
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
		return super.publishKafkaEvent(topicDescriptor, eventType, eventContext, REDIS_USER_DEBUG_KEY);
	}

	public UserPreferences getUserPreferences(String recipientId) {
		return _userPreferencesRepository.findByRecipientId(recipientId);
	}

	/**
	 * Creates an UserPreference used by Debug E2E tests
	 *
	 * @return The UserPreference id.
	 */
	public String createDebugUserPreference() {

		RequestContext requestContext = RequestContext.ensureGet();

		final OrgData orgData = new OrgData();
		orgData.setPod(requestContext.getPod());
		orgData.setOrg(OrgLifecycleDebugService.DEBUG_ORG);

		requestContext.setOrgData(orgData);

		final String id = UUID.randomUUID().toString();
		UserPreferences userPreferences = new UserPreferences.UserPreferencesBuilder()
				.withRecipient(new RecipientBuilder()
						.withId(id)
						.withEmail("debug@email.com")
						.withPhone("debug-phone")
						.build())
				.build();
		_userPreferencesRepository.create(userPreferences);

		return id;
	}

	/**
	 * List all UserPreferences by tenant.
	 *
	 * @param tenant The Tenant
	 * @return List of tenants.
	 */
	public List<UserPreferences> list(String tenant) {
		return _userPreferencesRepository.findAllByTenant(tenant);
	}
}
