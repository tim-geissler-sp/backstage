/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.aws;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * Invocation payload with general attributes for all subscription types
 */
@Data
@SuperBuilder
public class InvocationPayload {
	private String _triggerId;
	private String _triggerType;
	private String _invocationId;
	private Map<String, String> _headers;
	private Map<String, Object> _input;
	private String _responseMode;
	private String _callbackUrl;
	private String _secret;
	private Map<String, String> _metadata;
}
