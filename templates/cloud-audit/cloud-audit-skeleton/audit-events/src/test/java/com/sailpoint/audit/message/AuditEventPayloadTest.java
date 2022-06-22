/*
 * Copyright (c) 2017. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.message;


import com.sailpoint.atlas.util.JsonUtil;
import com.sailpoint.audit.service.model.AuditEventDTO;
import org.junit.Before;
import org.junit.Test;
import org.testng.Assert;
import sailpoint.object.AuditEvent;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by mark.boyle on 5/25/17.
 */
public class AuditEventPayloadTest {

	AuditEventPayload auditEventPayload;

	String testStr = "TEST STRING";
	@Before
	public void setUp(){
		auditEventPayload = new AuditEventPayload();
		auditEventPayload.setAuditEventXml(testStr);
		auditEventPayload.setUseAerStorage( false );
		AuditEvent ae = new AuditEvent("Test-Source", "Test-Action");
		AuditEventDTO _auditEvent = new AuditEventDTO(ae);
		auditEventPayload.setAuditEventJson(_auditEvent);
	}
	@Test
	public void testGetterSetter(){
		Assert.assertNotNull(auditEventPayload);
		Assert.assertEquals(auditEventPayload.getAuditEventXml(), testStr);
		Assert.assertFalse(auditEventPayload.isUseAerStorage());
		Assert.assertTrue(auditEventPayload.hasXml());
		Assert.assertTrue(auditEventPayload.hasJson());
		// Set to null and test again.
		auditEventPayload.setAuditEventXml(null);
		Assert.assertFalse(auditEventPayload.hasXml());
		auditEventPayload.setAuditEventXml("");
		Assert.assertFalse(auditEventPayload.hasXml());
		auditEventPayload.setAuditEventJson((Map)null);
		Assert.assertFalse(auditEventPayload.hasJson());
		Assert.assertNull(auditEventPayload.getAuditEventJson());
	}

}
