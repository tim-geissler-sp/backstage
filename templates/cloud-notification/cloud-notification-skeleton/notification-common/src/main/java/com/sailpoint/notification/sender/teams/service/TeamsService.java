/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.teams.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.idn.RestClientProvider;
import com.sailpoint.atlas.idn.ServiceNames;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.mantisclient.BaseRestClient;
import com.sailpoint.metrics.MetricsUtil;
import com.sailpoint.notification.api.event.dto.TeamsNotification;
import com.sailpoint.notification.api.event.dto.TeamsNotificationRendered;
import com.sailpoint.notification.api.event.dto.TeamsNotificationResponse;
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
 * MS Team Notification Service.
 */
@Singleton
@CommonsLog
public class TeamsService {

	public final static String ERROR_RATE_LIMIT = "rate_limited";
	public final static String ERROR_RECIPIENT_NOT_FOUND = "recipient_not_found";
	public final static String TEAMS_NOTIFICATION_SERVICE = "SP_TEAMS_INTEGRATION_NOTIFICATION_SERVICE";
	public final static String TEAMS_NOTIFICATION_URI = "SP_TEAMS_INTEGRATION_NOTIFICATION_URI";
	public final static String TEAMS_GET_TENANT_URI = "TEAMS_GET_TENANT_URI";
	public final static String DEFAULT_SERVICE = "https://teams-integration.cloud.sailpoint.com";
	public final static String DEFAULT_NOTIFY_URI = "/v3/api/notify";
	public final static String DEFAULT_GET_TENANTS_URI = "/v3/api/tenants";

	private final NotificationMetricsUtil _metricsUtil;
	private final RestClientProvider _restClientProvider;
	private final String _teamsURL;
	private final String _teamsNotifyURI;
	private final String _teamsGetTenantURI;

	public static final String METRIC_PREFIX = TeamsService.class.getName();

	@Inject
	public TeamsService(NotificationMetricsUtil metricsUtil, RestClientProvider restClientProvider, AtlasConfig atlasConfig) {
		_metricsUtil = metricsUtil;
		_restClientProvider = restClientProvider;
		_teamsURL = atlasConfig.getString(TEAMS_NOTIFICATION_SERVICE,
				DEFAULT_SERVICE);
		_teamsNotifyURI = atlasConfig.getString(TEAMS_NOTIFICATION_URI,
				DEFAULT_NOTIFY_URI);
		_teamsGetTenantURI = atlasConfig.getString(TEAMS_GET_TENANT_URI,
				DEFAULT_GET_TENANTS_URI);
	}
	/**
	 * Enum of possible teams metric results
	 */
	public enum TeamsResultMetricName {
		SUCCESS,
		FAILURE
	}

	/**
	 * Gets tenants that are setup to use teams.
	 * @return list of tenants
	 */
	public List<String> getTeamsTenants() {
		try {
			BaseRestClient client = _restClientProvider.getInternalRestClientWithServiceURL(_teamsURL, ServiceNames.TEAMS);
			TenantsResponseDto tenantsResponseDto = JsonUtil.parse(TenantsResponseDto.class,
					client.get(_teamsGetTenantURI));
			return tenantsResponseDto.getTenants();
		} catch (Exception e) {
			log.error("teams get tenant responded with error", e);
			return Collections.emptyList();
		}
	}

	/**
	 * Sends teams notification to teams integration service.
	 * @param notificationRendered notification event
	 * @param context event handler context
	 */
	public void sendTeamsNotifications(TeamsNotificationRendered notificationRendered, EventHandlerContext context) {

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

			BaseRestClient client = _restClientProvider.getInternalRestClientWithServiceURL(_teamsURL, ServiceNames.TEAMS);

			TeamsNotificationResponse response = JsonUtil.parse(TeamsNotificationResponse.class,
					client.post(_teamsNotifyURI, teamsNotification));
			if(response == null) {
				throw new InvalidNotificationException("Empty response from teams integration");
			}

			if(!response.isOk()) {
				if(ERROR_RECIPIENT_NOT_FOUND.equals(response.getError())) {
					//Error if IDN user is not using teams app ignore it.
					log.warn("Trying to notify user without teams app: " + response.toString());
					return;
				}
				log.error("teams app responded with error: " + response.toString());
				if(ERROR_RATE_LIMIT.equals(response.getError())) {
					throw new TeamsRateLimitException("teams app responded with rate limit error: " + response.toString());
				} else {
					throw new InvalidNotificationException("teams app responded with error: " + response.toString());
				}
			}
			MetricsUtil.getCounter(METRIC_PREFIX + ".sendTeams." + TeamsResultMetricName.SUCCESS,
					_metricsUtil.getTags(context, Optional.empty())).inc();
		} catch (Exception ex) {
			handleFailure(ex, context, notificationRendered);
		}
	}

	/**
	 * Function will update FAILURE metric and will no retry for know teams exception errors.
	 * @param ex - original error.
	 * @param context - event context.
	 * @param event notification event.
	 */
	private void handleFailure(Exception ex, EventHandlerContext context, TeamsNotificationRendered event) {
		Throwable rootCause = ExceptionUtils.getRootCause(ex) == null ? ex : ExceptionUtils.getRootCause(ex);
		final Map<String, String> tags = _metricsUtil.getTags(context, Optional.of(rootCause.getClass().getSimpleName()));
		MetricsUtil.getCounter(METRIC_PREFIX + ".sendTeams." + TeamsResultMetricName.FAILURE, tags).inc();

		if(ex instanceof InvalidNotificationException) {
			log.error("teams message rejected or invalid for event " + event.toString(), ex);
		} else {
			log.error(ex);
			throw new RuntimeException(ex);
		}
	}
}
