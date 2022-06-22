/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.command;

import com.sailpoint.atlas.boot.api.common.UnauthorizedAccessException;
import com.sailpoint.ets.domain.Secret;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.event.EventPublisher;
import com.sailpoint.ets.domain.event.InvocationCompletedEvent;
import com.sailpoint.ets.domain.event.InvocationFailedEvent;
import com.sailpoint.ets.domain.invocation.Invocation;
import com.sailpoint.ets.domain.invocation.InvocationRepo;
import com.sailpoint.ets.domain.subscription.Subscription;
import com.sailpoint.ets.domain.subscription.SubscriptionRepo;
import com.sailpoint.ets.domain.trigger.Trigger;
import com.sailpoint.ets.domain.trigger.TriggerRepo;
import com.sailpoint.ets.exception.NotFoundException;
import com.sailpoint.ets.infrastructure.util.HashService;
import com.sailpoint.ets.infrastructure.util.InlineConfigConverter;
import com.sailpoint.ets.service.breaker.CircuitBreakerService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.sailpoint.ets.infrastructure.util.MetricsReporter.reportInvocationComplete;
import static com.sailpoint.ets.infrastructure.util.MetricsReporter.reportTotalInvocationTime;
import static com.sailpoint.ets.infrastructure.util.TriggerEventLogUtil.logInvocationCompletedEvent;
import static com.sailpoint.ets.infrastructure.util.TriggerEventLogUtil.logInvocationFailedEvent;

/**
 * CompleteInvocationCommand
 */
@Value
@Builder
@CommonsLog
public class CompleteInvocationCommand {

	@NonNull private final TenantId _tenantId;
	@NonNull private final UUID _invocationId;
	private final UUID _subscriptionId;
	private final Map<String, Object> _output;
	private final String _error;
	@NonNull private final Secret _secret;
	@NonNull private final String _requestId;

	public void handle(TriggerRepo triggerRepo, SubscriptionRepo subscriptionRepo, InvocationRepo invocationRepo, EventPublisher eventPublisher,
					   HashService hashService, CircuitBreakerService circuitBreakerService) {

		Invocation invocation = invocationRepo.findById(_invocationId)
			.orElse(null);

		if(invocation == null || !invocation.getTenantId().equals(_tenantId)) {
			throw new NotFoundException("invocation", _invocationId.toString());
		}

		if(!hashService.matches(invocation.getSecret().toString(), _secret.toString())) {
			throw new UnauthorizedAccessException("secret for invocation with id: " + _invocationId + " doesn't match");
		}

		String error;
		Map<String, Object> output;

		if (_subscriptionId == null) {
			error = _error;
			output = _output;
		} else {
			Subscription subscription = subscriptionRepo.findById(_subscriptionId)
				.orElseThrow(() -> new NotFoundException("subscription", _subscriptionId.toString()));
			error = (String)subscription.getConfig().get(InlineConfigConverter.ERROR);
			output = (Map<String, Object>)subscription.getConfig().get(InlineConfigConverter.OUTPUT);
		}


		if (StringUtils.isEmpty(error)) {
			if (output == null) {
				error = "no output was provided";
			} else {
				Trigger trigger = triggerRepo.findById(invocation.getTriggerId())
						.orElseThrow(() -> new NotFoundException("trigger", invocation.getTriggerId().toString()));

				try {
					trigger.validateOutput(output);
				} catch (Exception ex) {
					error = "output was not in the correct format: " + ex.getMessage();
				}
			}
		}
		CircuitBreaker circuitBreaker = circuitBreakerService.getCircuitBreaker(_tenantId.toString() + "_" + invocation.getTriggerId());
		Duration duration = Duration.between(invocation.getCreated(), OffsetDateTime.now());
		if (StringUtils.isEmpty(error)) {
			circuitBreaker.onSuccess(duration.toNanos(), TimeUnit.NANOSECONDS);
			InvocationCompletedEvent invocationCompletedEvent = invocation.newCompletedEvent(output, _requestId);
			eventPublisher.publish(invocationCompletedEvent);
			reportTotalInvocationTime(invocation, false);
			reportInvocationComplete(false);

			log.info(logInvocationCompletedEvent("Trigger invocation completion succeeded.", invocationCompletedEvent));
		} else {
			circuitBreaker.onError(duration.toNanos(),TimeUnit.NANOSECONDS, new Exception((error)));
			InvocationFailedEvent invocationFailedEvent = invocation.newFailedEvent(error);
			eventPublisher.publish(invocationFailedEvent);
			reportTotalInvocationTime(invocation, true);
			reportInvocationComplete(true);

			log.error(logInvocationFailedEvent(invocationFailedEvent));
		}

		invocationRepo.delete(invocation);
	}
}
