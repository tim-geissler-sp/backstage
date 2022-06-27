/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Exception thrown when a field exceeded it's size limit
 */
@Getter
@AllArgsConstructor
public class FieldTooLargeException extends RuntimeException {
	private String _field;
	private String _maxSize;
}
