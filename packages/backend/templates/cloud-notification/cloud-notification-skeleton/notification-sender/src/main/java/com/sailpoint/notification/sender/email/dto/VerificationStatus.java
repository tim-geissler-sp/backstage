/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.email.dto;

/**
 * Hermes Verification Status overwriting {@link com.amazonaws.services.simpleemail.model.VerificationStatus}
 */
public enum VerificationStatus {

	PENDING,
	SUCCESS,
	FAILED;
}
