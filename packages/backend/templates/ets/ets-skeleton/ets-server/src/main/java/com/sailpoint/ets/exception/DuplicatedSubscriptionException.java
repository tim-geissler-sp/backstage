/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Exception thrown when a tenant is trying to subscribe to a trigger when it's already made.
 */
@Getter
@AllArgsConstructor
public class DuplicatedSubscriptionException extends RuntimeException {
	private String _triggerId;
}
