/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service.normalizer;

import org.junit.Before;
import sailpoint.object.Attributes;
import sailpoint.object.AuditEvent;

import java.util.Arrays;
import java.util.Date;

public class ParentNormalizerTest {

	AuditEvent _auditEvent;

	@Before
	public void setUp() {
		_auditEvent = new AuditEvent("Actor", "Action");
		_auditEvent.setCreated(new Date());
		_auditEvent.setApplication("[cc] My App [source-1234]");
		_auditEvent.setString4("I have information");

		Attributes<String, Object> attributes = new Attributes<>();
		attributes.put("errors", Arrays.asList("error1", "error2"));

		_auditEvent.setAttributes(attributes);
	}
}
