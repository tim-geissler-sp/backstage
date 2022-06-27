/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.trigger;

import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.domain.trigger.EtsFeatureStore;
import com.sailpoint.ets.domain.trigger.TriggerId;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * FeatureFlagEtsFeatureStore is an implementation for {@link EtsFeatureStore} that is based on atlas feature flag
 */
@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class FeatureFlagEtsFeatureStore implements EtsFeatureStore {
	private final FeatureFlagService _featureFlagService;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEnabledForTenant(TriggerId triggerId) {
		String triggerFlag = EtsFeatureStore.getFeatureKey(triggerId);
		return _featureFlagService.getBoolean(triggerFlag, false);
	}
}
