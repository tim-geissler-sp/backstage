/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.domain.command.status;

import com.google.common.collect.ImmutableMap;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.infrastructure.status.DynamoDBInvocationStatusRepo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for CompleteInvocationStatusCommand.
 */
@RunWith(MockitoJUnitRunner.class)
public class CompleteInvocationStatusCommandTest {
	@Mock
	DynamoDBInvocationStatusRepo _dynamoDBInvocationStatusRepo;


	@Test
	public void testInvocationCompleted() {
		CompleteInvocationStatusCommand cmd = CompleteInvocationStatusCommand.builder()
			.invocationId(UUID.randomUUID())
			.tenantId(new TenantId("dev#acme-solar"))
			.output(ImmutableMap.of("value1", "key1"))
			.build();
		cmd.handle(_dynamoDBInvocationStatusRepo);
		verify(_dynamoDBInvocationStatusRepo).complete(eq(cmd.getTenantId()), eq(cmd.getInvocationId()), any());
	}
}
