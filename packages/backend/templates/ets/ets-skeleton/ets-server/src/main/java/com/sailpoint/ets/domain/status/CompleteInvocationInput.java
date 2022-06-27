/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.status;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.Map;

/**
 * Complete Invocation Input.
 */
@Builder
@Getter
@EqualsAndHashCode
public class CompleteInvocationInput {
	private final Map<String, Object> _output;
	private final String _error;
}
