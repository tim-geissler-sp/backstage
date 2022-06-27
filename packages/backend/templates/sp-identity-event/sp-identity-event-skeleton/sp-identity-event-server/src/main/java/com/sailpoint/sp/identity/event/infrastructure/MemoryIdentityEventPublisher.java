/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.infrastructure;

import com.sailpoint.utilities.JsonUtil;
import com.sailpoint.sp.identity.event.domain.IdentityEventPublisher;
import com.sailpoint.sp.identity.event.domain.event.IdentityEvent;
import java.time.OffsetDateTime;
import lombok.extern.apachecommons.CommonsLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-Memory implementation of IdentityEventPublisher
 */
@CommonsLog
public class MemoryIdentityEventPublisher implements IdentityEventPublisher {

	private final List<IdentityEvent> _events = Collections.synchronizedList(new ArrayList<>());

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void publish(IdentityEvent event, OffsetDateTime identityChangedTimestamp) {
		log.info(event.getClass().getSimpleName() + ": " + JsonUtil.toJsonPretty(event));
		_events.add(event);
	}

	/**
	 * Gets the list of events that have been published.
	 *
	 *  @return The list of events.
	 */
	public List<IdentityEvent> getEvents() {
		return new ArrayList<>(_events);
	}

	/**
	 * Clears all event data.
	 */
	public void clear() {
		_events.clear();
	}
}
