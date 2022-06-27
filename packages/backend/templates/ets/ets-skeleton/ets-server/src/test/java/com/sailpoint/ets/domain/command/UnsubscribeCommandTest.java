/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.command;

import com.sailpoint.ets.domain.subscription.Subscription;
import com.sailpoint.ets.domain.subscription.SubscriptionRepo;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.exception.NotFoundException;
import com.sailpoint.ets.infrastructure.aws.Invoker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.Optional;
import java.util.UUID;

import static com.sailpoint.ets.infrastructure.util.EventBridgeConfigConverter.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UnsubscribeCommand
 */
@RunWith(MockitoJUnitRunner.class)
public class UnsubscribeCommandTest {

	@Captor
	ArgumentCaptor<UUID> _subscriptionIdCaptor;

	@Captor
	ArgumentCaptor<String> _accountCaptor;

	@Captor
	ArgumentCaptor<String> _regionCaptor;

	@Captor
	ArgumentCaptor<String> _nameCaptor;

	@Mock
	SubscriptionRepo _subscriptionRepo;

	@Mock
	Subscription _subscription;

	@Mock
	Invoker _invoker;

	private UnsubscribeCommand _cmd;

	@Before
	public void setUp() {
		when(_subscription.getId()).thenReturn(UUID.fromString("0612a993-a2f8-4365-9dcc-4b5d620a64f0"));
	}

	@Test(expected=NotFoundException.class)
	public void testNotExistedSubscriptionId() {
		when(_subscriptionRepo.findById(any())).thenReturn(Optional.empty());

		givenCommand();
		whenTheCommandIsHandled();
		thenTheFunctionShouldNotBeInvoked();
	}

	@Test(expected=NotFoundException.class)
	public void testMismatchedTenantId() {
		when(_subscription.getTenantId()).thenReturn(new TenantId("acme-ocean"));
		when(_subscriptionRepo.findById(any())).thenReturn(Optional.of(_subscription));

		givenCommand();
		whenTheCommandIsHandled();
		thenTheFunctionShouldNotBeInvoked();
	}

	@Test
	public void testDeleteSubscription() {
		when(_subscription.getTenantId()).thenReturn(new TenantId("acme-solar"));
		when(_subscriptionRepo.findById(any())).thenReturn(Optional.of(_subscription));

		givenCommand();
		whenTheCommandIsHandled();
		thenTheSubscriptionIsRemoved();
	}

	@Test
	public void testDeleteEventBridgeSubscription() {
		when(_subscription.getTenantId()).thenReturn(new TenantId("acme-solar"));
		when(_subscription.getType()).thenReturn(SubscriptionType.EVENTBRIDGE);
		when(_subscription.getConfig()).thenReturn(ImmutableMap.of(AWS_ACCOUNT_NUMBER, "123456789012",
			AWS_REGION, "us-east-1", AWS_PARTNER_EVENT_SOURCE_NAME, "eventBridgeEventSource"));
		when(_subscriptionRepo.findById(any())).thenReturn(Optional.of(_subscription));

		givenCommand();
		whenTheCommandIsHandled();

		verify(_invoker).deletePartnerEventSource(_accountCaptor.capture(), _regionCaptor.capture(), _nameCaptor.capture());
		assertEquals("123456789012", _accountCaptor.getValue());
		assertEquals("us-east-1", _regionCaptor.getValue());
		assertEquals("eventBridgeEventSource", _nameCaptor.getValue());
	}

	private void givenCommand() {
		_cmd = UnsubscribeCommand.builder()
			.tenantId(new TenantId("acme-solar"))
			.subscriptionId(UUID.fromString("0612a993-a2f8-4365-9dcc-4b5d620a64f0"))
			.build();
	}

	private void whenTheCommandIsHandled() {
		_cmd.handle(_subscriptionRepo, _invoker);
		verify(_subscriptionRepo).deleteById(_subscriptionIdCaptor.capture());
	}

	private void thenTheFunctionShouldNotBeInvoked() {
		fail("The function should not be invoked.");
	}

	private void thenTheSubscriptionIsRemoved() {
		assertEquals("0612a993-a2f8-4365-9dcc-4b5d620a64f0", _subscriptionIdCaptor.getValue().toString());
	}

}
