/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service.normalizer;

import com.sailpoint.atlas.search.model.event.Event;
import com.sailpoint.audit.utils.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CRUDNormalizerTest extends ParentNormalizerTest {

	CRUDNormalizer _normalizer;

	@Before
	public void setUp() {
		super.setUp();
		TestUtils.setDummyRequestContext();
		_normalizer = new CRUDNormalizer();
		_auditEvent.setAction("update");
		_auditEvent.setTarget("SomeDomainObject:value");
	}

	@Test
	public void testNonNullTarget() {
		Event event = _normalizer.normalize(_auditEvent, "someType");
		Assert.assertNotNull(event);
	}

	@Test
	public void testInvalidTarget() {
		_auditEvent.setTarget(null);

		Event event = _normalizer.normalize(_auditEvent, "someType");
		Assert.assertEquals(null, event.getTarget().getName());

		_auditEvent.setTarget("SomeDomainObject");
		event = _normalizer.normalize(_auditEvent, "someType");
		Assert.assertEquals("SomeDomainObject", event.getTarget().getName());

		_auditEvent.setTarget("SomeDomainObject:");
		event = _normalizer.normalize(_auditEvent, "someType");
		Assert.assertEquals("SomeDomainObject:", event.getTarget().getName());
	}

	@Test
	public void testTargetNameWithColon() {
		_auditEvent.setTarget("Cloud Role: This Role: Has a colon");

		Event event = _normalizer.normalize(_auditEvent, "someType");
		Assert.assertNotNull(event);
		Assert.assertEquals("This Role: Has a colon", event.getTarget().getName());
	}
}
