/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.domain.command;

import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.event.EventPublisher;
import com.sailpoint.ets.domain.event.InvocationFailedEvent;
import com.sailpoint.ets.domain.invocation.Invocation;
import com.sailpoint.ets.domain.invocation.InvocationRepo;
import com.sailpoint.ets.domain.trigger.TriggerId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CompleteExpiredInvocationsCommand
 */
@RunWith(MockitoJUnitRunner.class)
public class CompleteExpiredInvocationsCommandTest {

	@Captor
	ArgumentCaptor<Invocation> _invocationCaptor;

	@Captor
	ArgumentCaptor<InvocationFailedEvent> _invocationFailedEventCaptor;

	@Mock
	InvocationRepo _invocationRepo;

	@Mock
	EventPublisher _eventPublisher;

	Invocation _invocation;

	@Before
	public void setupUp() {
		_invocation = Invocation.builder()
			.id(UUID.randomUUID())
			.tenantId(new TenantId("dev#acme-solar"))
		    .triggerId(new TriggerId("idn:test-trigger"))
			.build();
	}

	private CompleteExpiredInvocationsCommand _cmd;

	@Test
	public void testNoExpiredInvocation() {
		when(_invocationRepo.findByDeadlineBefore(any(), any())).thenReturn(Collections.emptyList());

		givenCommand();
		int completedInvocations = whenTheCommandIsHandled(false);
		thenTheNumberOfCompletedInvocation(0, completedInvocations);
	}

	@Test
	public void testMakeSubscription() {

		when(_invocationRepo.findByDeadlineBefore(any(), any())).thenReturn(Collections.singletonList(_invocation));

		givenCommand();
		int completedInvocations = whenTheCommandIsHandled(true);
		thenInvocationIsCaptured();
		thenTheNumberOfCompletedInvocation(1, completedInvocations);
	}

	private void givenCommand() {
		_cmd = CompleteExpiredInvocationsCommand.builder()
			.maxInvocations(10)
			.requestId(UUID.randomUUID().toString())
			.build();
	}

	private int whenTheCommandIsHandled(boolean shouldVerifyFunctionInvocation) {
		int completedInvocations = _cmd.handle(_invocationRepo, _eventPublisher);
		if (shouldVerifyFunctionInvocation) {
			verify(_invocationRepo).delete(_invocationCaptor.capture());
			verify(_eventPublisher).publish(_invocationFailedEventCaptor.capture());
		}

		return completedInvocations;
	}

	private void thenTheNumberOfCompletedInvocation(int expected, int actual) {
		assertEquals(expected, actual);
	}

	private void thenInvocationIsCaptured() {
		assertEquals(1, _invocationCaptor.getAllValues().size());
		assertEquals(1, _invocationFailedEventCaptor.getAllValues().size());
	}
}
