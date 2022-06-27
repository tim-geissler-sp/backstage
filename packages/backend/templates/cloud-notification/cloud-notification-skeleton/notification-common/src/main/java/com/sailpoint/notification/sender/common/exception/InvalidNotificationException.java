/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.sender.common.exception;

public class InvalidNotificationException extends Exception {

	public InvalidNotificationException(String message) {
		super(message);
	}

	public InvalidNotificationException(Exception ex) {
		super(ex);
	}
}
