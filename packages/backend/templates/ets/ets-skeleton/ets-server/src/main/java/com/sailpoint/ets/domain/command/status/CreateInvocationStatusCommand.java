/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.domain.command.status;

import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.status.CompleteInvocationInput;
import com.sailpoint.ets.domain.status.InvocationStatus;
import com.sailpoint.ets.domain.status.InvocationType;
import com.sailpoint.ets.domain.status.StartInvocationInput;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.domain.trigger.TriggerId;
import com.sailpoint.ets.domain.trigger.TriggerType;
import com.sailpoint.ets.infrastructure.status.DynamoDBInvocationStatusRepo;
import lombok.Builder;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.sailpoint.ets.infrastructure.util.TriggerEventLogUtil.logCreateInvocationStatus;
import static com.sailpoint.ets.infrastructure.util.TriggerEventLogUtil.logExceptionWithInvocationId;

/**
 * CreateInvocationStatusCommand.
 */
@Value
@Builder
@CommonsLog
public class CreateInvocationStatusCommand {
	private final UUID _invocationId;
	private final TriggerId _triggerId;
	private final TenantId _tenantId;
	private final TriggerType _type;
	private final InvocationType _invocationType;
	private final UUID _subscriptionId;
	private final String _subscriptionName;
	private final SubscriptionType _subscriptionType;
	private final Map<String, Object> _subscriptionConfig;
	private final Map<String, Object> _input;
	private final Map<String, Object> _context;


	public void handle(final DynamoDBInvocationStatusRepo dynamoDBInvocationStatusRepo) {
		try {
			InvocationStatus invocationStatus = InvocationStatus.builder()
				.id(_invocationId)
				.tenantId(_tenantId)
				.triggerId(_triggerId)
				.subscriptionId(_subscriptionId)
				.subscriptionName(_subscriptionName)
				.type(_invocationType)
				.created(OffsetDateTime.now())
				.startInvocationInput(StartInvocationInput.builder()
					.input(_input)
					.triggerId(_triggerId)
					.contentJson(_context)
					.build())
				.build();

			//complete for fire and forget trigger.
			if(_type == TriggerType.FIRE_AND_FORGET) {
				invocationStatus.setCompleted(OffsetDateTime.now());
				invocationStatus.setCompleteInvocationInput(CompleteInvocationInput
					.builder()
					.build());
			} else if (_subscriptionType == SubscriptionType.INLINE) { // Immediately set completed status for INLINE subscription
				invocationStatus.setCompleted(OffsetDateTime.now());
				invocationStatus.setCompleteInvocationInput(CompleteInvocationInput
					.builder()
					.output((Map<String, Object>) _subscriptionConfig.get("output"))
					.error((String) _subscriptionConfig.get("error"))
					.build());
			}
			dynamoDBInvocationStatusRepo.start(invocationStatus);

			log.info(logCreateInvocationStatus("Create Invocation Status succeeded.", invocationStatus));
		} catch (Exception e) {
			log.error(logExceptionWithInvocationId("Create Invocation Status failed.", Objects.toString(_invocationId), e));
		}
	}
}
