/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.aws;

import lombok.Builder;
import lombok.Data;

/**
 * Meta data that is sent out as part of invocation payload
 */
@Data
@Builder
public class MetadataPayload {
	private String _invocationId;
	private String _triggerId;
	private String _triggerType;

	private String _responseMode;
	private String _secret;
	private String _callbackURL;
}
