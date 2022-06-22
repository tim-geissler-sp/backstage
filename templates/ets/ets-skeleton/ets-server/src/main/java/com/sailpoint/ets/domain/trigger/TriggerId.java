/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.trigger;

import lombok.Value;

import java.io.Serializable;

/**
 * TriggerId
 */
@Value
public class TriggerId implements Serializable {

	private final String _value;

	@Override
	public String toString() {
		return _value;
	}
}
