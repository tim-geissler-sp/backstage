/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain.command;

import com.sailpoint.sp.identity.event.domain.IdentityStateRepository;
import com.sailpoint.sp.identity.event.domain.TenantId;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * DeleteTenant
 */
@Value
@Builder
public class DeleteTenantCommand {
	@NonNull TenantId _tenantId;

	/**
	 * Purges all data related to the specified tenant.
	 *
	 * @param identityStateRepository The new tenant state repository.
	 */
	public void handle(IdentityStateRepository identityStateRepository) {
		identityStateRepository.deleteAllByTenant(_tenantId);
	}
}
