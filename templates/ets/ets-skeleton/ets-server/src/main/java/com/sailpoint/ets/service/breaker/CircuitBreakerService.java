/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.service.breaker;

import com.sailpoint.atlas.boot.messaging.annotation.MessageHandler;
import com.sailpoint.atlas.boot.messaging.idn.IdnMessageScope;
import com.sailpoint.atlas.boot.messaging.service.MessageClientService;
import com.sailpoint.atlas.messaging.client.JobSubmission;
import com.sailpoint.atlas.messaging.client.Payload;
import com.sailpoint.atlas.messaging.client.SendMessageOptions;
import com.sailpoint.atlas.messaging.server.MessageHandlerContext;
import com.sailpoint.ets.infrastructure.util.MetricsReporter;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service for provide access and handle state of ETS CircuitBreakers.
 */
@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@CommonsLog
public class CircuitBreakerService {
	private final CircuitBreakerConfig _circuitBreakerConfig;

	private final MessageClientService _messageClientService;
	/**
	 * The circuitBreakers, indexed by name of the backend.
	 */
	private final ConcurrentMap<String, CircuitBreaker> _circuitBreakers = new ConcurrentHashMap<>();

	public CircuitBreaker getCircuitBreaker(String name) {
		return _circuitBreakers.computeIfAbsent(Objects.requireNonNull(name, "Name must not be null"), (k) -> {
			CircuitBreaker breaker = CircuitBreaker.of(name,
				_circuitBreakerConfig);
			breaker.getEventPublisher().onStateTransition(this::onStateTransition);
			return breaker;
		});
	}

	/**
	 * Handle State Transition for CircuitBreaker. Publish state change message across the org.
	 * @param event state transition event.
	 */
	private void onStateTransition(CircuitBreakerOnStateTransitionEvent event) {
		log.warn("Publish CircuitBreaker state transition event " + event.toString());
		MetricsReporter.reportCircuitBreakerStateChangeCount(event.getCircuitBreakerName(),
			event.getStateTransition().getToState().toString());
		JobSubmission jobSubmission = new JobSubmission(new Payload("CircuitBreakerOnStateTransitionEvent", event));
		_messageClientService.submitJob(IdnMessageScope.ETS, jobSubmission, new  SendMessageOptions());
	}

	@MessageHandler(scope= IdnMessageScope.ETS, payloadType = "CircuitBreakerOnStateTransitionEvent")
	public void contextMethod(MessageHandlerContext context) {
		CircuitBreakerOnStateTransitionEvent event = context.getMessageContent(CircuitBreakerOnStateTransitionEvent.class);
		if(event == null) {
			return;
		}
		CircuitBreaker breaker = getCircuitBreaker(event.getCircuitBreakerName());
		log.info("Handle CircuitBreaker state transition event for breaker " + event.getCircuitBreakerName());
		if(breaker.getState() != event.getStateTransition().getToState()) {
			log.info("Update state for CircuitBreaker " + event.getCircuitBreakerName() + " transition from " +
				breaker.getState() + " to state " + event.getStateTransition().getToState());
			switch (event.getStateTransition().getToState()) {
				case DISABLED:
					breaker.transitionToDisabledState();
					break;
				case CLOSED:
					breaker.transitionToClosedState();
					break;
				case OPEN:
					breaker.transitionToOpenState();
					break;
				case FORCED_OPEN:
					breaker.transitionToForcedOpenState();
					break;
				case HALF_OPEN:
					breaker.transitionToHalfOpenState();
					break;
			}
		}
	}
}
