/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.sender.common.service;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.idn.RestClientProvider;
import com.sailpoint.atlas.idn.ServiceNames;
import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import com.sailpoint.mantisclient.BaseRestClient;
import com.sailpoint.notification.api.event.dto.SlackNotification;
import com.sailpoint.notification.api.event.dto.SlackNotificationRendered;
import com.sailpoint.notification.api.event.dto.SlackNotificationResponse;
import com.sailpoint.notification.api.event.dto.TeamsNotification;
import com.sailpoint.notification.api.event.dto.TeamsNotificationRendered;
import com.sailpoint.notification.sender.common.exception.InvalidNotificationException;
import com.sailpoint.notification.sender.email.Validator;
import com.sailpoint.notification.sender.slack.service.SlackService;
import com.sailpoint.notification.sender.teams.service.TeamsService;
import com.sailpoint.utilities.JsonUtil;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.api.event.dto.NotificationRendered;
import com.sailpoint.notification.sender.email.MailClient;
import com.sailpoint.notification.sender.email.service.model.Mail;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.params.SetParams;

import java.util.UUID;

import static com.sailpoint.notification.sender.common.event.NotificationRenderedEventHandler.SLACK_EVENT_TYPE_HEADER;
import static com.sailpoint.notification.sender.common.event.NotificationRenderedEventHandler.TEAMS_EVENT_TYPE_HEADER;
import static com.sailpoint.notification.sender.slack.service.SlackService.SLACK_NOTIFICATION_SERVICE;
import static com.sailpoint.notification.sender.slack.service.SlackService.SLACK_NOTIFY_URI;
import static com.sailpoint.notification.sender.teams.service.TeamsService.TEAMS_NOTIFICATION_SERVICE;
import static com.sailpoint.notification.sender.teams.service.TeamsService.TEAMS_NOTIFICATION_URI;

/**
 * Service that exclusively houses debugging methods
 */
@Singleton
public class SenderDebugService {

	private static final Log _log = LogFactory.getLog(SenderDebugService.class);

	@Inject
	Provider<EventService> _eventService;

	@Inject
	Provider<RedisPool> _redisPoolProvider;

	@Inject
	MailClient _mailClient;

	@Inject
	Validator _validator;

	@Inject
	RestClientProvider _restClientProvider;

	@Inject
	AtlasConfig _atlasConfig;

	public final static String REDIS_DEBUG_KEY = "REDIS_DEBUG_KEY";

	/**
	 * Publishes a MAIL event to Kafka event stream to be picked up executors. This allows applications without access
	 * to Atlas Iris to queue MAIL events through the REST application.
	 * @param notificationRendered NotificationRendered entity object
	 */
	public String publishMailEvent(NotificationRendered notificationRendered) {
		String uniqueKey = "test" + UUID.randomUUID();

		Event event = EventBuilder.withTypeAndContent(EventType.NOTIFICATION_RENDERED, notificationRendered)
				.addHeader(REDIS_DEBUG_KEY, uniqueKey)
				.addHeader("SES_CONFIG_SET_NAME", "HermesDevTestEventDesitination")
				.build();
		_eventService.get().publishAsync(IdnTopic.NOTIFICATION, event);

		return uniqueKey;
	}

	public String publishSlackEvent(SlackNotificationRendered notificationRendered) {
		String uniqueKey = "test" + UUID.randomUUID();

		Event event = EventBuilder.withTypeAndContent(EventType.NOTIFICATION_RENDERED, notificationRendered)
				.addHeader(REDIS_DEBUG_KEY, uniqueKey)
				.addHeader(SLACK_EVENT_TYPE_HEADER, true)
				.build();
		_eventService.get().publishAsync(IdnTopic.NOTIFICATION, event);

		return uniqueKey;
	}

	public String publishTeamsEvent(TeamsNotificationRendered notificationRendered) {
		String uniqueKey = "test" + UUID.randomUUID();

		Event event = EventBuilder.withTypeAndContent(EventType.NOTIFICATION_RENDERED, notificationRendered)
				.addHeader(REDIS_DEBUG_KEY, uniqueKey)
				.addHeader(TEAMS_EVENT_TYPE_HEADER, true)
				.build();
		_eventService.get().publishAsync(IdnTopic.NOTIFICATION, event);

		return uniqueKey;
	}

	/**
	 * Retrieves the value for the given key from Redis.
	 * @param key redis key
	 * @return value
	 */
	public String retrieveFromStore(String key) {
		return _redisPoolProvider.get().exec(jedis -> {
			String result = jedis.get(key);
			_log.info("Retrieve key: " + key + " from redis store value: " + result);
			return result;
		});
	}

	/**
	 * Sends teas message using the teams integration service but doesn't pass the entity through a validator
	 * and also persists message in redis to enable E2E tests.
	 *
	 * @param notificationRendered event
	 */
	public void sendTeamsWithDebugging(TeamsNotificationRendered notificationRendered, String debugKey) {
		try {
			TeamsNotification teamsNotification = TeamsNotification.builder()
					.recipient(notificationRendered.getRecipient())
					.text(notificationRendered.getText())
					.title(notificationRendered.getTitle())
					.messageJSON(notificationRendered.getMessageJSON())
					.isSubscription(notificationRendered.getIsSubscription())
					.approvalId(notificationRendered.getApprovalId())
					.requestId(notificationRendered.getRequestId())
					.notificationType(notificationRendered.getNotificationType())
					.customFields(notificationRendered.getCustomFields())
					.org(notificationRendered.getOrg())
					.autoApprovalData(notificationRendered.getAutoApprovalData())
					.build();

			String slackURL = _atlasConfig.getString(TEAMS_NOTIFICATION_SERVICE,
					TeamsService.DEFAULT_SERVICE);
			String slackURI = _atlasConfig.getString(TEAMS_NOTIFICATION_URI,
					TeamsService.DEFAULT_NOTIFY_URI);

			BaseRestClient client = _restClientProvider.getInternalRestClientWithServiceURL(slackURL, ServiceNames.SLACK);
			SlackNotificationResponse response = JsonUtil.parse(SlackNotificationResponse.class,
					client.post(slackURI, teamsNotification));
			if(response == null) {
				throw new InvalidNotificationException("Empty response from teams integration during E2E test");
			}
			if(!response.isOk()) {
				_log.error("Teams app responded with error during E2E test: " + response.toString());
				writeToStore(debugKey, JsonUtil.toJson(response));
			} else {
				writeToStore(debugKey, JsonUtil.toJson(teamsNotification));
			}
		} catch (Exception e) {
			_log.warn("Teams Debug service error.", e);
			if (e.getMessage() != null) {
				writeToStore(debugKey, e.getMessage());
			}
		}
	}

	/**
	 * Sends slack message using the slack integration service but doesn't pass the entity through a validator
	 * and also persists message in redis to enable E2E tests.
	 *
	 * @param notificationRendered event
	 */
	public void sendSlackWithDebugging(SlackNotificationRendered notificationRendered, String debugKey) {
		try {
			SlackNotification slackNotification = SlackNotification.builder()
					.recipient(notificationRendered.getRecipient())
					.text(notificationRendered.getText())
					.blocks(notificationRendered.getBlocks())
					.attachments(notificationRendered.getAttachments())
					.notificationType(notificationRendered.getNotificationType())
					.isSubscription(notificationRendered.getIsSubscription())
					.org(notificationRendered.getOrg())
					.approvalId(notificationRendered.getApprovalId())
					.requestId(notificationRendered.getRequestId())
					.autoApprovalData(notificationRendered.getAutoApprovalData())
					.customFields(notificationRendered.getCustomFields())
					.build();

			String slackURL = _atlasConfig.getString(SLACK_NOTIFICATION_SERVICE,
					SlackService.DEFAULT_SERVICE);
			String slackURI = _atlasConfig.getString(SLACK_NOTIFY_URI,
					SlackService.DEFAULT_NOTIFY_URI);

			BaseRestClient client = _restClientProvider.getInternalRestClientWithServiceURL(slackURL, ServiceNames.SLACK);
			SlackNotificationResponse response = JsonUtil.parse(SlackNotificationResponse.class,
						client.post(slackURI, slackNotification));
			if(response == null) {
				throw new InvalidNotificationException("Empty response from slack integration during E2E test");
			}
			if(!response.isOk()) {
				_log.error("Slack app responded with error during E2E test: " + response.toString());
				writeToStore(debugKey, JsonUtil.toJson(response));
			}
			writeToStore(debugKey+"-request", JsonUtil.toJson(slackNotification));
		} catch (Exception e) {
			_log.warn("Slack Debug service error.", e);
			if (e.getMessage() != null) {
				writeToStore(debugKey, e.getMessage());
			}
		}
	}


	/**
	 * Sends email using the mail client implementation that is bound to the interface similar to MailService#sendMail
	 * but doesn't pass the entity through a validator and also persists mail in redis to enable E2E tests.
	 *
	 * @param notificationRendered event
	 */
	public void sendMailWithDebugging(NotificationRendered notificationRendered, String debugKey) {
		try {
			Mail.MailBuilder mailBuilder = new Mail.MailBuilder();
			if(debugKey != null && debugKey.startsWith("testLargeEvent")) {
				mailBuilder.withConfigurationSet("HermesDevTestEventDesitination")
						.withToAddress(notificationRendered.getRecipient().getEmail())
						.withFromAddress(notificationRendered.getFrom())
						.withSubject(notificationRendered.getSubject())
						.withHtml("Test Large Event")
						.withReplyToAddress(notificationRendered.getReplyTo());
			} else {
				mailBuilder.withConfigurationSet("HermesDevTestEventDesitination")
						.withToAddress(notificationRendered.getRecipient().getEmail())
						.withFromAddress(notificationRendered.getFrom())
						.withSubject(notificationRendered.getSubject())
						.withHtml(notificationRendered.getBody())
						.withReplyToAddress(notificationRendered.getReplyTo());
				_validator.notificationRenderedValidator(notificationRendered);
				_mailClient.sendMail(mailBuilder.build());
			}
			writeToStore(debugKey, JsonUtil.toJson(mailBuilder.build()));
		} catch (Exception e) {
			_log.warn("Mail Debug service error.", e);
			if (e.getMessage() != null) {
				writeToStore(debugKey, e.getMessage());
			}
		}
	}

	/**
	 * Write the key/value to Redis
	 * @param key redis key
	 * @param value value
	 * @return Result of jedis set which is a status code reply
	 */
	private String writeToStore(String key, String value) {
		return _redisPoolProvider.get().exec(jedis -> {
			String result = jedis.set(key, value, SetParams.setParams().nx().ex(60));
			_log.info("Result of notification " + value + "  writing to store with key " + key + " : " + result);
			return result;
		});
	}

}
