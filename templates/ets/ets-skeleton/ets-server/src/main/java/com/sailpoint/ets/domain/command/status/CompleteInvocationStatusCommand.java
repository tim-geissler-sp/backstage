/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.domain.command.status;

import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.status.CompleteInvocationInput;
import com.sailpoint.ets.infrastructure.status.DynamoDBInvocationStatusRepo;
import lombok.Builder;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.sailpoint.ets.infrastructure.util.TriggerEventLogUtil.logCompleteInvocationStatus;
import static com.sailpoint.ets.infrastructure.util.TriggerEventLogUtil.logExceptionWithInvocationId;

/**
 * CompleteInvocationStatusCommand.
 */
@Value
@Builder
@CommonsLog
public class CompleteInvocationStatusCommand {
	private final TenantId _tenantId;
	private final UUID _invocationId;
	private final String _error;
	private final Map<String, Object> _output;

	public void handle(final DynamoDBInvocationStatusRepo dynamoDBInvocationStatusRepo) {
		try {
			CompleteInvocationInput completeInvocation = CompleteInvocationInput.builder()
				.output(_output)
				.error(_error)
				.build();
			dynamoDBInvocationStatusRepo.complete(_tenantId, _invocationId, completeInvocation);

			log.info(logCompleteInvocationStatus("Complete Invocation Status succeeded.", Objects.toString(_invocationId), completeInvocation));
		} catch (Exception e) {
			log.warn(logExceptionWithInvocationId("Complete Invocation Status failed.", Objects.toString(_invocationId), e));
		}
	}
}
