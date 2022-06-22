/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.interest.matcher.service;

import com.google.inject.Provider;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import com.sailpoint.iris.client.Event;
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

public class InterestMatcherDebugServiceTest {

	@Mock
	Provider<EventService> _esProvider;

	@Mock
	EventService _eventService;

	@Mock
	Provider<RedisPool> _redisPoolProvider;

	@Mock
	RedisPool _redisPool;

	private InterestMatcherDebugService _interestMatcherDebugService;


	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		when(_esProvider.get()).thenReturn(_eventService);
		when(_redisPoolProvider.get()).thenReturn(_redisPool);

		_interestMatcherDebugService = new InterestMatcherDebugService(_esProvider, _redisPoolProvider);
	}

	@Test
	public void testPublishKafkaEvent() {
		Assert.assertNotNull(_interestMatcherDebugService.publishKafkaEvent(IdnTopic.NOTIFICATION,
				"test_event", "{}", InterestMatcherDebugService.REDIS_INTEREST_DEBUG_KEY));
		verify(_eventService, times(1)).publishAsync(eq(IdnTopic.NOTIFICATION), any(Event.class));
	}
}