/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.event;

import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.service.VerifiedFromAddressService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OrgDeletedEventHandler.
 */
@RunWith(MockitoJUnitRunner.class)
public class OrgDeletedEventHandlerTest {

	@Mock
	VerifiedFromAddressService _verifiedFromAddressService;

	@Mock
	EventHandlerContext _eventHandlerContext;

	private OrgDeletedEventHandler _orgDeletedEventHandler;

	private String _pod;

	private String _org;

	private Event _orgDeleted;

	@Before
	public void setUp() {
		_orgDeletedEventHandler = new OrgDeletedEventHandler(_verifiedFromAddressService);
	}

	@Test
	public void orgDeletedTest() {
		givenPodAndOrg("dev", "acme-solar");
		givenOrgLifecycleEvent(EventType.ORG_DELETED);
		givenEventHandlerGetEventMock();

		// When handle event
		_orgDeletedEventHandler.handleEvent(_eventHandlerContext);

		// Verify deleteAll called
		verify(_verifiedFromAddressService, times(1)).deleteAll();
	}

	private void givenOrgLifecycleEvent(String eventType) {
		_orgDeleted = EventBuilder
				.withTypeAndContentJson(eventType, "{}")
				.addHeader(EventHeaders.POD, _pod)
				.addHeader(EventHeaders.ORG, _org)
				.build();
	}

	private void givenPodAndOrg(String pod, String org) {
		_pod = pod;
		_org = org;
	}

	private void givenEventHandlerGetEventMock() {
		when(_eventHandlerContext.getEvent()). thenReturn(_orgDeleted);
	}
}
