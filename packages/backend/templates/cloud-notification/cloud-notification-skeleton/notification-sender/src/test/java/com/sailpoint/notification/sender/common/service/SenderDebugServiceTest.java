/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.sender.common.service;

import com.google.inject.Provider;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.idn.RestClientProvider;
import com.sailpoint.atlas.idn.ServiceNames;
import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.TopicDescriptor;
import com.sailpoint.mantisclient.BaseRestClient;
import com.sailpoint.notification.api.event.RecipientBuilder;
import com.sailpoint.notification.api.event.dto.NotificationRendered;
import com.sailpoint.notification.api.event.dto.SlackNotificationRendered;
import com.sailpoint.notification.api.event.dto.SlackNotificationResponse;
import com.sailpoint.notification.api.event.dto.TeamsNotificationRendered;
import com.sailpoint.notification.sender.email.MailClient;
import com.sailpoint.notification.sender.email.Validator;
import com.sailpoint.utilities.JsonUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import redis.clients.jedis.Jedis;

import java.util.function.Function;

import static com.sailpoint.notification.sender.slack.service.SlackService.DEFAULT_SERVICE;
import static com.sailpoint.notification.sender.slack.service.SlackService.DEFAULT_NOTIFY_URI;
import static com.sailpoint.notification.sender.slack.service.SlackService.SLACK_NOTIFICATION_SERVICE;
import static com.sailpoint.notification.sender.slack.service.SlackService.SLACK_NOTIFY_URI;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SenderDebugServiceTest {

	@Mock
	Provider<EventService> _esProvider;

	@Mock
	EventService _eventService;

	@Mock
	Provider<RedisPool> _redisPoolProvider;

	@Mock
	RedisPool _redisPool;

	@Mock
	MailClient _mailClient;

	@Mock
	AtlasConfig _atlasConfig;

	@Mock
	RestClientProvider _restClientProvider;

	@Mock
	BaseRestClient _client;

	SenderDebugService _senderDebugService;

	NotificationRendered notificationRendered;

	SlackNotificationRendered slackNotificationRendered;

	TeamsNotificationRendered teamsNotificationRendered;

	@Mock
	Jedis _jedis;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		when(_esProvider.get()).thenReturn(_eventService);
		when(_redisPoolProvider.get()).thenReturn(_redisPool);
		when(_restClientProvider.getInternalRestClientWithServiceURL(DEFAULT_SERVICE, ServiceNames.SLACK))
				.thenReturn(_client);
		when(_atlasConfig.getString(SLACK_NOTIFICATION_SERVICE,
				DEFAULT_SERVICE)).thenReturn(DEFAULT_SERVICE);
		when(_atlasConfig.getString(SLACK_NOTIFY_URI,
                DEFAULT_NOTIFY_URI)).thenReturn(DEFAULT_NOTIFY_URI);

		_senderDebugService = new SenderDebugService();
		_senderDebugService._mailClient = _mailClient;
		_senderDebugService._eventService = _esProvider;
		_senderDebugService._redisPoolProvider = _redisPoolProvider;
		_senderDebugService._atlasConfig = _atlasConfig;
		_senderDebugService._restClientProvider = _restClientProvider;
		_senderDebugService._validator = new Validator();


		notificationRendered = NotificationRendered.builder()
				.recipient(new RecipientBuilder()
						.withEmail("to@to")
						.build())
				.from("from@from")
				.subject("test")
				.body("<body>hello<br />html</body>")
				.build();

		slackNotificationRendered = SlackNotificationRendered.builder()
				.recipient(new RecipientBuilder()
						.withId("12345")
						.build())
				.text("Hello world")
				.attachments("[{'pretext': 'pre-hello', 'text': 'text-world'}]")
				.blocks("[{'type': 'section', 'text': {'type': 'plain_text', 'text': 'Hello world'}}]")
				.build();
		teamsNotificationRendered = TeamsNotificationRendered.builder()
				.recipient(new RecipientBuilder()
						.withId("12345")
						.build())
				.text("Hello world Text")
				.title("Hello world Title")
				.messageJSON("[{'type': 'section', 'text': {'type': 'plain_text', 'text': 'Hello world'}}]")
				.build();
	}

	@Test
	public void testPublishMailEvent() {
		Assert.assertNotNull(_senderDebugService.publishMailEvent(notificationRendered));
		verify(_eventService, times(1)).publishAsync(any(TopicDescriptor.class), any(Event.class));
	}

	@Test
	public void testPublishSlackEvent() {
		Assert.assertNotNull(_senderDebugService.publishSlackEvent(slackNotificationRendered));
		verify(_eventService, times(1)).publishAsync(any(TopicDescriptor.class), any(Event.class));
	}

	@Test
	public void testPublishTeamsEvent() {
		Assert.assertNotNull(_senderDebugService.publishTeamsEvent(teamsNotificationRendered));
		verify(_eventService, times(1)).publishAsync(any(TopicDescriptor.class), any(Event.class));
	}

	@Test
	public void testSendValidMailWithDebugging() {
		_senderDebugService.sendMailWithDebugging(notificationRendered, "key");
		verify(_redisPool, times(1)).exec(any(Function.class));
	}

	@Test
	public void testSendValidSlackWithDebugging() {
		when(_client.post(any(),any())).thenReturn(getOKSlackResponse());
		_senderDebugService.sendSlackWithDebugging(slackNotificationRendered, "key");
		verify(_redisPool, times(1)).exec(any(Function.class));
	}

	@Test
	public void testSendSlackWithDebuggingHandleError() {
		when(_client.post(any(),any())).thenReturn(getErrorSlackResponse());
		_senderDebugService.sendSlackWithDebugging(slackNotificationRendered, "key");
		verify(_redisPool, times(2)).exec(any(Function.class));
	}

	@Test
	public void testInvaliSendSlackWithDebugging() {
		when(_client.post(any(),any())).thenReturn(null);
		_senderDebugService.sendSlackWithDebugging(slackNotificationRendered, "key");
		verify(_redisPool, times(1)).exec(any(Function.class));
	}

	@Test
	public void testSendInvalidMailWithDebugging() throws Exception {
		Mockito.doThrow(new RuntimeException("InvalidParameterValue")).when(_mailClient).sendMail(any());
		_senderDebugService.sendMailWithDebugging(notificationRendered.derive().from("from").build(), "key");
		ArgumentCaptor<Function> captor = ArgumentCaptor.forClass(Function.class);
		verify(_redisPool, times(1)).exec(captor.capture());
		captor.getValue().apply(_jedis);

		ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
		verify(_jedis, times(1)).set(anyString(), stringArgumentCaptor.capture(), any());
		stringArgumentCaptor.getValue().equals("InvalidParameterValue");
	}

	@Test
	public void testSendLargeMailWithDebugging() throws Exception {
		_senderDebugService.sendMailWithDebugging(notificationRendered, "testLargeEvent");
		verify(_redisPool, times(1)).exec(any(Function.class));
		verify(_mailClient, times(0)).sendMail(any());
	}

	@Test
	public void testRetrieveFromStore() {
		when(_redisPool.exec(any())).thenReturn("test");
		Assert.assertEquals(_senderDebugService.retrieveFromStore("key"), "test");
	}
	public static String getOKSlackResponse() {
		return JsonUtil.toJson(new SlackNotificationResponse(true, "","", "", ""));
	}

	public static String getErrorSlackResponse() {
		return JsonUtil.toJson(new SlackNotificationResponse(false, "", "error", "", ""));
	}
}
