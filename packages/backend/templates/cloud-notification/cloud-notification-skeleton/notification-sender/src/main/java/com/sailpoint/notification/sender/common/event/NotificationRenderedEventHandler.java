/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.event;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.server.EventHandler;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.api.event.dto.Notification;
import com.sailpoint.notification.api.event.dto.NotificationRendered;
import com.sailpoint.notification.api.event.dto.SlackNotificationRendered;
import com.sailpoint.notification.api.event.dto.TeamsNotificationRendered;
import com.sailpoint.notification.sender.common.exception.InvalidNotificationException;
import com.sailpoint.notification.sender.common.lifecycle.NotificationMetricsUtil;
import com.sailpoint.notification.sender.common.service.SenderDebugService;
import com.sailpoint.notification.sender.email.service.MailService;
import com.sailpoint.notification.sender.slack.service.SlackService;
import com.sailpoint.notification.sender.teams.service.TeamsService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.ThreadContext;

import java.util.Optional;

/**
 * MailEventHandler
 */
@Singleton
public class NotificationRenderedEventHandler implements EventHandler {

	public static String SLACK_EVENT_TYPE_HEADER = "SLACK_EVENT_TYPE";
	public static String TEAMS_EVENT_TYPE_HEADER = "TEAMS_EVENT_TYPE";
	private static final String MESSAGE_ID = "message_id";

	private static final Log _log = LogFactory.getLog(NotificationRenderedEventHandler.class);

	private final MailService _mailService;

	private final SlackService _slackService;

	private final TeamsService _teamsService;

	private final SenderDebugService _senderDebugService;

	@Inject
	public NotificationRenderedEventHandler(MailService mailService, SlackService slackService,
											TeamsService teamsService, SenderDebugService senderDebugService) {
		_mailService = mailService;
		_slackService = slackService;
		_teamsService = teamsService;
		_senderDebugService = senderDebugService;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handleEvent(EventHandlerContext context) {

		Event notificationRenderedEvent = context.getEvent();
		if(notificationRenderedEvent.getHeader(SLACK_EVENT_TYPE_HEADER).isPresent()) {
			handleSlackEvent(notificationRenderedEvent, context);
		} else if(notificationRenderedEvent.getHeader(TEAMS_EVENT_TYPE_HEADER).isPresent()) {
			handleTeamsEvent(notificationRenderedEvent, context);
		} else {
			handleEmailEvent(notificationRenderedEvent);
		}

	}
	private void handleTeamsEvent(Event notificationRenderedEvent, EventHandlerContext context) {
		TeamsNotificationRendered notificationRendered = notificationRenderedEvent.getContent(TeamsNotificationRendered.class);
		notificationRendered.setOrg(notificationRenderedEvent.getHeader("org").orElse(null));
		_log.info("Handling NOTIFICATION_RENDERED event for Teams " + notificationRenderedEvent.getId() +
				" " + notificationRendered.toString());
		Optional<String> debugHeader = getDebugHeader(notificationRenderedEvent, notificationRendered);
		if(debugHeader.isPresent()) {
			_senderDebugService.sendTeamsWithDebugging(notificationRendered, debugHeader.get());
		} else {
			_teamsService.sendTeamsNotifications(notificationRendered, context);
		}
	}

	private void handleSlackEvent(Event notificationRenderedEvent, EventHandlerContext context) {
		SlackNotificationRendered notificationRendered = notificationRenderedEvent.getContent(SlackNotificationRendered.class);
		notificationRendered.setOrg(notificationRenderedEvent.getHeader("org").orElse(null));
		_log.info("Handling NOTIFICATION_RENDERED event for Slack " + notificationRenderedEvent.getId() +
				" " + notificationRendered.toString());
		Optional<String> debugHeader = getDebugHeader(notificationRenderedEvent, notificationRendered);
		if(debugHeader.isPresent()) {
			_senderDebugService.sendSlackWithDebugging(notificationRendered, debugHeader.get());
		} else {
			_slackService.sendSlackNotifications(notificationRendered, context);
		}
	}

	private void handleEmailEvent(Event notificationRenderedEvent) {
		try {
			String messageId = notificationRenderedEvent.getHeader(MESSAGE_ID).orElse(null);
			ThreadContext.put(MESSAGE_ID, messageId);

			NotificationRendered notificationRendered = notificationRenderedEvent.getContent(NotificationRendered.class);
			_log.info("Handling NOTIFICATION_RENDERED event " + notificationRenderedEvent.getId() +
					" " + notificationRendered.toString() + " message_id:" + messageId);
			Optional<String> debugHeader = getDebugHeader(notificationRenderedEvent, notificationRendered);
			if(debugHeader.isPresent()) {
				_senderDebugService.sendMailWithDebugging(notificationRendered, debugHeader.get());
			} else {
				try {
					_mailService.sendMail(notificationRendered);
				} catch (InvalidNotificationException e) {
					_log.warn("Failed to handle NOTIFICATION_RENDERED event, exception: ", e);
				}
			}
		} finally {
			ThreadContext.remove(MESSAGE_ID);
		}
	}

	private Optional<String> getDebugHeader(Event event, Notification notification) {
		if(event.getHeader(SenderDebugService.REDIS_DEBUG_KEY).isPresent()) {
			return event.getHeader(SenderDebugService.REDIS_DEBUG_KEY);
		} else {
			Optional<Event> domainEvent = NotificationMetricsUtil.getDomainEvent(notification);
			if(domainEvent.isPresent()) {
				return domainEvent.get().getHeader(SenderDebugService.REDIS_DEBUG_KEY);
			}
		}
		return Optional.empty();
	}
}
