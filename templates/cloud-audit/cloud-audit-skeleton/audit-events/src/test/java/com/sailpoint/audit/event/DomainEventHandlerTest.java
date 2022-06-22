/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.event;

import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.audit.event.model.EventCatalog;
import com.sailpoint.audit.event.model.EventDescriptor;
import com.sailpoint.audit.event.normalizer.JsonPathNormalizer;
import com.sailpoint.audit.event.normalizer.NormalizerFactory;
import com.sailpoint.audit.service.AuditEventService;
import com.sailpoint.audit.service.util.AuditUtil;
import com.sailpoint.audit.utils.TestUtils;
import com.sailpoint.audit.verification.AuditVerificationRequest;
import com.sailpoint.audit.verification.AuditVerificationService;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.PodTopic;
import com.sailpoint.iris.server.EventHandlerContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import sailpoint.object.AuditEvent;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DomainEventHandlerTest {

	@Mock
	private EventHandlerContext _eventHandlerContext;

	@Mock
	private EventCatalog _eventCatalog;

	@Mock
	private NormalizerFactory _normalizerFactory;

	@Mock
	AuditEventService _auditEventService;

	@Mock
	AuditVerificationService _auditVerificationService;

	@Mock
	AuditUtil _util;

	@InjectMocks
	private DomainEventHandler _domainEventHandler;

	@Before
	public void setUp() throws Exception {

		RequestContext requestContext = TestUtils.setDummyRequestContext();

		when(_normalizerFactory.get(any(String.class))).thenReturn(new JsonPathNormalizer());
		when(_eventHandlerContext.getTopic()).thenReturn(new PodTopic(IdnTopic.SEARCH.getName(), requestContext.getPod()));
		when(_eventHandlerContext.getEvent()).thenReturn(new Event("SAVED_SEARCH_CREATE_PASSED", Collections.emptyMap(), "{}"));
		when(_eventHandlerContext.getTimestamp()).thenReturn(OffsetDateTime.now());

		when(_eventCatalog.get(eq(IdnTopic.SEARCH), eq("SAVED_SEARCH_CREATE_PASSED"))).thenReturn(Optional.of(EventDescriptor.NULL));
		when(_auditEventService.storeAuditEvent(any(com.sailpoint.atlas.search.model.event.Event.class))).thenReturn(new AuditEvent());
	}

	@Test
	public void handleEvent() {

		_domainEventHandler.handleEvent(_eventHandlerContext);

		verify(_util, times(1)).publishAuditEvent(any(com.sailpoint.atlas.search.model.event.Event.class), eq(true));
		verify(_auditVerificationService, times(1)).submitForVerification(any(AuditVerificationRequest.class));
	}

	@Test
	public void handleEventNullStore() {

		when(_auditEventService.storeAuditEvent(any(com.sailpoint.atlas.search.model.event.Event.class))).thenReturn(null);

		_domainEventHandler.handleEvent(_eventHandlerContext);

		verify(_util, never()).publishAuditEvent(any(com.sailpoint.atlas.search.model.event.Event.class), eq(true));
	}

	@Test
	public void handleEventNotFound() {

		when(_eventCatalog.get(eq(IdnTopic.SEARCH), eq("SAVED_SEARCH_CREATE_PASSED"))).thenReturn(Optional.empty());

		_domainEventHandler.handleEvent(_eventHandlerContext);

		verify(_util, never()).publishAuditEvent(any(com.sailpoint.atlas.search.model.event.Event.class), eq(true));
	}

	@Test
	public void testPublishAllAuditEvents() {
		//Test
		_domainEventHandler.handleEvent(_eventHandlerContext);

		//Verification
		verify(_util, times(1)).publishAuditEvent(any(com.sailpoint.atlas.search.model.event.Event.class), eq(true));
	}
}
