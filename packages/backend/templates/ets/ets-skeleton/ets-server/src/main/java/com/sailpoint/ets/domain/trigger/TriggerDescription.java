/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.trigger;

import lombok.Value;

import java.io.Serializable;

/**
 * TriggerDescription
 */
@Value
public class TriggerDescription implements Serializable {

	private final String _value;

	@Override
	public String toString() {
		return _value;
	}
}
