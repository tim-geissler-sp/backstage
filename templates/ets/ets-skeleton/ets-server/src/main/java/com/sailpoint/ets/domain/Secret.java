/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain;

import lombok.Value;

import java.io.Serializable;
import java.util.UUID;

/**
 * Secret
 */
@Value
public class Secret implements Serializable {

	private final String _value;

	public static Secret generate() {
		return new Secret(UUID.randomUUID().toString());
	}

	@Override
	public String toString() {
		return _value;
	}
}
