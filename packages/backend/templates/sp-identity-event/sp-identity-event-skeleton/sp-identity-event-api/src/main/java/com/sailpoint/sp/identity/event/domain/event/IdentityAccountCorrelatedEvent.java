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
 * IdentityAccountCorrelatedEvent.
 */
@Value
@Builder
public class IdentityAccountCorrelatedEvent implements IdentityEvent {
	@NonNull IdentityReference _identity;
	@NonNull AccountReference _account;
	@NonNull SourceReference _source;
	@Singular Map<String, Object> _attributes;
	int _entitlementCount;


	/**
	 * {@inheritDoc}
	 */
	@Override
	public IdentityReference getIdentity() {
		return _identity;
	}
}
