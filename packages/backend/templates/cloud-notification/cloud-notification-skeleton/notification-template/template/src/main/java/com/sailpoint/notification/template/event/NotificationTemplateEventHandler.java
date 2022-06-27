/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.event;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.server.EventHandler;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.api.event.dto.NotificationRendered;
import com.sailpoint.notification.api.event.dto.Recipient;
import com.sailpoint.notification.api.event.dto.SlackNotificationRendered;
import com.sailpoint.notification.api.event.dto.TeamsNotificationRendered;
import com.sailpoint.notification.sender.common.event.interest.matching.dto.NotificationInterestMatched;
import com.sailpoint.notification.sender.common.event.userpreferences.dto.UserPreferencesMatched;
import com.sailpoint.notification.context.service.GlobalContextService;
import com.sailpoint.notification.template.common.model.TemplateMediumDto;
import com.sailpoint.notification.template.context.TemplateContext;
import com.sailpoint.notification.template.service.NotificationTemplateDebugService;
import com.sailpoint.notification.template.service.NotificationTemplateService;
import com.sailpoint.utilities.StringUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.sailpoint.notification.context.service.GlobalContextService.EMAIL_OVERRIDE;

/**
 * Event handler for 'NOTIFICATION_PREFERENCES_MATCHED' events
 */
@Singleton
public class NotificationTemplateEventHandler implements EventHandler {

	public static String SLACK_EVENT_TYPE_HEADER = "SLACK_EVENT_TYPE";
	public static String TEAMS_EVENT_TYPE_HEADER = "TEAMS_EVENT_TYPE";


	@VisibleForTesting
	static Log _log = LogFactory.getLog(NotificationTemplateEventHandler.class);

	private final NotificationTemplateService _notificationTemplateService;

	private final NotificationTemplateDebugService _notificationTemplateDebugService;

	private final GlobalContextService _globalContextService;

	@Inject
	NotificationTemplateEventHandler(NotificationTemplateService notificationTemplateService,
									 NotificationTemplateDebugService notificationTemplateDebugService,
									 GlobalContextService globalContextService) {
		_notificationTemplateService = notificationTemplateService;
		_notificationTemplateDebugService = notificationTemplateDebugService;
		_globalContextService = globalContextService;

	}

	@Override
	public void handleEvent(EventHandlerContext context) {

		try {
			// Process incoming event
			Event event = context.getEvent();
			final String tenant = event.ensureHeader(EventHeaders.ORG);
			final UserPreferencesMatched userPreferencesMatched = event.getContent(UserPreferencesMatched.class);
			final String notificationKey = userPreferencesMatched.getNotificationKey();
			final String medium = userPreferencesMatched.getMedium();
			final Optional<String> brand = userPreferencesMatched.getBrand();
			final Locale locale = Locale.ENGLISH;
			final Event domainEvent = userPreferencesMatched.getDomainEvent();
			Recipient recipient = userPreferencesMatched.getRecipient();

			_log.info("Handling " + event.getType() + " event " + event.getId() +
					" " + userPreferencesMatched.toString());

			if (notificationKey == null) {
				throw new IllegalStateException("Template Notification Key are required.");
			}

			if (medium == null) {
				throw new IllegalStateException("Medium is required.");
			}

			//Get global context, and also retrieve emailOverride
			Map<String, Object> globalContext = brand.isPresent() ? _globalContextService.getContext(tenant, brand.get()) :
					_globalContextService.getDefaultContext(tenant);

			// Get template context.
			final TemplateContext templateContext = _notificationTemplateService.getTemplateContext(null, event.getContentJson(), globalContext);
			final TemplateContext templateContextV2 = _notificationTemplateService.getTemplateContextV2(templateContext, event.getContentJson(), globalContext);
			renderTemplate(event,tenant, notificationKey, medium,locale, templateContextV2, domainEvent, globalContext, recipient);

		} catch (Exception e) {
			_log.error("Error processing event " + context.getEvent().getType(), e);
		}
	}

	private void renderTemplate(Event event, String tenant, String notificationKey, String medium, Locale locale, TemplateContext templateContextV2, Event domainEvent, Map<String, Object> globalContext, Recipient recipient) {
		if(medium.toUpperCase().equals(TemplateMediumDto.EMAIL.toString())) {
			renderEmailTemplate(event,tenant, notificationKey, medium,locale, templateContextV2, domainEvent, globalContext, recipient);
		} else if(medium.toUpperCase().equals(TemplateMediumDto.SLACK.toString())) {
			renderSlackTemplate(event, tenant, notificationKey, medium,locale, templateContextV2, domainEvent, recipient);
		} else if(medium.toUpperCase().equals(TemplateMediumDto.TEAMS.toString())) {
			renderTeamsTemplate(event, tenant, notificationKey, medium,locale, templateContextV2, domainEvent, recipient);
		}
	}

	private void renderEmailTemplate(final Event event, final String tenant, final String notificationKey,
									 final String medium, final Locale locale, TemplateContext templateContextV2,
									 final Event domainEvent, Map<String, Object> globalContext, Recipient recipient) {
		// Render email template.
		NotificationRendered notificationRendered = _notificationTemplateService
				.renderByNotificationKey(tenant, notificationKey, medium, locale, templateContextV2);

		if (notificationRendered == null) {
			throw new IllegalStateException("Unable to render notification template.");
		}

		String emailOverride = (String) globalContext.get(EMAIL_OVERRIDE);
		//Handle email override
		if (StringUtil.isNotNullOrEmpty(emailOverride) && !isExtendedNotificationEvent(domainEvent)) {
			String originalRecipient = recipient.getEmail();
			recipient = recipient
					.derive()
					.withEmail(emailOverride)
					.build();
			String newSubject =  String.format("[Original recipient: %s] %s", originalRecipient, notificationRendered.getSubject());
			notificationRendered = notificationRendered
					.derive()
					.subject(newSubject)
					.build();
			_log.info("Redirecting email to " + emailOverride + ". Subject " + newSubject);
		}

		// Handle debug or regular event
		if (event.getHeader(NotificationTemplateDebugService.REDIS_TEMPLATE_DEBUG_KEY).isPresent()) {
			// Debug event
			_log.info("Handling debug " + event.getType() + " event id " + event.getId());

			String header = event.getHeader(NotificationTemplateDebugService.REDIS_TEMPLATE_DEBUG_KEY).get();
			_notificationTemplateDebugService.writeToStore(header, notificationRendered.getSubject());
		} else {
			// Event to be published.
			Event publishEvent = EventBuilder.withTypeAndContent(EventType.NOTIFICATION_RENDERED,
					notificationRendered.derive()
							.domainEvent(domainEvent)
							.recipient(recipient)
							.notificationKey(notificationKey)
							.build())
					.build();

			// Publish event.
			_notificationTemplateService.publishEvent(publishEvent);
		}
	}

	private void renderSlackTemplate(final Event event, final String tenant, final String notificationKey,
									 final String medium, final Locale locale, TemplateContext templateContextV2,
									 final Event domainEvent, Recipient recipient) {
		// Render slack template.
		SlackNotificationRendered slackNotificationRendered = _notificationTemplateService
				.renderSlackByNotificationKey(tenant, notificationKey, medium, locale, templateContextV2);

		if (slackNotificationRendered == null) {
			throw new IllegalStateException("Unable to render slack notification template.");
		}

		// Handle debug or regular event
		if (event.getHeader(NotificationTemplateDebugService.REDIS_TEMPLATE_DEBUG_KEY).isPresent()) {
			// Debug event
			_log.info("Handling debug " + event.getType() + " event id " + event.getId());

			String header = event.getHeader(NotificationTemplateDebugService.REDIS_TEMPLATE_DEBUG_KEY).get();
			_notificationTemplateDebugService.writeToStore(header, slackNotificationRendered.getText() +
					" " + slackNotificationRendered.getBlocks() + " " + slackNotificationRendered.getAttachments());
		} else {
			// Event to be published.
			Event publishEvent = EventBuilder.withTypeAndContent(EventType.NOTIFICATION_RENDERED,
					slackNotificationRendered.derive()
							.domainEvent(domainEvent)
							.recipient(recipient)
							.notificationKey(notificationKey)
							.build())
					.addHeader(SLACK_EVENT_TYPE_HEADER, true)
					.build();

			// Publish event.
			_notificationTemplateService.publishEvent(publishEvent);
		}
	}

	private void renderTeamsTemplate(final Event event, final String tenant, final String notificationKey,
									 final String medium, final Locale locale, TemplateContext templateContextV2,
									 final Event domainEvent, Recipient recipient) {
		// Render teams template.
		TeamsNotificationRendered teamsNotificationRendered = _notificationTemplateService
				.renderTeamsMessageByNotificationKey(tenant, notificationKey, medium, locale, templateContextV2);

		if (teamsNotificationRendered == null) {
			throw new IllegalStateException("Unable to render teams notification template.");
		}

		// Handle debug or regular event
		if (event.getHeader(NotificationTemplateDebugService.REDIS_TEMPLATE_DEBUG_KEY).isPresent()) {
			// Debug event
			_log.info("Handling debug " + event.getType() + " event id " + event.getId());

			String header = event.getHeader(NotificationTemplateDebugService.REDIS_TEMPLATE_DEBUG_KEY).get();
			_notificationTemplateDebugService.writeToStore(header, teamsNotificationRendered.getTitle() +
					" " + teamsNotificationRendered.getText() + " " + teamsNotificationRendered.getMessageJSON());
		} else {
			// Event to be published.
			Event publishEvent = EventBuilder.withTypeAndContent(EventType.NOTIFICATION_RENDERED,
					teamsNotificationRendered.derive()
							.domainEvent(domainEvent)
							.recipient(recipient)
							.notificationKey(notificationKey)
							.build())
					.addHeader(TEAMS_EVENT_TYPE_HEADER, true)
					.build();

			// Publish event.
			_notificationTemplateService.publishEvent(publishEvent);
		}
	}

	private boolean isExtendedNotificationEvent(Event domainEvent) {
		try {
			if ("Extended Notification Event".equals(domainEvent.getContent(NotificationInterestMatched.class).getInterestName())) {
				return true;
			}
		} catch (Exception e) {
			_log.error(e);
		}
		return false;
	}
}
