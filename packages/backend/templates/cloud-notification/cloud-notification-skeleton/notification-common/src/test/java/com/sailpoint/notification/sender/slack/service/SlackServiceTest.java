/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.slack.service;

import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.exception.NotFoundException;
import com.sailpoint.atlas.idn.RestClientProvider;
import com.sailpoint.atlas.idn.ServiceNames;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.GlobalTopic;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.mantisclient.BaseRestClient;
import com.sailpoint.notification.api.event.RecipientBuilder;
import com.sailpoint.notification.api.event.dto.SlackNotificationRendered;
import com.sailpoint.notification.api.event.dto.SlackNotificationResponse;
import com.sailpoint.notification.sender.common.lifecycle.NotificationMetricsUtil;
import com.sailpoint.notification.sender.slack.service.SlackService;
import com.sailpoint.utilities.JsonUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.sailpoint.notification.sender.slack.service.SlackService.DEFAULT_GET_TENANTS_URI;
import static com.sailpoint.notification.sender.slack.service.SlackService.DEFAULT_SERVICE;
import static com.sailpoint.notification.sender.slack.service.SlackService.DEFAULT_NOTIFY_URI;
import static com.sailpoint.notification.sender.slack.service.SlackService.ERROR_RATE_LIMIT;
import static com.sailpoint.notification.sender.slack.service.SlackService.SLACK_GET_TENANTS_URI;
import static com.sailpoint.notification.sender.slack.service.SlackService.SLACK_NOTIFICATION_SERVICE;
import static com.sailpoint.notification.sender.slack.service.SlackService.SLACK_NOTIFY_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for testing SlackService
 */
public class SlackServiceTest {

	@Mock
	NotificationMetricsUtil _metricsUtil;

	@Mock
	RestClientProvider _restClientProvider;

	@Mock
	BaseRestClient _client;

	@Mock
	AtlasConfig _atlasConfig;

	@Mock
	EventHandlerContext _context;

	@Mock
	Event _event;

	SlackNotificationRendered _slackNotificationRendered;

	SlackService _slackService;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		_slackNotificationRendered = SlackNotificationRendered.builder()
				.recipient(new RecipientBuilder()
						.withId("12345")
						.build())
				.text("Hello world")
				.attachments("[{'pretext': 'pre-hello', 'text': 'text-world'}]")
				.blocks("[{'type': 'section', 'text': {'type': 'plain_text', 'text': 'Hello world'}}]")
				.build();

		when(_context.getEvent()).thenReturn(_event);
		when(_event.getType())
				.thenReturn("NOTIFICATION");
		when(_context.getTopic())
				.thenReturn(new GlobalTopic(IdnTopic.NOTIFICATION.getName()));

		when(_event.getHeader(anyString())).thenReturn(Optional.empty());

		when(_restClientProvider.getInternalRestClientWithServiceURL(DEFAULT_SERVICE, ServiceNames.SLACK))
				.thenReturn(_client);
		when(_atlasConfig.getString(SLACK_NOTIFICATION_SERVICE,
				DEFAULT_SERVICE)).thenReturn(DEFAULT_SERVICE);
		when(_atlasConfig.getString(SLACK_NOTIFY_URI,
				DEFAULT_NOTIFY_URI)).thenReturn(DEFAULT_NOTIFY_URI);
		when(_atlasConfig.getString(SLACK_GET_TENANTS_URI, DEFAULT_GET_TENANTS_URI)).thenReturn(DEFAULT_GET_TENANTS_URI);

		_slackService = new SlackService(_metricsUtil,
				_restClientProvider, _atlasConfig);
	}

	@Test
	public void testSlackMessageSuccess() {
		when(_client.post(any(),any())).thenReturn(getOKSlackResponse());
		_slackService.sendSlackNotifications(_slackNotificationRendered, _context);
		verify(_client, times(1)).post(eq(DEFAULT_NOTIFY_URI), any(SlackNotificationRendered.class));
	}

	@Test
	public void testSlackMessageError() {
		when(_client.post(any(),any())).thenReturn(getErrorSlackResponse());
		_slackService.sendSlackNotifications(_slackNotificationRendered, _context);
		verify(_metricsUtil).getTags(any(), any());
	}

	@Test(expected = RuntimeException.class)
	public void testSlackMessageRetryError() {
		when(_client.post(any(),any())).thenReturn(getErrorRetrySlackResponse());
		_slackService.sendSlackNotifications(_slackNotificationRendered, _context);
		verify(_metricsUtil).getTags(any(), any());
	}

	@Test
	public void slackGetTenantsSuccess() {
		when(_client.get(any())).thenReturn(getOKSlackGetTenantsResponse());
		List<String> tenants = _slackService.getSlackTenants();
		verify(_client, times(1)).get(DEFAULT_GET_TENANTS_URI);
		assertEquals(4, tenants.size());
		assertTrue(tenants.contains("acme-jovian"));
	}

    @Test
    public void slackGetTenantsEmpty() {
        when(_client.get(any())).thenThrow(NotFoundException.class);
		List<String> tenants = _slackService.getSlackTenants();
		verify(_client, times(1)).get(DEFAULT_GET_TENANTS_URI);
		assertTrue(tenants.isEmpty());
    }

	public static String getOKSlackGetTenantsResponse() {
		return JsonUtil.toJson(Collections.singletonMap("tenants", Arrays.asList("acme-solar", "acme-lunar", "acme-jovian", "acme-atlantis")));
	}

	public static String getOKSlackResponse() {
		return JsonUtil.toJson(new SlackNotificationResponse(true, "","", "", ""));
	}

	public static String getErrorSlackResponse() {
		return JsonUtil.toJson(new SlackNotificationResponse(false, "", "error", "", ""));
	}

	public static String getErrorRetrySlackResponse() {
		return JsonUtil.toJson(new SlackNotificationResponse(false, "", ERROR_RATE_LIMIT, "", ""));
	}
}
