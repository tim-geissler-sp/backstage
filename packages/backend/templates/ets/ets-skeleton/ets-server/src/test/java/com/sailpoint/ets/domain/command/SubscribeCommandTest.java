/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.domain.command;

import com.sailpoint.ets.EtsProperties;
import com.sailpoint.ets.domain.*;
import com.sailpoint.ets.domain.subscription.Subscription;
import com.sailpoint.ets.domain.subscription.SubscriptionRepo;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.domain.trigger.Trigger;
import com.sailpoint.ets.domain.trigger.EtsFeatureStore;
import com.sailpoint.ets.domain.trigger.TriggerId;
import com.sailpoint.ets.domain.trigger.TriggerRepo;
import com.sailpoint.ets.domain.trigger.TriggerType;
import com.sailpoint.ets.exception.DuplicatedSubscriptionException;
import com.sailpoint.ets.exception.NotFoundException;
import com.sailpoint.ets.exception.LimitExceededException;
import com.sailpoint.ets.exception.ValidationException;
import com.sailpoint.ets.infrastructure.aws.Invoker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SubscribeCommand
 */
@RunWith(MockitoJUnitRunner.class)
public class SubscribeCommandTest {

	@Captor
	ArgumentCaptor<Subscription> _subscriptionCaptor;

	@Mock
	TriggerRepo _triggerRepo;

	@Mock
	SubscriptionRepo _subscriptionRepo;

	@Mock
	Trigger _trigger;

	@Mock
	Subscription _subscription;

	@Mock
	EtsFeatureStore _etsFeatureStore;

	@Mock
	Invoker _invoker;

	private EtsProperties _properties;
	private SubscribeCommand _cmd;

	@Before
	public void setUp() {
		_properties = new EtsProperties();
		_properties.setSubscriptionLimit(3);
	}

	@Test(expected=NotFoundException.class)
	public void testNotExistedTriggerId() {
		when(_triggerRepo.findById(any())).thenReturn(Optional.empty());

		givenCommand();
		whenTheCommandIsHandled();
		thenTheFunctionShouldNotBeInvoked();
	}

	@Test(expected=NotFoundException.class)
	public void testDisabledTriggerId() {
		when(_trigger.isEnabledForTenant(any())).thenReturn(false);
		when(_triggerRepo.findById(any())).thenReturn(Optional.of(_trigger));

		givenCommand();
		whenTheCommandIsHandled();
		thenTheFunctionShouldNotBeInvoked();
	}

	@Test(expected=DuplicatedSubscriptionException.class)
	public void testDuplicatedSubscription() {
		when(_trigger.isEnabledForTenant(any())).thenReturn(true);
		when(_trigger.getType()).thenReturn(TriggerType.REQUEST_RESPONSE);
		when(_triggerRepo.findById(any())).thenReturn(Optional.of(_trigger));
		when(_subscriptionRepo.findByTenantIdAndTriggerId(any(), any())).thenReturn(Optional.of(_subscription));

		givenCommand();
		whenTheCommandIsHandled();
		thenTheFunctionShouldNotBeInvoked();
	}

	@Test
	public void testSubscriptionLimitNotExceeded() {
		when(_trigger.isEnabledForTenant(any())).thenReturn(true);
		when(_trigger.getType()).thenReturn(TriggerType.FIRE_AND_FORGET);
		when(_triggerRepo.findById(any())).thenReturn(Optional.of(_trigger));
		when(_subscriptionRepo.findAllByTenantIdAndTriggerId(any(), any())).thenReturn((Stream.of(_subscription, _subscription)));

		givenCommand();
		whenTheCommandIsHandled();
		thenTheSubscriptionIsMade();
	}

	@Test(expected= LimitExceededException.class)
	public void testSubscriptionLimitExceeded() {
		when(_trigger.isEnabledForTenant(any())).thenReturn(true);
		when(_trigger.getType()).thenReturn(TriggerType.FIRE_AND_FORGET);
		when(_triggerRepo.findById(any())).thenReturn(Optional.of(_trigger));
		when(_subscriptionRepo.findAllByTenantIdAndTriggerId(any(), any())).thenReturn((Stream.of(_subscription, _subscription, _subscription)));

		givenCommand();
		whenTheCommandIsHandled();
		thenTheFunctionShouldNotBeInvoked();
	}

	@Test
	public void testMakeSubscription() {
		when(_trigger.isEnabledForTenant(any())).thenReturn(true);
		when(_triggerRepo.findById(any())).thenReturn(Optional.of(_trigger));
		when(_subscriptionRepo.findByTenantIdAndTriggerId(any(), any())).thenReturn(Optional.empty());

		givenCommand();
		whenTheCommandIsHandled();
		thenTheSubscriptionIsMade();
	}

	private void givenCommand() {
		_cmd = SubscribeCommand.builder()
			.tenantId(new TenantId("tenantId"))
			.triggerId(new TriggerId("triggerId"))
			.type(SubscriptionType.HTTP)
			.config(Collections.singletonMap("url", "not empty"))
			.build();
	}

	private void whenTheCommandIsHandled() {
		_cmd.handle(_triggerRepo, _subscriptionRepo, _invoker, _etsFeatureStore, _properties);
		verify(_subscriptionRepo).save(_subscriptionCaptor.capture());
	}

	private void thenTheFunctionShouldNotBeInvoked() {
		fail("The function should not be invoked.");
	}

	private void thenTheSubscriptionIsMade() {
		assertEquals("tenantId", _subscriptionCaptor.getValue().getTenantId().toString());
		assertEquals("triggerId", _subscriptionCaptor.getValue().getTriggerId().toString());
		assertEquals(SubscriptionType.HTTP, _subscriptionCaptor.getValue().getType());
		assertEquals(1, _subscriptionCaptor.getValue().getConfig().size());
	}

}
