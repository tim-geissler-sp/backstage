/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service.normalizer;

import com.sailpoint.atlas.search.model.event.Event;
import com.sailpoint.audit.utils.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseNormalizerTest extends ParentNormalizerTest {

	BaseNormalizer _normalizer;

	@Before
	public void setUp() {
		super.setUp();
		TestUtils.setDummyRequestContext();
		_normalizer = new BaseNormalizer();
		_auditEvent.setAction("update");
		_auditEvent.setTarget("SomeDomainObject:value");
	}

	@Test
	public void testErrors() {
		Event event = _normalizer.normalize(_auditEvent, "someType");
		Assert.assertNotNull(event);
		Assert.assertTrue(event.getAttributes().get("errors") instanceof String);
	}

	@Test
	public void testErrorsNotListOfString() {
		Map<String, String> m = new HashMap<>();
		m.put("someKey", "someValue");

		_auditEvent.getAttributes().put("errors", Arrays.asList(new HashMap<>(), m));

		Event event = _normalizer.normalize(_auditEvent, "someType");
		Assert.assertNotNull(event);
		Assert.assertTrue(event.getAttributes().get("errors") instanceof String);
	}

	@Test
	public void testErrorsAsString() {
		_auditEvent.getAttributes().put("errors", "test error");

		Event event = _normalizer.normalize(_auditEvent, "someType");
		Assert.assertNotNull(event);
		Assert.assertTrue(event.getAttributes().get("errors") instanceof String);
	}
}