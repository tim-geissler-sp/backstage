/*
 * Copyright (C) 2018 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.lifecycle;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.atlas.event.lifecycle.EventLifecycleMetricsReporter;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.iris.server.EventLifecycleListener;
import com.sailpoint.metrics.MetricsUtil;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.api.event.dto.NotificationRendered;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Metric reporter for Notification events
 */
@Singleton
public class NotificationLifecycleMetricsReporter implements EventLifecycleListener {

	private static final Log _log = LogFactory.getLog(EventLifecycleMetricsReporter.class);

	private final static String METRIC_NAME = NotificationLifecycleMetricsReporter.class.getName();

	private final static String NOTIFICATION_LATENCY = ".notification.latency";

	@Inject
	NotificationMetricsUtil _metricsUtil;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onComplete(int processorId, EventHandlerContext context) {
		if(!EventType.NOTIFICATION_RENDERED.equals(context.getEvent().getType())) {
			return;
		}

		try {

			NotificationRendered notificationRendered  = context.getEvent().getContent(NotificationRendered.class);
			Optional<Event> domainEvent = NotificationMetricsUtil.getDomainEvent(notificationRendered);
			if(!domainEvent.isPresent()) {
				return;
			}
			if(domainEvent.get().getTimestamp() == null) {
				return;
			}

			_log.debug("onComplete notification service " + domainEvent.get().getTimestamp() +
					" id " + domainEvent.get().getId());
			Map<String, String> tags = _metricsUtil.getTags(context, Optional.empty());

			String timerName = METRIC_NAME + NOTIFICATION_LATENCY;

			Duration consumerDuration = Duration.between(domainEvent.get().getTimestamp(), OffsetDateTime.now());
			MetricsUtil.getTimer(timerName, tags).update(consumerDuration.toNanos(), TimeUnit.NANOSECONDS);
			_log.debug("onComplete notification latency " + consumerDuration.toString());

		} catch (Exception e) {
			_log.error("Error reporting notification latency metric.", e);
		}
	}
}
