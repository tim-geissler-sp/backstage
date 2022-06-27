/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Exception thrown when a limit has been exceeded.
 */
@Getter
@AllArgsConstructor
public class LimitExceededException extends RuntimeException {
	private String _limitName;
	private String _maxValue;
}
