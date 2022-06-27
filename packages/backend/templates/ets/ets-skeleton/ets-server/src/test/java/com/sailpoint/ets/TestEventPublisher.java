/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets;

import com.sailpoint.ets.domain.event.DomainEvent;
import com.sailpoint.ets.domain.event.EventPublisher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TestEventPublisher
 */
public class TestEventPublisher implements EventPublisher {

	private final List<DomainEvent> _events = Collections.synchronizedList(new ArrayList<>());

	@Override
	public void publish(DomainEvent domainEvent) {
		_events.add(domainEvent);
	}

	public List<DomainEvent> getEvents() {
		synchronized (_events) {
			return new ArrayList<>(_events);
		}
	}

}
