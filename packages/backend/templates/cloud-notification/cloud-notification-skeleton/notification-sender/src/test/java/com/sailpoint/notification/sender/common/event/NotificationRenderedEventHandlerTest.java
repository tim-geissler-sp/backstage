/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.event;

import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.api.event.RecipientBuilder;
import com.sailpoint.notification.api.event.dto.NotificationRendered;
import com.sailpoint.notification.api.event.dto.SlackNotificationRendered;
import com.sailpoint.notification.api.event.dto.TeamsNotificationRendered;
import com.sailpoint.notification.sender.common.exception.InvalidNotificationException;
import com.sailpoint.notification.sender.common.service.SenderDebugService;
import com.sailpoint.notification.sender.email.service.MailService;
import com.sailpoint.notification.sender.slack.service.SlackService;
import com.sailpoint.notification.sender.teams.service.TeamsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.sailpoint.notification.sender.common.event.NotificationRenderedEventHandler.SLACK_EVENT_TYPE_HEADER;
import static com.sailpoint.notification.sender.common.event.NotificationRenderedEventHandler.TEAMS_EVENT_TYPE_HEADER;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for NotificationRenderedEventHandler.
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationRenderedEventHandlerTest {

	@Mock
	MailService _mailService;
	@Mock
	SlackService _slackService;
	@Mock
	TeamsService _teamsService;
	@Mock
	SenderDebugService _senderDebugService;

	@Mock
	EventHandlerContext _eventHandlerContext;

	NotificationRenderedEventHandler _notificationRenderedEventHandler;

	@Before
	public void setUp() {
		reset(_eventHandlerContext);
		reset(_mailService);
		reset(_slackService);
		reset(_teamsService);
		_notificationRenderedEventHandler = new NotificationRenderedEventHandler(_mailService, _slackService,
				_teamsService, _senderDebugService);
	}

	@Test
	public void testNotificationRenderedEventHandlerMail() throws InvalidNotificationException {
		when(_eventHandlerContext.getEvent()).thenReturn(getMailEvent());
		_notificationRenderedEventHandler.handleEvent(_eventHandlerContext);
		verify(_mailService, times(1)).sendMail(any());
		verify(_slackService, times(0)).sendSlackNotifications(any(), any());
		verify(_teamsService, times(0)).sendTeamsNotifications(any(), any());
	}

	@Test
	public void testNotificationRenderedEventHandlerSlack() throws InvalidNotificationException {
		when(_eventHandlerContext.getEvent()).thenReturn(getSlackEvent());
		_notificationRenderedEventHandler.handleEvent(_eventHandlerContext);
		verify(_mailService, times(0)).sendMail(any());
		verify(_slackService, times(1)).sendSlackNotifications(any(), any());
		verify(_teamsService, times(0)).sendTeamsNotifications(any(), any());
	}

	@Test
	public void testNotificationRenderedEventHandlerTeams() throws InvalidNotificationException {
		when(_eventHandlerContext.getEvent()).thenReturn(getTeamsEvent());
		_notificationRenderedEventHandler.handleEvent(_eventHandlerContext);
		verify(_mailService, times(0)).sendMail(any());
		verify(_slackService, times(0)).sendSlackNotifications(any(), any());
		verify(_teamsService, times(1)).sendTeamsNotifications(any(), any());
	}

	private Event getMailEvent() {
		EventBuilder builder = EventBuilder.withTypeAndContent("NOTIFICATION_RENDERED", NotificationRendered.builder()
				.recipient(new RecipientBuilder()
						.withEmail("to@to")
						.build())
				.from("from@from")
				.subject("test")
				.body("<body>hello<br />html</body>")
				.build())
				.addHeader("message_id", "12345");
		return builder.build();
	}

	private Event getSlackEvent() {
		EventBuilder builder = EventBuilder.withTypeAndContent("NOTIFICATION_RENDERED", SlackNotificationRendered.builder()
				.recipient(new RecipientBuilder()
						.withId("12345")
						.build())
				.text("Hello world")
				.attachments("[{'pretext': 'pre-hello', 'text': 'text-world'}]")
				.blocks("[{'type': 'section', 'text': {'type': 'plain_text', 'text': 'Hello world'}}]")
				.build());
		builder.addHeader(SLACK_EVENT_TYPE_HEADER, true);
		return builder.build();
	}

	private Event getTeamsEvent() {
		EventBuilder builder = EventBuilder.withTypeAndContent("NOTIFICATION_RENDERED", TeamsNotificationRendered.builder()
				.recipient(new RecipientBuilder()
						.withId("12345")
						.build())
				.text("Hello world Text")
				.title("Hello world Title")
				.messageJSON("[{'type': 'section', 'text': {'type': 'plain_text', 'text': 'Hello world'}}]")
				.build());
		builder.addHeader(TEAMS_EVENT_TYPE_HEADER, true);
		return builder.build();
	}
}
