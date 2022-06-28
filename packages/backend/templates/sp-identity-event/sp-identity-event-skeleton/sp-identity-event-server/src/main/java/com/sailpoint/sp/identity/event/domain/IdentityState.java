/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.OffsetDateTime;

/**
 * IdentityState
 */
@Value
@Builder(toBuilder = true)
public class IdentityState {
	@NonNull Identity _identity;

	@NonNull OffsetDateTime _lastEventTime;
	boolean deleted;
	OffsetDateTime _expiration;

	/**
	 * Gets whether or not this State is expired.
	 *
	 * @return True if this state is expired, false otherwise.
	 */
	public boolean isExpired() {
		if (_expiration == null) {
			return false;
		}

		return _expiration.isBefore(OffsetDateTime.now());
	}
}
