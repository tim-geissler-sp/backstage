/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.service;

import com.google.inject.Provider;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import com.sailpoint.iris.client.Event;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for UserPreferencesDebugService.
 */
public class OrgLifecycleDebugServiceTest {

	@Mock
	Provider<EventService> _esProvider;

	@Mock
	EventService _eventService;

	@Mock
	Provider<RedisPool> _redisPoolProvider;

	@Mock
	RedisPool _redisPool;

	@Mock
	UserPreferencesRepository _userPreferencesRepository;

	private OrgLifecycleDebugService _orgLifecycleDebugService;


	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		when(_esProvider.get()).thenReturn(_eventService);
		when(_redisPoolProvider.get()).thenReturn(_redisPool);

		_orgLifecycleDebugService = new OrgLifecycleDebugService(_esProvider,
				_redisPoolProvider, _userPreferencesRepository);
	}

	@Test
	public void testPublishKafkaEvent() {
		Assert.assertNotNull(_orgLifecycleDebugService.publishKafkaEvent(IdnTopic.ORG_LIFECYCLE, "ORG_DELETE", "{}"));
		verify(_eventService, times(1)).publishAsync(eq(IdnTopic.ORG_LIFECYCLE), any(Event.class));
	}
}