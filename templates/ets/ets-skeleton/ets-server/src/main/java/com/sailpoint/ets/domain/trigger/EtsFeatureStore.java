/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.trigger;

import com.sailpoint.ets.domain.subscription.SubscriptionType;

/**
 * Feature store for ETS
 */
public interface EtsFeatureStore {

	/**
	 * Check if the trigger is enabled for the tenant in request context.
	 * @param triggerId the ID of the trigger
	 * @return true if the trigger is enabled for the tenant. False otherwise.
	 */
	boolean isEnabledForTenant(TriggerId triggerId);

	/**
	 * Returns the feature store key based on trigger Id.
	 *
	 * @param triggerId the ID of the trigger
	 * @return the feature store key
	 */
	static String getFeatureKey(TriggerId triggerId) {
		if (triggerId == null) {
			return null;
		}

		return "ETS_" + triggerId.toString().replaceAll("[:-]", "_").toUpperCase();
	}
}
