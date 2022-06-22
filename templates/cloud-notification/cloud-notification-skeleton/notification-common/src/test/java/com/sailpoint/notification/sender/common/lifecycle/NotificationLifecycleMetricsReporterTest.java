/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.lifecycle;

import com.google.common.collect.ImmutableMap;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.utilities.JsonUtil;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.client.GlobalTopic;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.api.event.dto.NotificationRendered;
import com.sailpoint.notification.sender.common.event.interest.matching.NotificationInterestMatchedBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NotificationLifecycleMetricsReporterTest {

	@Mock
	NotificationMetricsUtil _metricsUtil;

	@Mock
	EventHandlerContext _context;

	@Mock
	Event _event;

	@Mock
	NotificationRendered _notificationRendered;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		when(_notificationRendered.getDomainEvent())
				.thenReturn(ImmutableMap.of("contentJson", createEvent()));

		when(_event.getType())
				.thenReturn(EventType.NOTIFICATION_RENDERED);

		when(_event.getContent(NotificationRendered.class))
				.thenReturn(_notificationRendered);

		when(_context.getTopic())
				.thenReturn(new GlobalTopic(IdnTopic.NOTIFICATION.getName()));

		when(_context.getEvent()).thenReturn(_event);

	}

	@Test
	public void notificationLifecycleMetricsReporterTest() {
		NotificationLifecycleMetricsReporter notificationLifecycleMetricsReporter
				= new NotificationLifecycleMetricsReporter();

		notificationLifecycleMetricsReporter._metricsUtil = _metricsUtil;
		notificationLifecycleMetricsReporter.onComplete(1, _context);

		verify(_event, times(1)).getType();
		verify(_event, times(1)).getContent(NotificationRendered.class);
		verify(_metricsUtil, times(1)).getTags(any(), any());
	}

	private String createEvent() {
		Event eventOriginal = EventBuilder.withTypeAndContentJson("ACCESS_APPROVAL_REQUESTED", "test event")
				.addHeader(EventHeaders.POD, "dev")
				.addHeader(EventHeaders.ORG, "acme-solar")
				.build();

		NotificationInterestMatchedBuilder builder = new NotificationInterestMatchedBuilder(
				UUID.randomUUID().toString(), eventOriginal)
				.withRecipientId("1234")
				.withNotificationKey("approval_request")
				.withCategoryName("email")
				.withInterestName("Access Approval Request");


		return JsonUtil.toJson(builder.build());
	}
}
