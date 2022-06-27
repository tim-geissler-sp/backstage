/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.domain.command;

import com.sailpoint.atlas.boot.messaging.service.MessageClientService;
import com.sailpoint.ets.domain.Secret;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.event.DomainEvent;
import com.sailpoint.ets.domain.event.EventPublisher;
import com.sailpoint.ets.domain.event.InvocationCompletedEvent;
import com.sailpoint.ets.domain.event.InvocationFailedEvent;
import com.sailpoint.ets.domain.invocation.Invocation;
import com.sailpoint.ets.domain.invocation.InvocationRepo;
import com.sailpoint.ets.domain.subscription.SubscriptionRepo;
import com.sailpoint.ets.domain.trigger.Trigger;
import com.sailpoint.ets.domain.trigger.TriggerId;
import com.sailpoint.ets.domain.trigger.TriggerRepo;
import com.sailpoint.ets.exception.NotFoundException;
import com.sailpoint.ets.exception.ValidationException;
import com.sailpoint.ets.infrastructure.util.HashService;
import com.sailpoint.ets.service.breaker.CircuitBreakerService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CompleteInvocationCommand
 */
@RunWith(MockitoJUnitRunner.class)
public class CompleteInvocationCommandTest {

	@Captor
	ArgumentCaptor<Invocation> _invocationCaptor;

	@Captor
	ArgumentCaptor<DomainEvent> _domainEventCaptor;

	@Mock
	TriggerRepo _triggerRepo;

	@Mock
	SubscriptionRepo _subscriptionRepo;

	@Mock
	InvocationRepo _invocationRepo;

	@Mock
	EventPublisher _eventPublisher;

	@Mock
	HashService _hashService;

	@Mock
	Trigger _trigger;

	@Mock
	MessageClientService _messageClientService;

	Invocation _invocation;

	private CompleteInvocationCommand _cmd;
	private CircuitBreakerService _circuitBreakerService;
	private CircuitBreakerConfig _circuitBreakerConfig;

	@Before
	public void setUp() {
		_invocation = Invocation.builder()
			.id(UUID.randomUUID())
			.tenantId(new TenantId("dev#acme-solar"))
			.triggerId(new TriggerId("idn:access"))
			.secret(new Secret("4321"))
			.created(OffsetDateTime.now())
			.build();

		when(_hashService.matches("4321", "1234"))
			.thenReturn(true);

		_circuitBreakerConfig = CircuitBreakerConfig.ofDefaults();
		_circuitBreakerService = new CircuitBreakerService(_circuitBreakerConfig, _messageClientService);
	}

	@Test(expected=NotFoundException.class)
	public void testNotExistedInvocationId() {
		when(_invocationRepo.findById(any())).thenReturn(Optional.empty());

		givenCommand(null, null);
		_cmd.handle(_triggerRepo, _subscriptionRepo, _invocationRepo, _eventPublisher, _hashService, _circuitBreakerService);
		verify(_invocationRepo, times(0)).delete(_invocationCaptor.capture());
		verify(_eventPublisher, times(0)).publish(_domainEventCaptor.capture());
	}

	@Test(expected=NotFoundException.class)
	public void testNotExistedTriggerId() {
		when(_invocationRepo.findById(any())).thenReturn(Optional.of(_invocation));
		when(_triggerRepo.findById(any())).thenReturn(Optional.empty());

		givenCommand(null, Collections.emptyMap());
		whenTheCommandIsHandled();
		thenTheFunctionShouldNotBeInvoked();
	}

	@Test
	public void testInvocationFailedWithErrorWithNoOutput() {
		when(_invocationRepo.findById(any())).thenReturn(Optional.of(_invocation));

		givenCommand(null, null);
		whenTheCommandIsHandled();
		thenInvocationFailEventIsPublished("no output was provided");
	}

	@Test
	public void testInvocationFailedWithValidationError() {
		when(_invocationRepo.findById(any())).thenReturn(Optional.of(_invocation));
		doThrow(new ValidationException("", "")).when(_trigger).validateOutput(any());
		when(_triggerRepo.findById(any())).thenReturn(Optional.of(_trigger));

		givenCommand(null, Collections.emptyMap());
		whenTheCommandIsHandled();
		thenInvocationFailEventIsPublished("output was not in the correct format: null");
	}

	@Test
	public void testInvocationCompleted() {
		when(_invocationRepo.findById(any())).thenReturn(Optional.of(_invocation));
		doNothing().when(_trigger).validateOutput(any());
		when(_triggerRepo.findById(any())).thenReturn(Optional.of(_trigger));

		givenCommand(null, Collections.emptyMap());
		whenTheCommandIsHandled();
		thenInvocationCompletedEventIsPublished();
	}

	private void givenCommand(String error, Map<String, Object> output) {
		_cmd = CompleteInvocationCommand.builder()
			.invocationId(UUID.fromString("0612a993-a2f8-4365-9dcc-4b5d620a64f0"))
			.output(output)
			.error(error)
			.secret(new Secret("1234"))
			.tenantId(new TenantId("dev#acme-solar"))
			.requestId(UUID.randomUUID().toString())
			.build();
	}

	private void whenTheCommandIsHandled() {
		_cmd.handle(_triggerRepo, _subscriptionRepo, _invocationRepo, _eventPublisher, _hashService, _circuitBreakerService);
		verify(_invocationRepo).delete(_invocationCaptor.capture());
		verify(_eventPublisher).publish(_domainEventCaptor.capture());
	}

	private void thenTheFunctionShouldNotBeInvoked() {
		fail("The function should not be invoked.");
	}

	private void thenInvocationFailEventIsPublished(String expectedErrorMessage) {
		assertTrue(_domainEventCaptor.getValue() instanceof InvocationFailedEvent);
		assertEquals(expectedErrorMessage, ((InvocationFailedEvent)_domainEventCaptor.getValue()).getReason());
		assertEquals(_invocation, _invocationCaptor.getValue());
	}

	private void thenInvocationCompletedEventIsPublished() {
		assertTrue(_domainEventCaptor.getValue() instanceof InvocationCompletedEvent);
		assertEquals(0, ((InvocationCompletedEvent)_domainEventCaptor.getValue()).getOutput().size());
		assertEquals(_invocation, _invocationCaptor.getValue());
	}

}
