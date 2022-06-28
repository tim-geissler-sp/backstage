/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.event.dto;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Optional;

/**
 * IdentityAttributesChangedEvent
 */
@Value
@Builder
public class IdentityAttributesChangedEvent implements IdentityEvent {
	@NonNull IdentityReference _identity;
	@NonNull @Singular
    List<AttributeChange> _changes;

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
