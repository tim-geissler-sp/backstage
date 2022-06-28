/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.status;

import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.trigger.TriggerId;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * InvocationStatus.
 */
@Builder
@Getter
@Setter
public class InvocationStatus {
	@NonNull
	private final UUID _id;

	@NonNull
	private final TenantId _tenantId;

	@NonNull
	private final TriggerId _triggerId;

	@NonNull
	private final InvocationType _type;

	@NonNull
	private final UUID _subscriptionId;

	@NonNull
	private final OffsetDateTime _created;

	private OffsetDateTime _completed;

	private StartInvocationInput _startInvocationInput;

	private CompleteInvocationInput _completeInvocationInput;

	@Builder.Default
	private final String _subscriptionName = "";
}
