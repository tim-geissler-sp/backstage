/*
 * Copyright (C) 2020. SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.service.model;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sailpoint.object.Attributes;
import sailpoint.object.AuditEvent;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;

/**
 * Created by mark.boyle on 4/14/17.
 */
public class AuditDetailTest {

	@Mock
	AuditEvent _auditEvent;

	AuditDetails _auditDetails;

	Date testDate = new Date();
	String eventId = "A1234";
	String eventName = "Audit";
	String eventAction = "Audit Action";
	String eventSource = "Audit SOURCE";
	String eventTarget = "Audit TARGET";
	String eventApplication = "Audit APPLICAI";
	String eventAccount = "Audit Account";
	String eventType = "Audit Type";
	String eventHostName = "home";
	String eventIpaddr = "127.0.0.1";
	String eventContextId = "2345-dead-beef";
	String eventInfo = "Audit Event Testing";
	String eventTrackingId = "123456789-10";


	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);


		when(_auditEvent.getId()).thenReturn(eventId);
		when(_auditEvent.getName()).thenReturn(eventName);
		when(_auditEvent.getCreated()).thenReturn(testDate);
		when(_auditEvent.getAction()).thenReturn(eventAction);
		when(_auditEvent.getSource()).thenReturn(eventSource);
		when(_auditEvent.getTarget()).thenReturn(eventTarget);
		when(_auditEvent.getApplication()).thenReturn(eventApplication);
		when(_auditEvent.getAccountName()).thenReturn(eventAccount);
		when(_auditEvent.getInstance()).thenReturn(eventType);
		when(_auditEvent.getString1()).thenReturn(eventHostName);
		when(_auditEvent.getString2()).thenReturn(eventIpaddr);
		when(_auditEvent.getString3()).thenReturn(eventContextId);
		when(_auditEvent.getString4()).thenReturn(eventInfo);
		when(_auditEvent.getTrackingId()).thenReturn(eventTrackingId);
		when(_auditEvent.getAttributes()).thenReturn(new Attributes<String, Object>());

	}

	@Test
	public void constructorTest() {

		_auditDetails = new AuditDetails(_auditEvent);
		Assert.assertNotNull(_auditDetails);
		Assert.assertEquals(eventId, _auditDetails.getId());

		Assert.assertEquals(testDate, _auditDetails.getCreated() );
		Assert.assertEquals(eventAction, _auditDetails.getAction() );
		Assert.assertEquals(eventSource, _auditDetails.getSource() );
		Assert.assertEquals(eventTarget, _auditDetails.getTarget() );
		Assert.assertEquals(eventApplication, _auditDetails.getApplication() );
		Assert.assertEquals(eventAccount, _auditDetails.getAccountName() );
		Assert.assertEquals(eventType, _auditDetails.getType() );
		Assert.assertEquals(eventHostName, _auditDetails.getHostname() );
		Assert.assertEquals(eventIpaddr, _auditDetails.getIpaddr() );
		Assert.assertEquals(eventContextId, _auditDetails.getContextid() );
		Assert.assertEquals(eventInfo, _auditDetails.getInfo() );
		Assert.assertEquals(eventTrackingId, _auditDetails.getTrackingId() );

		Assert.assertNotNull( _auditDetails.getAttributes() );
		Assert.assertEquals(0 , _auditDetails.getAttributes().size());

	}

	@Test
	public void settersTest(){

		String newTrackingId = "10-987654321";
		String newAccountName = "Audits ROCK";
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("foo","baz");
		attributes.put("one",1);

		_auditDetails = new AuditDetails(_auditEvent);

		Assert.assertEquals(eventTrackingId, _auditDetails.getTrackingId() );
		_auditDetails.setTrackingId(newTrackingId);
		Assert.assertEquals( newTrackingId, _auditDetails.getTrackingId() );

		Assert.assertEquals(eventAccount, _auditDetails.getAccountName() );
		_auditDetails.setAccountName(newAccountName);
		Assert.assertEquals( newAccountName, _auditDetails.getAccountName() );

		Assert.assertEquals(0 , _auditDetails.getAttributes().size());
		_auditDetails.setAttributes(attributes);
		Assert.assertEquals(2 , _auditDetails.getAttributes().size());


	}
}
