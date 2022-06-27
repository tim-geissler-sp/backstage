/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.domain.command.status;

import com.google.common.collect.ImmutableMap;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.status.InvocationStatus;
import com.sailpoint.ets.domain.status.InvocationType;
import com.sailpoint.ets.domain.trigger.TriggerId;
import com.sailpoint.ets.domain.trigger.TriggerType;
import com.sailpoint.ets.infrastructure.status.DynamoDBInvocationStatusRepo;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

import static org.mockito.Mockito.verify;

/**
 * Unit tests for CreateInvocationStatusCommand.
 */
@RunWith(MockitoJUnitRunner.class)
public class CreateInvocationStatusCommandTest {
	@Mock
	DynamoDBInvocationStatusRepo _dynamoDBInvocationStatusRepo;
	@Captor
	ArgumentCaptor<InvocationStatus> _invocationStatusCaptor;

	@Test
	public void testCreateInvocation() {
		CreateInvocationStatusCommand cmd = CreateInvocationStatusCommand.builder()
			.invocationId(UUID.randomUUID())
			.tenantId(new TenantId("dev#acme-solar"))
			.context(ImmutableMap.of("valueContext1", "keyContext1"))
			.input(ImmutableMap.of("valueInput1", "keyInput1"))
			.subscriptionId(UUID.randomUUID())
			.triggerId(new TriggerId("idn:test"))
			.type(TriggerType.REQUEST_RESPONSE)
			.invocationType(InvocationType.REAL_TIME)
			.build();
		cmd.handle(_dynamoDBInvocationStatusRepo);
		verify(_dynamoDBInvocationStatusRepo).start(_invocationStatusCaptor.capture());

		Assert.assertEquals(cmd.getInvocationId(), _invocationStatusCaptor.getValue().getId());
		Assert.assertEquals(cmd.getTenantId(), _invocationStatusCaptor.getValue().getTenantId());
		Assert.assertEquals(cmd.getContext(), _invocationStatusCaptor.getValue().getStartInvocationInput().getContentJson());
		Assert.assertEquals(cmd.getInput(), _invocationStatusCaptor.getValue().getStartInvocationInput().getInput());
		Assert.assertEquals(cmd.getTriggerId(), _invocationStatusCaptor.getValue().getStartInvocationInput().getTriggerId());
		Assert.assertEquals(cmd.getTriggerId(), _invocationStatusCaptor.getValue().getTriggerId());
		Assert.assertEquals(cmd.getSubscriptionId(), _invocationStatusCaptor.getValue().getSubscriptionId());
		Assert.assertEquals(cmd.getInvocationType(), _invocationStatusCaptor.getValue().getType());
	}

	@Test
	public void testCreateInvocationFireAndForgetTrigger() {
		CreateInvocationStatusCommand cmd = CreateInvocationStatusCommand.builder()
			.invocationId(UUID.randomUUID())
			.tenantId(new TenantId("dev#acme-solar"))
			.context(ImmutableMap.of("valueContext1", "keyContext1"))
			.input(ImmutableMap.of("valueInput1", "keyInput1"))
			.subscriptionId(UUID.randomUUID())
			.triggerId(new TriggerId("idn:test"))
			.type(TriggerType.FIRE_AND_FORGET)
			.invocationType(InvocationType.REAL_TIME)
			.build();
		cmd.handle(_dynamoDBInvocationStatusRepo);
		verify(_dynamoDBInvocationStatusRepo).start(_invocationStatusCaptor.capture());

		Assert.assertEquals(cmd.getInvocationId(), _invocationStatusCaptor.getValue().getId());
		Assert.assertEquals(cmd.getTenantId(), _invocationStatusCaptor.getValue().getTenantId());
		Assert.assertEquals(cmd.getContext(), _invocationStatusCaptor.getValue().getStartInvocationInput().getContentJson());
		Assert.assertEquals(cmd.getInput(), _invocationStatusCaptor.getValue().getStartInvocationInput().getInput());
		Assert.assertEquals(cmd.getTriggerId(), _invocationStatusCaptor.getValue().getStartInvocationInput().getTriggerId());
		Assert.assertEquals(cmd.getTriggerId(), _invocationStatusCaptor.getValue().getTriggerId());
		Assert.assertEquals(cmd.getSubscriptionId(), _invocationStatusCaptor.getValue().getSubscriptionId());
		Assert.assertEquals(cmd.getInvocationType(), _invocationStatusCaptor.getValue().getType());
		Assert.assertNotNull(_invocationStatusCaptor.getValue().getCompleted());
		Assert.assertNotNull(_invocationStatusCaptor.getValue().getCompleteInvocationInput());
	}
}
