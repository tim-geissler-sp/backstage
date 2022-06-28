/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.status;

import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Subscription status representing subscription count for a tenant
 */
@Data
@AllArgsConstructor
public class SubscriptionStatus {
	private TenantId tenant;
	private SubscriptionType type;
	private long count;
}
