/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain;

import com.sailpoint.sp.identity.event.domain.event.IdentityEvent;
import java.time.OffsetDateTime;

/**
 * IdentityEventPublisher is a domain interface for publication of the identity-related
 * events.
 */
public interface IdentityEventPublisher {

	/**
	 * Publishes the specified event.
	 *
	 * @param event The event to publish.
	 * @param identityChangedTimestamp The timestamp of the source IDENTITY_CHANGED event
	 */
	void publish(IdentityEvent event, OffsetDateTime identityChangedTimestamp);
}
