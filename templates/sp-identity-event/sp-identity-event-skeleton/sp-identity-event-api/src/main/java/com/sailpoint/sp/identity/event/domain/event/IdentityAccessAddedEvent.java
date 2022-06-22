/*
 * Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain.event;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Map;

/**
 * IdentityAccessAddedEvent.
 */
@Value
@Builder
public class IdentityAccessAddedEvent implements IdentityEvent {
	@NonNull IdentityReference identity;
	@NonNull AccessReference access;
	@NonNull Map<String, Object> attributes;
}
