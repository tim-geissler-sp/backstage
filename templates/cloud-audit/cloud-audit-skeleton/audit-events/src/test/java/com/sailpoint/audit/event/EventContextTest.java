/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.event;

import com.google.common.collect.ImmutableMap;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.audit.utils.TestUtils;
import com.sailpoint.iris.client.PodTopic;
import com.sailpoint.iris.server.EventHandlerContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Optional;

import static com.sailpoint.audit.event.EventContext.REQUEST_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EventContextTest {

	@Mock
	private EventHandlerContext _eventHandlerContext;

	private EventContext _context;

	@Before
	public void setUp() {

		RequestContext requestContext = TestUtils.setDummyRequestContext();

		when(_eventHandlerContext.getTopic()).thenReturn(new PodTopic(IdnTopic.SEARCH.getName(), requestContext.getPod()));
		when(_eventHandlerContext.getEvent()).thenReturn(new com.sailpoint.iris.client.Event("SAVED_SEARCH_CREATE_PASSED", ImmutableMap.of(REQUEST_ID, REQUEST_ID), "{}"));

		_context = new EventContext(_eventHandlerContext);
	}

	@Test
	public void test() {

		assertEquals(IdnTopic.SEARCH, _context.getIdnTopic());
		assertEquals(REQUEST_ID, _context.getRequestId());
		assertEquals("SAVED_SEARCH_CREATE_PASSED", _context.getEventType());
		assertEquals(Collections.emptyMap(), _context.getDomainEvent());

		assertTrue(_context.toString().contains("idnTopic=SEARCH, requestId=requestId, eventType=SAVED_SEARCH_CREATE_PASSED, domainEvent={}"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void error() {

		when(_eventHandlerContext.getTopic()).thenReturn(new PodTopic("noTopic", "pod"));

		_context = new EventContext(_eventHandlerContext);
	}
}
