/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.teams.service;

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
import com.sailpoint.notification.api.event.dto.TeamsNotificationRendered;
import com.sailpoint.notification.api.event.dto.TeamsNotificationResponse;
import com.sailpoint.notification.sender.common.lifecycle.NotificationMetricsUtil;
import com.sailpoint.utilities.JsonUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.sailpoint.notification.sender.teams.service.TeamsService.DEFAULT_GET_TENANTS_URI;
import static com.sailpoint.notification.sender.teams.service.TeamsService.DEFAULT_SERVICE;
import static com.sailpoint.notification.sender.teams.service.TeamsService.DEFAULT_NOTIFY_URI;
import static com.sailpoint.notification.sender.teams.service.TeamsService.ERROR_RATE_LIMIT;
import static com.sailpoint.notification.sender.teams.service.TeamsService.TEAMS_GET_TENANT_URI;
import static com.sailpoint.notification.sender.teams.service.TeamsService.TEAMS_NOTIFICATION_SERVICE;
import static com.sailpoint.notification.sender.teams.service.TeamsService.TEAMS_NOTIFICATION_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for testing TeamsService
 */
public class TeamsServiceTest {

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

	TeamsNotificationRendered _teamsNotificationRendered;

	TeamsService _teamsService;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		_teamsNotificationRendered = TeamsNotificationRendered.builder()
				.recipient(new RecipientBuilder()
						.withId("12345")
						.build())
				.text("Hello world")
				.title("Title")
				.messageJSON("")
				.build();

		when(_context.getEvent()).thenReturn(_event);
		when(_event.getType())
				.thenReturn("NOTIFICATION");
		when(_context.getTopic())
				.thenReturn(new GlobalTopic(IdnTopic.NOTIFICATION.getName()));

		when(_event.getHeader(anyString())).thenReturn(Optional.empty());

		when(_restClientProvider.getInternalRestClientWithServiceURL(DEFAULT_SERVICE, ServiceNames.TEAMS))
				.thenReturn(_client);
		when(_atlasConfig.getString(TEAMS_NOTIFICATION_SERVICE,
				DEFAULT_SERVICE)).thenReturn(DEFAULT_SERVICE);
		when(_atlasConfig.getString(TEAMS_NOTIFICATION_URI,
                DEFAULT_NOTIFY_URI)).thenReturn(DEFAULT_NOTIFY_URI);
		when(_atlasConfig.getString(TEAMS_GET_TENANT_URI,
				DEFAULT_GET_TENANTS_URI)).thenReturn(DEFAULT_GET_TENANTS_URI);

		_teamsService = new TeamsService(_metricsUtil,
				_restClientProvider, _atlasConfig);
	}

	@Test
	public void testTeamsMessageSuccess() {
		when(_client.post(any(),any())).thenReturn(getOKTeamsResponse());
		_teamsService.sendTeamsNotifications(_teamsNotificationRendered, _context);
		verify(_client, times(1)).post(eq(DEFAULT_NOTIFY_URI), any(TeamsNotificationRendered.class));
	}

	@Test
	public void testTeamsMessageError() {
		when(_client.post(any(),any())).thenReturn(getErrorTeamsResponse());
		_teamsService.sendTeamsNotifications(_teamsNotificationRendered, _context);
		verify(_metricsUtil).getTags(any(), any());
	}

	@Test(expected = RuntimeException.class)
	public void testTeamsMessageRetryError() {
		when(_client.post(any(),any())).thenReturn(getErrorRetryTeamsResponse());
		_teamsService.sendTeamsNotifications(_teamsNotificationRendered, _context);
		verify(_metricsUtil).getTags(any(), any());
	}

	@Test
	public void teamsGetTenantsSuccess() {
		when(_client.get(any())).thenReturn(getTeamsTenantsOKResponse());
		List<String> tenants = _teamsService.getTeamsTenants();
		verify(_client, times(1)).get(eq(DEFAULT_GET_TENANTS_URI));
		assertEquals(tenants.size(), 4);
		assertTrue(tenants.contains("acme-jovian"));
	}

	@Test
	public void teamsGetTenantsEmpty() {
		when(_client.get(any())).thenThrow(NotFoundException.class);
		List<String> tenants = _teamsService.getTeamsTenants();
		verify(_client, times(1)).get(eq(DEFAULT_GET_TENANTS_URI));
		assertEquals(tenants.size(), 0);
	}

	private static String getTeamsTenantsOKResponse() {
		return JsonUtil.toJson(Collections.singletonMap("tenants", Arrays.asList("acme-solar", "acme-lunar", "acme-jovian", "acme-atlantis")));
	}

	public static String getOKTeamsResponse() {
		return JsonUtil.toJson(new TeamsNotificationResponse(true, "",""));
	}

	public static String getErrorTeamsResponse() {
		return JsonUtil.toJson(new TeamsNotificationResponse(false, "", "error"));
	}

	public static String getErrorRetryTeamsResponse() {
		return JsonUtil.toJson(new TeamsNotificationResponse(false, "", ERROR_RATE_LIMIT));
	}
}
