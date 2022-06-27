/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.sp.identity.event;

import com.sailpoint.sp.identity.event.domain.IdentityEventPublisher;
import com.sailpoint.sp.identity.event.domain.event.IdentityEvent;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Identity EventPublisher for Integration Test
 */
public class TestIdentityEventPublisher implements IdentityEventPublisher {
	private final List<IdentityEvent> _events = Collections.synchronizedList(new ArrayList<>());

	@Override
	public void publish(IdentityEvent event, OffsetDateTime identityChangedTimestamp) {
		_events.add(event);
	}

	public List<IdentityEvent> getEvents(){
		synchronized (_events){
			return new ArrayList<>(_events);
		}
	}
}
