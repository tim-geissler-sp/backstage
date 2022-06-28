/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.trigger;

import lombok.Value;

import java.io.Serializable;

/**
 * TriggerName
 */
@Value
public class TriggerName implements Serializable {

	private final String _value;

	@Override
	public String toString() {return _value; }
}
