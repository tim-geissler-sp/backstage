/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.domain.event;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;

/**
 * InvocationFailedEvent
 */
@Getter
@Builder
public class InvocationFailedEvent implements DomainEvent, AckEvent {
	private final String _tenantId;
	private final String _triggerId;
	private final String _requestId;
	private final String _invocationId;
	private final String _reason;
	private final Map<String, Object> _context;

	@Override
	public Optional<String> getPartitionKey() {
		return Optional.of(_invocationId);
	}
}
