/*
 * Copyright (c) 2017. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service.model;

import org.junit.Test;
import sailpoint.object.Attributes;
import sailpoint.object.AuditEvent;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by mark.boyle on 10/4/17.
 */
public class AuditEventDTOTest {

	@Test
	public void baseConstructorTest(){
		AuditEventDTO auditEventDTO = new AuditEventDTO();
		assertNotNull(auditEventDTO.getCreated());
		assertNotNull(auditEventDTO.getUuid());
	}

	@Test
	public void getterSetterTest(){
		AuditEventDTO auditEventDTO = new AuditEventDTO();

		auditEventDTO.setAction("ACTION");
		auditEventDTO.setTarget("TARGET");
		auditEventDTO.setTargetType("TARGET-TYPE");
		auditEventDTO.setSource("SOURCE");
		auditEventDTO.setSourceType("SOURCE-TYPE");

		auditEventDTO.setType("TYPE");
		assertEquals("TYPE", auditEventDTO.getType());
		auditEventDTO.setInstance("SSO");
		assertEquals("SSO", auditEventDTO.getType());
		auditEventDTO.setInstance(null);
		assertEquals("TYPE", auditEventDTO.getType());

		auditEventDTO.setApplication("APPLICATION");
		auditEventDTO.setDescription("DESCRIPTION");
		auditEventDTO.setHostname("HOSTNAME");
		auditEventDTO.setIpaddr("12.34.56.78");
		auditEventDTO.setRequestId("1233456");
		auditEventDTO.setContextId("1234-5678");
		auditEventDTO.setInfo("INFO");
		auditEventDTO.setComment("COMMENT");
		auditEventDTO.setStack("aer");

		assertNotNull(auditEventDTO.getAction());
		assertNotNull(auditEventDTO.getTarget());
		assertNotNull(auditEventDTO.getSource());
		assertNotNull(auditEventDTO.getTargetType());
		assertNotNull(auditEventDTO.getSourceType());
		assertNotNull(auditEventDTO.getUuid());
		assertNotNull(auditEventDTO.getCreated());
		assertNotNull(auditEventDTO.getType());
		assertNotNull(auditEventDTO.getApplication());
		assertNotNull(auditEventDTO.getDescription());
		assertNotNull(auditEventDTO.getHostname());
		assertNotNull(auditEventDTO.getIpaddr());
		assertNotNull(auditEventDTO.getRequestId());
		assertNotNull(auditEventDTO.getContextId());
		assertNotNull(auditEventDTO.getInfo());
		assertNotNull(auditEventDTO.getComment());
		assertNotNull(auditEventDTO.getStack());
	}

	@Test
	public void setAttributes() {

		AuditEvent auditEvent = new AuditEvent();

		auditEvent.setAttributes(new Attributes<>());

		AuditEventDTO auditEventDTO = new AuditEventDTO(auditEvent);

		assertTrue(auditEventDTO.getAttributes() instanceof HashMap);
		assertFalse(auditEventDTO.getAttributes() instanceof Attributes);
	}
}
