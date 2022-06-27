/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.domain.event;

import com.sailpoint.ets.domain.Secret;
import com.sailpoint.ets.domain.status.InvocationType;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.domain.trigger.TriggerType;
import lombok.Builder;
import lombok.Value;

import java.util.Map;
import java.util.Optional;

/**
 * TriggerInvokedEvent
 */
@Value
@Builder
public class TriggerInvokedEvent implements DomainEvent {
	private final String _triggerId;
	private final String _tenantId;
	private final String _invocationId;
	private final String _requestId;
	private final Secret _secret;
	private final InvocationType _invocationType;
	private final TriggerType _type;
	private final String _subscriptionId;
	private final String _subscriptionName;
	private final SubscriptionType _subscriptionType;
	private final String scriptSource;
	private final Map<String, Object> _subscriptionConfig;
	private final Map<String, Object> _input;
	private final Map<String, Object> _context;
	private final Map<String, String> _headers;

	@Override
	public Optional<String> getPartitionKey() {
		return Optional.of(_invocationId);
	}
}
