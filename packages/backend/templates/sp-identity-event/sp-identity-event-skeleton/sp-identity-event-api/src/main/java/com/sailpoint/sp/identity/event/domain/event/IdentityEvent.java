/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain.event;

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
