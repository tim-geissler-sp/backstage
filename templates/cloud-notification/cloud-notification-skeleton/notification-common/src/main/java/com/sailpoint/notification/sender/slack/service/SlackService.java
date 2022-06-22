/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.slack.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.idn.RestClientProvider;
import com.sailpoint.atlas.idn.ServiceNames;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.mantisclient.BaseRestClient;
import com.sailpoint.metrics.MetricsUtil;
import com.sailpoint.notification.api.event.dto.SlackNotification;
import com.sailpoint.notification.api.event.dto.SlackNotificationRendered;
import com.sailpoint.notification.api.event.dto.SlackNotificationResponse;
import com.sailpoint.notification.sender.common.exception.InvalidNotificationException;
import com.sailpoint.notification.sender.common.lifecycle.NotificationMetricsUtil;
import com.sailpoint.notification.sender.common.rest.TenantsResponseDto;
import com.sailpoint.utilities.JsonUtil;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Slack Notification Service.
 */
@Singleton
@CommonsLog
public class SlackService {

	public final static String ERROR_RATE_LIMIT = "rate_limited";
	public final static String ERROR_RECIPIENT_NOT_FOUND = "recipient_not_found";
	public final static String SLACK_NOTIFICATION_SERVICE = "SP_SLACK_INTEGRATION_NOTIFICATION_SERVICE";
	public final static String SLACK_NOTIFY_URI = "SP_SLACK_INTEGRATION_NOTIFICATION_URI";
	public final static String SLACK_GET_TENANTS_URI = "SLACK_GET_TENANT_URI";
	public final static String DEFAULT_SERVICE = "https://slack-integration.cloud.sailpoint.com";
	public final static String DEFAULT_NOTIFY_URI = "/slack/messages/send-message";
	public final static String DEFAULT_GET_TENANTS_URI = "/v3/api/tenants";

	private final NotificationMetricsUtil _metricsUtil;
	private final RestClientProvider _restClientProvider;
	private final String _slackURL;
	private final String _slackNotifyURI;
	private final String _slackGetTenantsURI;

	public static final String METRIC_PREFIX = SlackService.class.getName();

	@Inject
	public SlackService(NotificationMetricsUtil metricsUtil, RestClientProvider restClientProvider, AtlasConfig atlasConfig) {
		_metricsUtil = metricsUtil;
		_restClientProvider = restClientProvider;
		_slackURL = atlasConfig.getString(SLACK_NOTIFICATION_SERVICE,
				DEFAULT_SERVICE);
		_slackNotifyURI = atlasConfig.getString(SLACK_NOTIFY_URI,
				DEFAULT_NOTIFY_URI);
		_slackGetTenantsURI = atlasConfig.getString(SLACK_GET_TENANTS_URI, DEFAULT_GET_TENANTS_URI);
	}
	/**
	 * Enum of possible mail metric results
	 */
	public enum SlackResultMetricName {
		SUCCESS,
		FAILURE
	}

	/**
	 * Sends slack notification to slack integration service.
	 * @param notificationRendered notification event
	 * @param context event handler context
	 */
	public void sendSlackNotifications(SlackNotificationRendered notificationRendered, EventHandlerContext context) {

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

			BaseRestClient client = _restClientProvider.getInternalRestClientWithServiceURL(_slackURL, ServiceNames.SLACK);
			SlackNotificationResponse response = JsonUtil.parse(SlackNotificationResponse.class,
					client.post(_slackNotifyURI, slackNotification));
			if(response == null) {
				throw new InvalidNotificationException("Empty response from slack integration");
			}

			if(!response.isOk()) {
				if(ERROR_RECIPIENT_NOT_FOUND.equals(response.getError())) {
					//Error if IDN user is not using slack app ignore it.
					log.warn("Trying to notify user without slack app: " + response.toString());
					return;
				}
				log.error("Slack app responded with error: " + response.toString());
				if(ERROR_RATE_LIMIT.equals(response.getError())) {
					throw new SlackRateLimitException("Slack app responded with rate limit error: " + response.toString());
				} else {
					throw new InvalidNotificationException("Slack app responded with error: " + response.toString());
				}
			}
			MetricsUtil.getCounter(METRIC_PREFIX + ".sendSlack." + SlackResultMetricName.SUCCESS,
					_metricsUtil.getTags(context, Optional.empty())).inc();
		} catch (Exception ex) {
			handleFailure(ex, context, notificationRendered);
		}
	}

	public List<String> getSlackTenants() {
		try {
			BaseRestClient client = _restClientProvider.getInternalRestClientWithServiceURL(_slackURL, ServiceNames.SLACK);
			TenantsResponseDto tenantsResponseDto = JsonUtil.parse(TenantsResponseDto.class, client.get(_slackGetTenantsURI));
			return tenantsResponseDto.getTenants();
		} catch (Exception ex) {
			log.error("slack get tenant responded with error", ex);
			return Collections.emptyList();
		}
	}

	/**
	 * Function will update FAILURE metric and will no retry for know slack exception errors.
	 * @param ex - original error.
	 * @param context - event context.
	 * @param event notification event.
	 */
	private void handleFailure(Exception ex, EventHandlerContext context, SlackNotificationRendered event) {
		Throwable rootCause = ExceptionUtils.getRootCause(ex) == null ? ex : ExceptionUtils.getRootCause(ex);
		final Map<String, String> tags = _metricsUtil.getTags(context, Optional.of(rootCause.getClass().getSimpleName()));
		MetricsUtil.getCounter(METRIC_PREFIX + ".sendSlack." + SlackResultMetricName.FAILURE, tags).inc();

		if(ex instanceof InvalidNotificationException) {
			log.error("Slack message rejected or invalid for event " + event.toString(), ex);
		} else {
			log.error(ex);
			throw new RuntimeException(ex);
		}
	}
}
