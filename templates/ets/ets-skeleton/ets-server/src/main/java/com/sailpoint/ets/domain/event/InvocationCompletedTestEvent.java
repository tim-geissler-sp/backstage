/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * InvocationCompletedTestEvent
 */
@Getter
public class InvocationCompletedTestEvent extends InvocationCompletedEvent {

	@Builder(builderMethodName = "testBuilder")
	public InvocationCompletedTestEvent(String tenantId, String triggerId, String invocationId,
		String requestId, Map<String, Object> output, Map<String, Object> context) {
		super(tenantId, triggerId, invocationId, requestId, output, context);
	}
}
