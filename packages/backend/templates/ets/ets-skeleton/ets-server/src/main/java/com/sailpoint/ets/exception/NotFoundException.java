/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Exception thrown when a referenced object is not found.
 * This includes the detailed name of this object and the value that is being referenced.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NotFoundException extends RuntimeException {
	private String _fieldName;
	private String _fieldValue;
}
