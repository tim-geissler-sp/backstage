/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Exception thrown when an illegal update is attempted
 */
@Getter
@AllArgsConstructor
public class IllegalUpdateException extends RuntimeException {
	private final String _field;
}
