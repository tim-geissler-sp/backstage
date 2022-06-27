/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.email.service.model;

/**
 * EmailLockException is thrown when an email is being referenced in GlobalContext
 * as a result of which some write operations may be prohibited
 */
public class EmailReferencedException extends RuntimeException {
    public EmailReferencedException(String message) {
        super(message);
    }
}
