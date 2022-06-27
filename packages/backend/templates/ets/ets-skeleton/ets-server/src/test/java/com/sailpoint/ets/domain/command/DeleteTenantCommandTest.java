/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.domain.command;

import com.sailpoint.atlas.boot.core.web.TenantIdentifier;
import com.sailpoint.ets.domain.invocation.InvocationRepo;
import com.sailpoint.ets.domain.subscription.SubscriptionRepo;
import com.sailpoint.ets.domain.TenantId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for DeleteTenantCommand.
 */
@RunWith(MockitoJUnitRunner.class)
public class DeleteTenantCommandTest {

	@Mock
	private SubscriptionRepo _subscriptionRepo;
	@Mock
	private InvocationRepo _invocationRepo;

	@Test
	public void orgDeleted() {
		DeleteTenantCommand cmd = DeleteTenantCommand.builder()
			.tenantId(new TenantId(new TenantIdentifier("dev", "acme-solar").toString()))
			.build();

		TenantId tenantId = new TenantId(new TenantIdentifier("dev", "acme-solar").toString());
		cmd.handle(_subscriptionRepo, _invocationRepo);

		verify(_subscriptionRepo).deleteAllByTenantId((eq(tenantId)));
		verify(_invocationRepo).deleteAllByTenantId((eq(tenantId)));
	}
}
