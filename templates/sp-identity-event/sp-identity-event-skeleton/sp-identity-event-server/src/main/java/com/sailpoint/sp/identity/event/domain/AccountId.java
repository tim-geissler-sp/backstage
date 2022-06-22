/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * AccountId value type.
 */
@Value
@Builder
public class AccountId {
	@NonNull String _id;
	@NonNull String _nativeIdentity;
	@NonNull String _name;
	String _uuid;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return _id;
	}
}
