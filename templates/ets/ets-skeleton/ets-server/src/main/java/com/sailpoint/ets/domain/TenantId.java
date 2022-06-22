/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain;

import lombok.Value;

import java.io.Serializable;

/**
 * TenantId
 */
@Value
public class TenantId implements Serializable {

	private final String _value;

	@Override
	public String toString() {
		return _value;
	}
}
