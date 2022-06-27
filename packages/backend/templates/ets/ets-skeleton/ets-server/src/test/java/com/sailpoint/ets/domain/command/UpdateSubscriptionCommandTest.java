/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.command;

import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.subscription.Subscription;
import com.sailpoint.ets.domain.subscription.SubscriptionRepo;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.domain.trigger.Trigger;
import com.sailpoint.ets.domain.trigger.EtsFeatureStore;
import com.sailpoint.ets.domain.trigger.TriggerId;
import com.sailpoint.ets.domain.trigger.TriggerRepo;
import com.sailpoint.ets.exception.IllegalUpdateException;
import com.sailpoint.ets.exception.NotFoundException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;
import java.util.UUID;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UpdateSubscriptionCommand
 */
@RunWith(MockitoJUnitRunner.class)
public class UpdateSubscriptionCommandTest {

	@Captor
	ArgumentCaptor<Subscription> _subscriptionCaptor;

	@Mock
	TriggerRepo _triggerRepo;

	@Mock
	SubscriptionRepo _subscriptionRepo;

	@Mock
	EtsFeatureStore _etsFeatureStore;

	@Mock
	Trigger _trigger;

	private UpdateSubscriptionCommand _cmd;

	@Test(expected= NotFoundException.class)
	public void testNotExistedSubscriptionId() {
		when(_subscriptionRepo.findById(any())).thenReturn(Optional.empty());

		givenCommand();
		whenTheCommandIsHandled();
		thenTheFunctionShouldNotBeInvoked();
	}

	@Test(expected= NotFoundException.class)
	public void testUpdatingSubscriptionFromDifferentTenant() {
		when(_subscriptionRepo.findById(any())).thenReturn(Optional.of(Subscription.builder()
			.tenantId(new TenantId("different")).build()));

		givenCommand();
		whenTheCommandIsHandled();
		thenTheFunctionShouldNotBeInvoked();
	}

	@Test(expected= NotFoundException.class)
	public void testUpdatingTriggerId() {
		when(_subscriptionRepo.findById(any())).thenReturn(Optional.of(Subscription.builder()
			.tenantId(new TenantId("acme-solar")).triggerId(new TriggerId("Updated")).build()));

		givenCommand();
		whenTheCommandIsHandled();
		thenTheFunctionShouldNotBeInvoked();
	}

	@Test(expected= IllegalUpdateException.class)
	public void testUpdatingFromEventBridgeType() {
		when(_subscriptionRepo.findById(any())).thenReturn(Optional.of(Subscription.builder().type(SubscriptionType.EVENTBRIDGE)
			.tenantId(new TenantId("acme-solar")).triggerId(new TriggerId("TriggerId")).build()));
		when(_triggerRepo.findById(any())).thenReturn(Optional.of(_trigger));
		when(_trigger.isEnabledForTenant(any())).thenReturn(true);

		givenCommand();
		whenTheCommandIsHandled();
		thenTheFunctionShouldNotBeInvoked();
	}

	@Test
	public void testEnablingSubscription() {
		when(_triggerRepo.findById(any())).thenReturn(Optional.of(_trigger));
		when(_trigger.isEnabledForTenant(any())).thenReturn(true);
		when(_subscriptionRepo.findById(any())).thenReturn(Optional.of(Subscription.builder()
			.tenantId(new TenantId("acme-solar")).triggerId(new TriggerId("TriggerId")).enabled(true).build()));

		givenCommand();
		whenTheCommandIsHandled();

		assertFalse(_subscriptionCaptor.getValue().isEnabled());
	}

	private void givenCommand() {
		_cmd = UpdateSubscriptionCommand.builder()
			.id(UUID.fromString("0612a993-a2f8-4365-9dcc-4b5d620a64f0"))
			.type(SubscriptionType.HTTP)
			.tenantId(new TenantId("acme-solar"))
			.triggerId(new TriggerId("TriggerId"))
			.enabled(false)
			.build();
	}

	private void whenTheCommandIsHandled() {
		_cmd.handle(_triggerRepo, _subscriptionRepo, _etsFeatureStore);
		verify(_subscriptionRepo).save(_subscriptionCaptor.capture());
	}

	private void thenTheFunctionShouldNotBeInvoked() {
		fail("The function should not be invoked.");
	}
}
