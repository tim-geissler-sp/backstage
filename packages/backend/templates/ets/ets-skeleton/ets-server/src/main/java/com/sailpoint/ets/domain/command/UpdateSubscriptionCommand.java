/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.command;

import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.subscription.Subscription;
import com.sailpoint.ets.domain.subscription.SubscriptionRepo;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.domain.trigger.EtsFeatureStore;
import com.sailpoint.ets.domain.trigger.TriggerId;
import com.sailpoint.ets.domain.trigger.TriggerRepo;
import com.sailpoint.ets.exception.IllegalUpdateException;
import com.sailpoint.ets.exception.NotFoundException;
import com.sailpoint.ets.exception.ValidationException;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static com.sailpoint.ets.infrastructure.util.EventBridgeConfigConverter.AWS_ACCOUNT_NUMBER;
import static com.sailpoint.ets.infrastructure.util.EventBridgeConfigConverter.AWS_REGION;

/**
 * Command to update subscription
 */
@Value
@Builder
public class UpdateSubscriptionCommand {

	private final UUID _id;
	private final TenantId _tenantId;
	private final TriggerId _triggerId;
	private final SubscriptionType _type;
	private final Duration _responseDeadline;
	private final Map<String, Object> _config;
	private final String _filter;
	private final String _scriptSource;
	private final String _name;
	private final String _description;
	private final boolean _enabled;

	public Subscription handle(TriggerRepo triggerRepo, SubscriptionRepo subscriptionRepo, EtsFeatureStore etsFeatureStore) {

		// Verify that subscription exists by ID
		Subscription subscription = subscriptionRepo.findById(_id)
			.filter( s -> s.getTenantId().equals(_tenantId) && s.getTriggerId().equals(_triggerId))
			.orElseThrow(() -> new NotFoundException("subscription", _id.toString()));

		// Verify that the trigger is enabled for the tenant
		triggerRepo.findById(_triggerId).filter(t -> t.isEnabledForTenant(etsFeatureStore))
			.orElseThrow(() -> new NotFoundException("trigger", _triggerId.toString()));

		// Make sure subscription can not be changed to or from event bridge to other types
		if (subscription.getType() != _type &&
			(subscription.getType() == SubscriptionType.EVENTBRIDGE || _type == SubscriptionType.EVENTBRIDGE)) {
			throw new IllegalUpdateException("type");
		}

		// Update config only if subscription type is not EVENTBRIDGE
		if (_type == SubscriptionType.EVENTBRIDGE) {
			if (!subscription.getConfig().get(AWS_REGION).equals(_config.get(AWS_REGION))) {
				throw new IllegalUpdateException(AWS_REGION);
			} else if (!subscription.getConfig().get(AWS_ACCOUNT_NUMBER).equals(_config.get(AWS_ACCOUNT_NUMBER))) {
				throw new IllegalUpdateException(AWS_ACCOUNT_NUMBER);
			}
		} else {
			subscription.setConfig(_config);
		}

		subscription.setResponseDeadline(_responseDeadline);
		subscription.setType(_type);
		subscription.setFilter(_filter);
		subscription.setScriptSource(_scriptSource);
		subscription.setName(_name);
		subscription.setDescription(_description);
		subscription.setEnabled(_enabled);

		subscriptionRepo.save(subscription);

		return subscription;
	}
}
