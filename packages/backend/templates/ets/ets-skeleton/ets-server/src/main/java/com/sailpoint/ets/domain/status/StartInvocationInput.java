/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.status;

import com.sailpoint.ets.domain.trigger.TriggerId;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.Map;

/**
 * Start Invocation Input.
 */
@Builder
@Getter
@EqualsAndHashCode
public class StartInvocationInput {
	@NonNull
	private final TriggerId _triggerId;

	@NonNull
	private final Map<String, Object> _input;

	private final Map<String, Object> _contentJson;
}
