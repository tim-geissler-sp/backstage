/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.command;

import com.sailpoint.ets.domain.invocation.InvocationRepo;
import com.sailpoint.ets.domain.subscription.SubscriptionRepo;
import com.sailpoint.ets.domain.TenantId;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Command for delete tenant and clean up subscriptions, invocations.
 */
@Value
@Builder
public class DeleteTenantCommand {
	private final TenantId _tenantId;
	private static final Log _log = LogFactory.getLog(DeleteTenantCommand.class);

	public void handle(final SubscriptionRepo subscriptionRepo, final InvocationRepo invocationRepo) {
		try {
			subscriptionRepo.deleteAllByTenantId(_tenantId);
			invocationRepo.deleteAllByTenantId(_tenantId);

			_log.info("deleted all ETS subscriptions/invocations for TenantId: " + _tenantId);
		} catch (Exception ex) {
			_log.warn("error deletingETS subscriptions/invocations for tenant", ex);
		}
	}
}
