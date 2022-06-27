/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * SourceId value type.
 */
@Value
@Builder
public class SourceId {
	@NonNull String _id;
	@NonNull String _name;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return _id;
	}
}
