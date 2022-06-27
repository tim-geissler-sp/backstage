/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain.event;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Optional;

/**
 * IdentityAccountAttributesChangedEvent.
 */
@Value
@Builder
public class IdentityAccountAttributesChangedEvent implements IdentityEvent {
	@NonNull IdentityReference _identity;
	@NonNull AccountReference _account;
	@NonNull SourceReference _source;
	@NonNull @Singular List<AttributeChange> _changes;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IdentityReference getIdentity() {
		return _identity;
	}

	public Optional<AttributeChange> getChange(String attribute) {
		return _changes.stream()
			.filter(ac -> ac.getAttribute().equalsIgnoreCase(attribute))
			.findFirst();
	}
}
