/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Exception thrown during a validation failure.
 * This includes the detailed field name that violates constraints and its value
 */
@Getter
@AllArgsConstructor
public class ValidationException extends RuntimeException {
	private String _fieldName;
	private String _fieldValue;

	public ValidationException(String fieldName, String fieldValue, Throwable cause) {
		super(cause);
		_fieldName = fieldName;
		_fieldValue = fieldValue;
	}
}
