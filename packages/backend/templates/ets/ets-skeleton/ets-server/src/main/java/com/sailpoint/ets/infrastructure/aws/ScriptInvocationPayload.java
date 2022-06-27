/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.aws;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * Invocation payload for SCRIPT subscriptions
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class ScriptInvocationPayload extends InvocationPayload {
	private String _scriptCode;
}
