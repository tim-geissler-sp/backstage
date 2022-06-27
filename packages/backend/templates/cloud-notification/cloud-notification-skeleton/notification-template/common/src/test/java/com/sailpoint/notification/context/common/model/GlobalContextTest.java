/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.context.common.model;

import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

/**
 * Unit tests for GlobalContext model.
 */
@RunWith(MockitoJUnitRunner.class)
public class GlobalContextTest {

	@Before
	public void setUp() {
	}

	@Test
	public void newGlobalContextTest() {
		GlobalContext globalContext = new GlobalContext("acme-solar");
		globalContext.setAttributes(ImmutableMap.of("k1", "v1", "k2", 2, "k3",
				Arrays.asList("SailPoint", "Hermes")));
		globalContext.setCreated(DateTime.now().minusDays(1));
		globalContext.setModified(DateTime.now());

		Assert.assertEquals("acme-solar", globalContext.getTenant());
		Assert.assertEquals("v1", globalContext.getAttributes().get("k1"));
		Assert.assertEquals(2, globalContext.getAttributes().get("k2"));
		Assert.assertEquals(Arrays.asList("SailPoint", "Hermes"),
				globalContext.getAttributes().get("k3"));

		Assert.assertTrue(DateTime.now().minusDays(1).withTimeAtStartOfDay().isEqual(
				globalContext.getCreated().withTimeAtStartOfDay()));
		Assert.assertTrue(DateTime.now().withTimeAtStartOfDay().isEqual(
				globalContext.getModified().withTimeAtStartOfDay()));

	}

	@Test(expected = NullPointerException.class)
	public void requiredTenantConstructorTest() {
		new GlobalContext(null);
	}

}
