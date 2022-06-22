/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain.command;

import com.sailpoint.sp.identity.event.domain.IdentityStateRepository;
import com.sailpoint.sp.identity.event.domain.TenantId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Test DeleteTenantCommand
 */
@RunWith(MockitoJUnitRunner.class)
public class DeleteTenantCommandTest {

	@Mock
	IdentityStateRepository _identityStateRepository;

	@Test
	public void deleteTenantCommandTest() {
		TenantId tenantId = new TenantId("dev#acme-solar");
		DeleteTenantCommand cmd = DeleteTenantCommand.builder()
			.tenantId(tenantId)
			.build();

		cmd.handle(_identityStateRepository);
		verify(_identityStateRepository).deleteAllByTenant(eq(tenantId));
	}
}
