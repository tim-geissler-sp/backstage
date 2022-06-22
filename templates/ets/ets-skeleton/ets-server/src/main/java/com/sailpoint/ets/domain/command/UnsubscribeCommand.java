/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.command;

import com.sailpoint.ets.domain.subscription.Subscription;
import com.sailpoint.ets.domain.subscription.SubscriptionRepo;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.exception.NotFoundException;
import com.sailpoint.ets.infrastructure.aws.EventBridge;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.UUID;

import static com.sailpoint.ets.infrastructure.util.EventBridgeConfigConverter.AWS_ACCOUNT_NUMBER;
import static com.sailpoint.ets.infrastructure.util.EventBridgeConfigConverter.AWS_PARTNER_EVENT_SOURCE_NAME;
import static com.sailpoint.ets.infrastructure.util.EventBridgeConfigConverter.AWS_REGION;

/**
 * UnsubscribeCommand
 */
@Value
@Builder
public class UnsubscribeCommand {

	@NonNull private final TenantId _tenantId;
	@NonNull private final UUID _subscriptionId;

	public void handle(SubscriptionRepo subscriptionRepo, EventBridge eventBridge) {
		Subscription subscription = subscriptionRepo.findById(_subscriptionId)
				.orElse(null);

		if(subscription == null || subscription.getTenantId() == null || !subscription.getTenantId().equals(_tenantId)) {
			throw new NotFoundException();
		}

		if (subscription.getType() == SubscriptionType.EVENTBRIDGE) {
			eventBridge.deletePartnerEventSource(subscription.getConfig().get(AWS_ACCOUNT_NUMBER).toString(),
				subscription.getConfig().get(AWS_REGION).toString(),
				subscription.getConfig().get(AWS_PARTNER_EVENT_SOURCE_NAME).toString());
		}

		subscriptionRepo.deleteById(subscription.getId());
	}
}
