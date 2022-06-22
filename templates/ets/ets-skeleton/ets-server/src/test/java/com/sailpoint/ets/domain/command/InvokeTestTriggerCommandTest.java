/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.domain.command;

import com.sailpoint.ets.EtsProperties;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.event.EventPublisher;
import com.sailpoint.ets.domain.event.TriggerInvokedEvent;
import com.sailpoint.ets.domain.invocation.Invocation;
import com.sailpoint.ets.domain.invocation.InvocationRepo;
import com.sailpoint.ets.domain.subscription.Subscription;
import com.sailpoint.ets.domain.subscription.SubscriptionRepo;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.domain.trigger.Trigger;
import com.sailpoint.ets.domain.trigger.EtsFeatureStore;
import com.sailpoint.ets.domain.trigger.TriggerId;
import com.sailpoint.ets.domain.trigger.TriggerRepo;
import com.sailpoint.ets.domain.trigger.TriggerType;
import com.sailpoint.ets.exception.NotFoundException;
import com.sailpoint.utilities.JsonUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for InvokeTriggerCommand
 */
@RunWith(MockitoJUnitRunner.class)
public class InvokeTestTriggerCommandTest {

	@Captor
	ArgumentCaptor<Invocation> _invocationCaptor;

	@Captor
	ArgumentCaptor<TriggerInvokedEvent> _triggerInvokedEventCaptor;

	@Mock
	TriggerRepo _triggerRepo;

	@Mock
	SubscriptionRepo _subscriptionRepo;

	@Mock
	InvocationRepo _invocationRepo;

	@Mock
	EventPublisher _eventPublisher;

	@Mock
	Trigger _trigger;

	@Mock
	Invocation _invocation;

	@Mock
	EtsFeatureStore _etsFeatureStore;

	private EtsProperties _properties;
	private InvokeTestTriggerCommand _cmd;

	@Before
	public void setUp() {
		_properties = new EtsProperties();
		_properties.setDeadlineMinutes(1);
	}

	@Test(expected=NotFoundException.class)
	public void testNotExistedTriggerId() {
		when(_triggerRepo.findById(any())).thenReturn(Optional.empty());

		givenCommand();
		whenTheCommandIsHandled();
		thenTheFunctionShouldNotBeInvoked();
	}

	@Test
	public void testDisabledTriggerId() {
		when(_trigger.isEnabledForTenant(any())).thenReturn(false);
		when(_triggerRepo.findById(any())).thenReturn(Optional.of(_trigger));

		givenCommand();
		List<Invocation> invocations = _cmd.handle(_triggerRepo, _subscriptionRepo, _invocationRepo,
			_eventPublisher, _properties, _etsFeatureStore);
		assertTrue(invocations.isEmpty());
	}

	@Test
	public void testInvokingRequestResponseTrigger() {
		when(_trigger.isEnabledForTenant(any())).thenReturn(true);
		when(_invocation.getId()).thenReturn(UUID.randomUUID());
		when(_trigger.invoke(any(), any(), any(), anyInt(), any())).thenReturn(_invocation);
		when(_trigger.getType()).thenReturn(TriggerType.REQUEST_RESPONSE);
		when(_triggerRepo.findById(any())).thenReturn(Optional.of(_trigger));
		when(_subscriptionRepo.findAllByTenantIdAndTriggerId(any(), any()))
			.thenReturn(Stream.of(Subscription.builder()
				.id(UUID.randomUUID())
				.type(SubscriptionType.HTTP)
				.build()));

		givenCommand();
		whenTheCommandIsHandled();
		thenTheInvocationIsComplete();
	}

	@Test
	public void testInvokingTriggerWithOptionalInput() {
		when(_trigger.isEnabledForTenant(any())).thenReturn(true);
		when(_invocation.getId()).thenReturn(UUID.randomUUID());
		when(_trigger.invoke(any(), any(), any(), anyInt(), any())).thenReturn(_invocation);
		when(_trigger.getType()).thenReturn(TriggerType.REQUEST_RESPONSE);
		when(_triggerRepo.findById(any())).thenReturn(Optional.of(_trigger));
		when(_subscriptionRepo.findAllByTenantIdAndTriggerId(any(), any()))
			.thenReturn(Stream.of(Subscription.builder()
				.id(UUID.randomUUID())
				.type(SubscriptionType.HTTP)
				.build()));

		// Pass in an input example that should validate correctly
		Map<String, Object> inputMap = new HashMap<>();
		inputMap.put("approved", true);
		inputMap.put("identityId", "201327fda1c44704ac01181e963d463e");
		givenCommand(inputMap);
		whenTheCommandIsHandled();
		thenTheInvocationIsComplete();

		//Verify that test input was used and not example input
		verify(_eventPublisher, times(1)).publish(_triggerInvokedEventCaptor.capture());
		assertEquals("201327fda1c44704ac01181e963d463e", _triggerInvokedEventCaptor.getValue().getInput().get("identityId"));
	}

	@Test
	public void testInvokingFireAndForgetTrigger() {
		when(_trigger.isEnabledForTenant(any())).thenReturn(true);
		when(_invocation.getId()).thenReturn(UUID.randomUUID());
		when(_trigger.invoke(any(), any(), any(), anyInt(), any())).thenReturn(_invocation);
		when(_trigger.getType()).thenReturn(TriggerType.FIRE_AND_FORGET);
		when(_triggerRepo.findById(any())).thenReturn(Optional.of(_trigger));
		Subscription subscription1 = Subscription.builder()
			.id(UUID.randomUUID())
			.type(SubscriptionType.HTTP).build();
		Subscription subscription2 = Subscription.builder()
			.id(UUID.randomUUID())
			.type(SubscriptionType.HTTP).build();
		when(_subscriptionRepo.findAllByTenantIdAndTriggerId(any(), any()))
			.thenReturn(Stream.of(subscription1, subscription2));

		givenCommand();

		_cmd.handle(_triggerRepo, _subscriptionRepo, _invocationRepo, _eventPublisher, _properties, _etsFeatureStore);
		verify(_eventPublisher, times(2)).publish(_triggerInvokedEventCaptor.capture());
		assertEquals("tenantId", _triggerInvokedEventCaptor.getValue().getTenantId());
		assertEquals("triggerId", _triggerInvokedEventCaptor.getValue().getTriggerId());
		assertEquals("FIRE_AND_FORGET", _triggerInvokedEventCaptor.getValue().getType().toString());
		assertEquals(SubscriptionType.HTTP, _triggerInvokedEventCaptor.getValue().getSubscriptionType());
	}

	@Test
	public void testInvokingSingleSubscription() {
		when(_trigger.isEnabledForTenant(any())).thenReturn(true);
		when(_invocation.getId()).thenReturn(UUID.randomUUID());
		when(_trigger.invoke(any(), any(), any(), anyInt(), any())).thenReturn(_invocation);
		when(_trigger.getType()).thenReturn(TriggerType.FIRE_AND_FORGET);
		when(_triggerRepo.findById(any())).thenReturn(Optional.of(_trigger));

		Subscription subscription1 = Subscription.builder()
			.id(UUID.randomUUID())
			.type(SubscriptionType.HTTP).build();
		Subscription subscription2 = Subscription.builder()
			.id(UUID.randomUUID())
			.type(SubscriptionType.HTTP).build();
		when(_subscriptionRepo.findAllByTenantIdAndTriggerId(any(), any()))
			.thenReturn(Stream.of(subscription1, subscription2));

		_cmd = InvokeTestTriggerCommand.builder()
			.tenantId(new TenantId("tenantId"))
			.requestId(UUID.randomUUID().toString())
			.triggerId(new TriggerId("triggerId"))
			.input(Optional.empty())
			.subscriptionIds(Optional.of(Collections.singleton(subscription1.getId())))
			.build();

		_cmd.handle(_triggerRepo, _subscriptionRepo, _invocationRepo, _eventPublisher, _properties, _etsFeatureStore);

		verify(_eventPublisher, times(1)).publish(_triggerInvokedEventCaptor.capture());
		assertEquals("tenantId", _triggerInvokedEventCaptor.getValue().getTenantId());
		assertEquals("triggerId", _triggerInvokedEventCaptor.getValue().getTriggerId());
		assertEquals("FIRE_AND_FORGET", _triggerInvokedEventCaptor.getValue().getType().toString());
		assertEquals(subscription1.getId().toString(), _triggerInvokedEventCaptor.getValue().getSubscriptionId());
		assertEquals(SubscriptionType.HTTP, _triggerInvokedEventCaptor.getValue().getSubscriptionType());
	}

	@Test
	public void testInvokingWithFilter() {
		when(_trigger.isEnabledForTenant(any())).thenReturn(true);
		when(_invocation.getId()).thenReturn(UUID.randomUUID());
		when(_trigger.invoke(any(), any(), any(), anyInt(), any())).thenReturn(_invocation);
		when(_trigger.getType()).thenReturn(TriggerType.FIRE_AND_FORGET);
		when(_trigger.getExampleInput()).thenReturn(JsonUtil.parse(Map.class, jsonAttributeChange));
		when(_triggerRepo.findById(any())).thenReturn(Optional.of(_trigger));

		Subscription subscription1 = Subscription.builder()
			.id(UUID.randomUUID())
			.type(SubscriptionType.HTTP)
			.filter("$.changes[?(@.attribute == 'manager')]")
			.build();
		Subscription subscription2 = Subscription.builder()
			.id(UUID.randomUUID())
			.type(SubscriptionType.HTTP)
			.filter("$.changes[?(@.attribute == 'custom')]")
			.build();
		Subscription subscription3 = Subscription.builder()
			.id(UUID.randomUUID())
			.type(SubscriptionType.HTTP)
			.enabled(true)
			.filter("$.changesss[?(@.attribute == 'causes exception in JsonPathUtil.isPathExist')]")
			.build();
		when(_subscriptionRepo.findAllByTenantIdAndTriggerId(any(), any()))
			.thenReturn(Stream.of(subscription1, subscription2, subscription3));

		givenCommand();

		_cmd.handle(_triggerRepo, _subscriptionRepo, _invocationRepo, _eventPublisher, _properties, _etsFeatureStore);

		//Verify that the subscription that filters on manager attribute alone is called
		verify(_eventPublisher, times(1)).publish(_triggerInvokedEventCaptor.capture());
		assertEquals(subscription1.getId().toString(), _triggerInvokedEventCaptor.getValue().getSubscriptionId());
	}

	private void givenCommand(Map<String, Object> input) {
		_cmd = InvokeTestTriggerCommand.builder()
			.tenantId(new TenantId("tenantId"))
			.requestId(UUID.randomUUID().toString())
			.triggerId(new TriggerId("triggerId"))
			.subscriptionIds(Optional.empty())
			.input(Optional.ofNullable(input))
			.headers(Collections.singletonMap("tenantId", UUID.randomUUID().toString()))
			.build();
	}

	private void givenCommand() {
		givenCommand(null);
	}

	private void whenTheCommandIsHandled() {

		_cmd.handle(_triggerRepo, _subscriptionRepo, _invocationRepo, _eventPublisher, _properties, _etsFeatureStore);
		verify(_invocationRepo).save(_invocationCaptor.capture());
		verify(_eventPublisher).publish(_triggerInvokedEventCaptor.capture());
	}

	private void thenTheFunctionShouldNotBeInvoked() {
		fail("The function should not be invoked.");
	}

	private void thenTheInvocationIsComplete() {
		assertEquals(_invocation, _invocationCaptor.getValue());
		assertEquals("tenantId", _triggerInvokedEventCaptor.getValue().getTenantId());
		assertEquals("triggerId", _triggerInvokedEventCaptor.getValue().getTriggerId());
		assertEquals(SubscriptionType.HTTP, _triggerInvokedEventCaptor.getValue().getSubscriptionType());
		assertNotNull(_triggerInvokedEventCaptor.getValue().getHeaders());
	}

	private String jsonAttributeChange = "{\n" +
		"  \"identity\": {\n" +
		"    \"id\": \"ee769173319b41d19ccec6cea52f237b\",\n" +
		"    \"name\": \"john.doe\",\n" +
		"    \"type\": \"IDENTITY\"\n" +
		"  },\n" +
		"  \"changes\": [\n" +
		"    {\n" +
		"      \"attribute\": \"department\",\n" +
		"      \"oldValue\": \"sales\",\n" +
		"      \"newValue\": \"marketing\"\n" +
		"    },\n" +
		"    {\n" +
		"      \"attribute\": \"manager\",\n" +
		"      \"oldValue\": {\n" +
		"        \"id\": \"ee769173319b41d19ccec6c235423237b\",\n" +
		"        \"name\": \"nice.guy\",\n" +
		"        \"type\": \"IDENTITY\"\n" +
		"      },\n" +
		"      \"newValue\": {\n" +
		"        \"id\": \"ee769173319b41d19ccec6c235423236c\",\n" +
		"        \"name\": \"mean.guy\",\n" +
		"        \"type\": \"IDENTITY\"\n" +
		"      }\n" +
		"    },\n" +
		"    {\n" +
		"      \"attribute\": \"email\",\n" +
		"      \"oldValue\": \"john.doe@hotmail.com\",\n" +
		"      \"newValue\": \"john.doe@gmail.com\"\n" +
		"    }\n" +
		"  ]\n" +
		"}";
}
