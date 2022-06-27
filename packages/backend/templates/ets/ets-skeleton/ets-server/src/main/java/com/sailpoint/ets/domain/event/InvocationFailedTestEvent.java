/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * InvocationFailedTestEvent
 */
@Getter
public class InvocationFailedTestEvent extends InvocationFailedEvent {

	@Builder(builderMethodName = "testBuilder")
	public InvocationFailedTestEvent(String tenantId, String triggerId, String requestId, String invocationId,
		String reason, Map<String, Object> context) {
		super(tenantId, triggerId, requestId, invocationId, reason, context);
	}
}
