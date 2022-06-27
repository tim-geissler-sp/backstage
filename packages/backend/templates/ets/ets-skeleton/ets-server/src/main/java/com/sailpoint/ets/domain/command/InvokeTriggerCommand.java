/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.command;

import com.sailpoint.atlas.util.JsonPathUtil;
import com.sailpoint.ets.EtsProperties;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.event.EventPublisher;
import com.sailpoint.ets.domain.event.TriggerInvokedEvent;
import com.sailpoint.ets.domain.invocation.Invocation;
import com.sailpoint.ets.domain.invocation.InvocationRepo;
import com.sailpoint.ets.domain.status.InvocationType;
import com.sailpoint.ets.domain.subscription.Subscription;
import com.sailpoint.ets.domain.subscription.SubscriptionRepo;
import com.sailpoint.ets.domain.trigger.Trigger;
import com.sailpoint.ets.domain.trigger.EtsFeatureStore;
import com.sailpoint.ets.domain.trigger.TriggerId;
import com.sailpoint.ets.domain.trigger.TriggerRepo;
import com.sailpoint.ets.exception.NotFoundException;
import com.sailpoint.utilities.JsonUtil;
import com.sailpoint.utilities.StringUtil;
import lombok.Builder;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.sailpoint.ets.domain.trigger.TriggerType.REQUEST_RESPONSE;
import static com.sailpoint.ets.infrastructure.util.MetricsReporter.reportInvocationStarted;
import static com.sailpoint.ets.infrastructure.util.TriggerEventLogUtil.logTriggerInvokedEvent;

/*
 * Class InvokeTriggerCommand.
 */
@Value
@Builder
@CommonsLog
public class InvokeTriggerCommand {

	private final TenantId _tenantId;
	private final String _requestId;
	private final TriggerId _triggerId;
	private final Map<String, Object> _input;
	private final Map<String, Object> _context;
	private final Map<String, String> _headers;

	/*
	 * Handle Invoke Trigger.
	 */
	public List<Invocation> handle(TriggerRepo triggerRepo, SubscriptionRepo subscriptionRepo,
								   InvocationRepo invocationRepo, EventPublisher eventPublisher,
								   EtsProperties properties, EtsFeatureStore etsFeatureStore) {

		Trigger trigger = triggerRepo.findById(_triggerId)
				.orElseThrow(() -> new NotFoundException("trigger", _triggerId.toString()));

		// Return empty invocation if subscription is made but the trigger is disabled later
		if (!trigger.isEnabledForTenant(etsFeatureStore)) {
			log.warn("Invocation skipped because trigger '" + _triggerId.toString() + "' is disabled");
			return Collections.emptyList();
		}

		return subscriptionRepo.findAllByTenantIdAndTriggerId(_tenantId, _triggerId)
			.map(
				subscription -> {
					if (!subscription.isEnabled()) {
						log.warn("Invocation skipped because subscription '" + subscription.getId() + "' is disabled");
						return null;
					}

					try {
						if (StringUtil.isNotNullOrEmpty(subscription.getFilter()) && !JsonPathUtil.isPathExist(JsonUtil.toJson(_input), subscription.getFilter())) {
							log.info("Invocation skipped because subscription '" + subscription.getId() + "' has filter " + subscription.getFilter());
							return null;
						}
					} catch (Exception e) {
						log.warn(String.format("Filtering failed for input: %s with filter: %s", JsonUtil.toJson(_input), subscription.getFilter()));
						return null;
					}

					Invocation invocation = invokeTrigger(trigger, properties, subscription, invocationRepo);

					TriggerInvokedEvent event = TriggerInvokedEvent.builder()
						.tenantId(_tenantId.toString())
						.triggerId(_triggerId.toString())
						.requestId(_requestId)
						.secret(invocation.getSecret())
						.invocationId(invocation.getId().toString())
						.invocationType(InvocationType.REAL_TIME)
						.type(trigger.getType())
						.subscriptionId(subscription.getId().toString())
						.subscriptionName(subscription.getName())
						.subscriptionType(subscription.getType())
						.subscriptionConfig(subscription.getConfig())
						.scriptSource(subscription.getScriptSource())
						.input(_input)
						.context(_context)
						.headers(_headers)
						.build();

					eventPublisher.publish(event);

					reportInvocationStarted(event);
					log.info(logTriggerInvokedEvent("Trigger invocation started successfully.", event));

					return invocation;
				}
			)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	private Invocation invokeTrigger(Trigger trigger, EtsProperties properties,
									 Subscription subscription, InvocationRepo invocationRepo) {
		Invocation invocation;
		if (trigger.getType() == REQUEST_RESPONSE) {
			invocation = trigger.invoke(subscription, _input, _context,
				properties.getDeadlineMinutes(), InvocationType.REAL_TIME);
			invocationRepo.save(invocation);
		} else {
			invocation = trigger.invoke(subscription, _input, _context,
				Integer.MIN_VALUE, InvocationType.REAL_TIME);
		}
		return invocation;
	}
}
