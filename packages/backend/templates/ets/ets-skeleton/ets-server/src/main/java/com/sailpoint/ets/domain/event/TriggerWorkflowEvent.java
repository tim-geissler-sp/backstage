/*
 * Copyright (C) 2022 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;

/**
 * TriggerWorkflowEvent
 */
@Getter
@Builder
public class TriggerWorkflowEvent implements DomainEvent {
	private final String _tenantId;
	private final String _triggerId;
	private final String _requestId;
	private final String _workflowId;
	private final Map<String, Object> _input;
	private final Map<String, String> _headers;

	@Override
	public Optional<String> getPartitionKey() {
		return Optional.of(_workflowId);
	}

}
