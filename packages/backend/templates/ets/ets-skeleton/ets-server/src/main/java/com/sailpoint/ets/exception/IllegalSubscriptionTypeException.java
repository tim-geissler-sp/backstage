/*
 * Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Exception thrown when a tenant is trying to subscribe to an internal trigger without an internal claim.
 */
@Getter
@AllArgsConstructor
public class IllegalSubscriptionTypeException extends RuntimeException {
	private final String _field;
}
