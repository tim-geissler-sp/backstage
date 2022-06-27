/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.util;

import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.mantis.core.service.model.AuditEventActions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import sailpoint.object.AuditEvent;

import java.util.Date;
import java.util.UUID;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

public class AuditEventComparatorTest {

	public static final Log log = LogFactory.getLog(AuditEventComparatorTest.class);

	Date nowDate = new Date();

	public AuditEvent getAuditEvent() {

		AuditEvent auditEvent = new AuditEvent();
		auditEvent.setCreated(nowDate);
		auditEvent.setId(UUID.randomUUID().toString().replaceAll("-", ""));

		auditEvent.setAccountName("jane.doe");
		auditEvent.setAction(AuditEventActions.ACTION_CREATE_ACCOUNT);
		auditEvent.setTarget("jane.doe");
		auditEvent.setSource("identityRefresh");
		auditEvent.setInstance("SSO");
		auditEvent.setApplication("Active Directory");
		auditEvent.setString1("localhost");
		auditEvent.setString2("127.0.0.1");
		auditEvent.setString3("7c5ac42af980344b01");
		auditEvent.setString4("NONE");

		// note: The ordering of keys in this map makes JSON diff'ing interesting.
		auditEvent.setAttribute("attrib1Key", "attrib1Val");
		auditEvent.setAttribute("attrib2Key", "attrib2Val");
		auditEvent.setAttribute("attrib3Key", "attrib3Val");
		auditEvent.setAttribute("attrib4Key", UUID.randomUUID().toString());

		auditEvent.setInterface("Is this ever used!?");

		return auditEvent;
	}

	@Test
	public void testEquality() {

		AuditEvent auditEvent = getAuditEvent();

		String aeJson = JsonUtils.toJson(auditEvent);
		AuditEvent readBack = JsonUtils.parse(AuditEvent.class, aeJson);

		AuditEventComparator aeComp = new AuditEventComparator();

		String objDiff = aeComp.diffAuditEvents(auditEvent, readBack);
		assertNull("Should get no differences, got:" + objDiff, objDiff);

		String jsonDiff = aeComp.diffAuditEvents(auditEvent, aeJson);
		assertNull("Should get no differences, got:" + jsonDiff, jsonDiff);

		// Mess with the ordering of the attributes field keys.
		readBack.setAttribute("attrib3Key", "attrib3Val");
		readBack.setAttribute("attrib2Key", "attrib2Val");
		String attrDiff = aeComp.diffAuditEvents(auditEvent, readBack);
		assertNull("Should get no differences, got:" + attrDiff, attrDiff);

	}

	@Test
	public void testInEquality() {

		AuditEvent auditEvent = getAuditEvent();

		String aeJson = JsonUtils.toJson(auditEvent);

		AuditEventComparator aeComp = new AuditEventComparator();

		AuditEvent changedId = JsonUtils.parse(AuditEvent.class, aeJson);
		changedId.setId(UUID.randomUUID().toString().replaceAll("-", ""));

		String objDiff = aeComp.diffAuditEvents(changedId, auditEvent);
		assertNotNull("Should get differences, got:" + objDiff, objDiff);
		log.info("Got diff: " + objDiff);

		String jsonDiff = aeComp.diffAuditEvents(changedId, aeJson);
		assertNotNull("Should get differences, got:" + jsonDiff, jsonDiff);
		log.info("Got diff: " + jsonDiff);

		// Mess with the ordering of the attributes field _and_ the contents.
		AuditEvent readBack = JsonUtils.parse(AuditEvent.class, aeJson);
		readBack.setAttribute("attrib4Key", UUID.randomUUID().toString());
		String attrDiff = aeComp.diffAuditEvents(auditEvent, readBack);
		assertNotNull("Should get differences, got:" + attrDiff, attrDiff);
		log.info("Got diff: " + attrDiff);

		// Mess with the ordering of the attributes field _and_ the contents.
		AuditEvent readBack2 = JsonUtils.parse(AuditEvent.class, aeJson);
		readBack2.setAttribute("attrib4Key", null);
		readBack2.setAttribute("attrib5Key", UUID.randomUUID().toString());
		String attrDiff2 = aeComp.diffAuditEvents(auditEvent, readBack2);
		assertNotNull("Should get differences, got:" + attrDiff2, attrDiff2);
		log.info("Got diff: " + attrDiff2);

	}
}
