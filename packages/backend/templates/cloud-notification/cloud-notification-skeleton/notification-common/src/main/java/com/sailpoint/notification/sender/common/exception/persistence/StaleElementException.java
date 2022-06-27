/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.sender.common.exception.persistence;

/**
 *  Exception that indicates we are trying to overwrite old data to the persistence layer
 */
public class StaleElementException extends RuntimeException {

	public StaleElementException(Exception ex) {
		super(ex);
	}
}
