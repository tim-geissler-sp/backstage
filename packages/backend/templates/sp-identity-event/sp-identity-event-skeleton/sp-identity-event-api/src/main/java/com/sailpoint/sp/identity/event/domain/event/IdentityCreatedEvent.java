/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain.event;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

/**
 * IdentityCreatedEvent
 */
@Value
@Builder
public class IdentityCreatedEvent implements IdentityEvent {
	@NonNull IdentityReference _identity;
	@Singular Map<String, Object> _attributes;
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IdentityReference getIdentity() {
		return _identity;
	}
}
