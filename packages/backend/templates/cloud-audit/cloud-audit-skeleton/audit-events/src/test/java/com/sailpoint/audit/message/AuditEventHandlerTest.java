/*
 * Copyright (c) 2017. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.message;

import com.sailpoint.atlas.messaging.server.MessageHandlerContext;
import com.sailpoint.audit.service.AuditEventService;
import com.sailpoint.mantis.core.service.CrudService;
import com.sailpoint.mantis.core.service.XmlService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import sailpoint.object.AuditEvent;

import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by mark.boyle on 5/25/17.
 */
public class AuditEventHandlerTest {

	@Mock
	CrudService _crudService;

	@Mock
	XmlService _xmlService;

	@Mock
	AuditEvent _auditEvent;

	@Mock
	AuditEventPayload _auditEventPayload;

	@Mock
	MessageHandlerContext _context;

	AuditEventHandler auditEventHandler;

	@Mock
	AuditEventService auditEventService;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		_auditEvent = new AuditEvent("TEST","TEST-ACTION","TEST-SRC");
		_auditEvent.setCreated(new Date());
		_auditEvent.setId("1234-DEAD-BEEF");
		_auditEvent.setLock("true");
		_auditEvent.setTrackingId("1234");
		_auditEvent.setApplication("[CC]");
		_auditEvent.setExtended1("FOO");
		when(_xmlService.parse(any())).thenReturn(_auditEvent);
		when(_auditEventPayload.isUseAerStorage()).thenReturn(false);

		when(_context.getMessageContent(any())).thenReturn(_auditEventPayload);
		auditEventHandler = new AuditEventHandler();
		auditEventHandler._auditEventService = auditEventService;
	}

	@Test
	public void testMessageType(){
		Assert.assertNotNull(AuditEventHandler.MessageType.values());
		Assert.assertEquals(AuditEventHandler.MessageType.AUDIT_EVENT , AuditEventHandler.MessageType.valueOf("AUDIT_EVENT"));
	}

	@Test
	public void testHandleMessage(){
		try {
			auditEventHandler.handleMessage(_context);
		} catch(Exception exc){
			System.out.println("Failed to handle message int audit-event test : " + exc.getMessage());
		}
		verify(auditEventService, atLeastOnce()).processAuditMessage(any(), any());
	}

}
