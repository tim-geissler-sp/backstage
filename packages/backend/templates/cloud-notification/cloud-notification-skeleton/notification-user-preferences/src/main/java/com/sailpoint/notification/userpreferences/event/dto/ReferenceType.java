/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.event.dto;

/*
 Type of attributes that reference non-identities (eg. authoritative_source is a SOURCE reference, identityProfile is an IDENTITY_PROFILE reference etc.)
 */
public enum ReferenceType {
	IDENTITY,
	SOURCE,
	IDENTITY_PROFILE,
	ACCOUNT,
	APP
}
