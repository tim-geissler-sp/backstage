/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.infrastructure;

import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.sp.identity.event.domain.IdentityEventHeaders;
import com.sailpoint.sp.identity.event.domain.IdentityEventPublisher;
import com.sailpoint.sp.identity.event.domain.event.IdentityEvent;
import lombok.extern.apachecommons.CommonsLog;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * In-Memory implementation of IdentityEventPublisher
 */
@CommonsLog
public class NoOpIdentityEventPublisher implements IdentityEventPublisher {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void publish(IdentityEvent event, OffsetDateTime identityChangedTimestamp) {
		final String identityId = event.getIdentity().getId();
		final String eventType = event.getClass().getSimpleName();
		EventBuilder irisEventBuilder = EventBuilder.withTypeAndContent(eventType, event)
			.addHeader(EventHeaders.PARTITON_KEY, identityId);
		if (identityChangedTimestamp != null) {
			irisEventBuilder.addHeader(IdentityEventHeaders.IDENTITY_CHANGED_TIMESTAMP, DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(identityChangedTimestamp));
		}
		log.info(String.format("Publishing %s for identity %s", eventType, identityId));
	}
}
