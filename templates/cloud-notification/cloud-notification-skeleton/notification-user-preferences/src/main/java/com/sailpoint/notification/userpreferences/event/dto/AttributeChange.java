/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.event.dto;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Value type for a changed attribute.
 */
@Value
@Builder
public class AttributeChange {
	@NonNull String _attribute;
	Object _oldValue;
	Object _newValue;
}