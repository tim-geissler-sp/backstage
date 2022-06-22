/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.event.dto;

/**
 * Marker interface for IdentityEvents
 */
public interface IdentityEvent {

	/**
	 * Gets the identity related to this event.
	 *
	 * @return The identity.
	 */
	IdentityReference getIdentity();

}
