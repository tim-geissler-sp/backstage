/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.domain.event;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * DomainEvent
 */
public interface DomainEvent {
	@Deprecated
	String getTenantId();
	@Deprecated
	String getRequestId();

	//todo: default needs to be removed when getTenantId() and getRequestId() are removed.
	default Map<String, String> getHeaders() {
		return Collections.EMPTY_MAP;
	}

	default Optional<String> getPartitionKey() {
		return Optional.empty();
	}
}
