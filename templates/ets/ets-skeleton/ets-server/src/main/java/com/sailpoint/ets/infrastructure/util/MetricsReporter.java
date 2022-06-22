/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.util;

import com.codahale.metrics.Gauge;
import com.google.common.collect.ImmutableMap;
import com.sailpoint.atlas.boot.core.web.TenantIdentifier;
import com.sailpoint.ets.domain.event.TriggerInvokedEvent;
import com.sailpoint.ets.domain.invocation.Invocation;
import com.sailpoint.ets.domain.status.SubscriptionStatus;
import com.sailpoint.ets.domain.trigger.TriggerId;
import com.sailpoint.ets.domain.trigger.TriggerType;
import com.sailpoint.ets.infrastructure.web.dto.ResponseMode;
import com.sailpoint.metrics.MetricsUtil;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for reporting ETS specific metrics.
 */
public class MetricsReporter {
	private static final String INVOCATION_STARTED_TOTAL = MetricsReporter.class.getName() + ".invocation.started.total";
	private static final String INVOCATION_COMPLETE_TOTAL = MetricsReporter.class.getName() + ".invocation.complete.total";
	private static final String EVENT_HANDLER_FAILURE_TOTAL = MetricsReporter.class.getName() + ".event-handler.failure.total";
	private static final String INVOCATION_LATENCY = MetricsReporter.class.getName() + ".invocation.latency";
	private static final String TRIGGER_TYPE_TOTAL = MetricsReporter.class.getName() + ".trigger.type.total";
	private static final String RESPONSE_MODE_TOTAL = MetricsReporter.class.getName() + ".response.mode.total";
	private static final String EVENTS_TABLE_COUNT = MetricsReporter.class.getName() + ".events.table.count";
	private static final String CIRCUIT_BREAKER_STATE_CHANGE_COUNT = MetricsReporter.class.getName() +
		".circuit.breaker.state.change.count";
	private static final String TENANT_SUBSCRIPTION_COUNT = MetricsReporter.class.getName() + ".tenant-subscription.count";

	/**
	 * Increment trigger invocation started counter.
	 *
	 * @param event TriggerInvokedEvent
	 */
	public static void reportInvocationStarted(TriggerInvokedEvent event) {
		if (event == null || event.getTriggerId() == null || event.getType() == null) {
			return;
		}

		Map<String, String> tags = new HashMap<>();
		tags.put("triggerId", event.getTriggerId());
		tags.put("triggerType", CamelCaseUtil.toCamelCase(event.getType()));

		MetricsUtil.getCounter(INVOCATION_STARTED_TOTAL, tags).inc();
	}

	/**
	 * Increment trigger invocation complete counter, with success/error tag.
	 *
	 * @param error True if invocation completed with error, false otherwise
	 */
	public static void reportInvocationComplete(boolean error) {
		Map<String, String> tags = new HashMap<>();
		tags.put("status", error ? "error" : "success");
		MetricsUtil.getCounter(INVOCATION_COMPLETE_TOTAL, tags).inc();
	}

	/**
	 * Increment event handler failure counter, with exception name tag.
	 *
	 * @param e Exception which caused the failure
	 * @param triggerId TriggerId of invocation in event handler failure
	 */
	public static void reportEventHandlerFailure(Exception e, TriggerId triggerId) {
		Map<String, String> tags = new HashMap<>();
		tags.put("exceptionClassName", e.getClass().getCanonicalName());
		tags.put("triggerId", triggerId.toString());
		MetricsUtil.getCounter(EVENT_HANDLER_FAILURE_TOTAL, tags).inc();
	}

	/**
	 * Measure invocation process time from start to finish, with success/error tag.
	 *
	 * @param invocation Invocation
	 * @param error      True if invocation completed with error, false otherwise
	 */
	public static void reportTotalInvocationTime(Invocation invocation, boolean error) {
		if (invocation == null || invocation.getCreated() == null) {
			return;
		}

		Map<String, String> tags = new HashMap<>();
		tags.put("status", error ? "error" : "success");
		Duration consumerDuration = Duration.between(invocation.getCreated(), OffsetDateTime.now());
		MetricsUtil.getTimer(INVOCATION_LATENCY, tags).update(consumerDuration.toNanos(), TimeUnit.NANOSECONDS);
	}

	/**
	 * Increment counter of trigger invocation by trigger type.
	 *
	 * @param triggerType TriggerType
	 */
	public static void reportTriggerType(TriggerType triggerType) {
		if (triggerType == null) {
			return;
		}

		Map<String, String> tags = new HashMap<>();
		tags.put("triggerType", CamelCaseUtil.toCamelCase(triggerType));
		MetricsUtil.getCounter(TRIGGER_TYPE_TOTAL, tags).inc();
	}

	/**
	 * Increment counter of trigger invocation by response mode.
	 *
	 * @param responseMode {@link ResponseMode}
	 */
	public static void reportResponseMode(ResponseMode responseMode) {
		Map<String, String> tags = new HashMap<>();
		tags.put("responseMode", CamelCaseUtil.toCamelCase(responseMode));
		MetricsUtil.getCounter(RESPONSE_MODE_TOTAL, tags).inc();
	}

	/**
	 * Set current count of items in the events table.
	 *
	 * @param count Events table count
	 */
	public static void reportEventsTableCount(long count) {
		final String metricsName = MetricsUtil.getMetricsName(EVENTS_TABLE_COUNT, Collections.emptyMap());

		if (!MetricsUtil.getRegistry().getNames().contains(metricsName)) {
			MetricsUtil.getRegistry().register(metricsName, new NumberGauge(count));
		} else {
			NumberGauge gauge = (NumberGauge) MetricsUtil.getRegistry().getGauges().get(metricsName);
			gauge.setValue(count);
		}
	}

	/**
	 * Increment CircuitBreakerStateChange counter.
	 */
	public static void reportCircuitBreakerStateChangeCount(String triggerId, String state) {
		Map<String, String> tags = new HashMap<>();
		tags.put("triggerId", triggerId);
		tags.put("circuitBreakerState", state);
		MetricsUtil.getCounter(CIRCUIT_BREAKER_STATE_CHANGE_COUNT, tags).inc();
	}

	/**
	 * Gauge metric to report subscription count for each tenant
	 * @param subscriptionStatusList a list of tenant with their corresponding subscription count
	 */
	public static void reportTenantSubscriptions(List<SubscriptionStatus> subscriptionStatusList) {
		subscriptionStatusList.forEach(ss -> {
			TenantIdentifier tenant = TenantIdentifier.parse(ss.getTenant().toString());
			final String metricsName = MetricsUtil.getMetricsName(TENANT_SUBSCRIPTION_COUNT, ImmutableMap.of("org", tenant.getOrg(), "type", ss.getType().name()));
			if (!MetricsUtil.getRegistry().getNames().contains(metricsName)) {
				MetricsUtil.getRegistry().register(metricsName, new NumberGauge(ss.getCount()));
			} else {
				NumberGauge gauge = (NumberGauge) MetricsUtil.getRegistry().getGauges().get(metricsName);
				gauge.setValue(ss.getCount());
			}
		});
	}

	protected static final class NumberGauge implements Gauge<Number> {
		Number _number;

		public NumberGauge(Number value) {
			_number = value;
		}

		@Override
		public Number getValue() {
			return _number;
		}

		public void setValue(Number number) {
			_number = number;
		}
	}
}
