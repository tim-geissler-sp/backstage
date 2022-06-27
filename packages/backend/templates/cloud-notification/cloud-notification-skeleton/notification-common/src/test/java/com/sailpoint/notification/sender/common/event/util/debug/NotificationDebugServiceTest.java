/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.event.util.debug;

import com.google.inject.Provider;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import com.sailpoint.iris.client.Event;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import redis.clients.jedis.Jedis;

import java.util.function.Function;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NotificationDebugServiceTest {
	@Mock
	Provider<EventService> _esProvider;

	@Mock
	EventService _eventService;

	@Mock
	Provider<RedisPool> _redisPoolProvider;

	@Mock
	RedisPool _redisPool;

	@Mock
	Jedis _jedis;

	private NotificationDebugService _notificationDebugService;


	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		when(_esProvider.get()).thenReturn(_eventService);
		when(_redisPoolProvider.get()).thenReturn(_redisPool);

		_notificationDebugService = new NotificationDebugService(_esProvider, _redisPoolProvider);
	}

	@Test
	public void testPublishKafkaEvent() {
		Assert.assertNotNull(_notificationDebugService.publishKafkaEvent(IdnTopic.NOTIFICATION,
				"test_event", "{}", "DEBUG_KEY"));
		verify(_eventService, times(1)).publishAsync(eq(IdnTopic.NOTIFICATION), any(Event.class));
	}

	@Test
	public void testWriteToStore() {
		_notificationDebugService.writeToStore("test_key", "test_value");
		ArgumentCaptor<Function> captor = ArgumentCaptor.forClass(Function.class);
		verify(_redisPool, times(1)).exec(captor.capture());
		captor.getValue().apply(_jedis);

		ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
		verify(_jedis, times(1)).set(anyString(), stringArgumentCaptor.capture(), any());
		Assert.assertEquals("test_value", stringArgumentCaptor.getValue());

	}
}
