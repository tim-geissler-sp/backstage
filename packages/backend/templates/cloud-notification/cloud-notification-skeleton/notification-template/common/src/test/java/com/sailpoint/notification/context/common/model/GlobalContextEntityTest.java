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
 * Unit tests for GlobalContextEntity model.
 */
@RunWith(MockitoJUnitRunner.class)
public class GlobalContextEntityTest {

	@Before
	public void setUp() {
	}

	@Test
	public void newGlobalContextTest() {
		GlobalContextEntity globalContextEntity = new GlobalContextEntity();
		globalContextEntity.setTenant("acme-solar");
		globalContextEntity.setAttributes(ImmutableMap.of("k1", "v1", "k2", 2, "k3",
				Arrays.asList("SailPoint", "Hermes")));
		globalContextEntity.setCreated(DateTime.now().minusDays(1));
		globalContextEntity.setModified(DateTime.now());

		Assert.assertEquals("acme-solar", globalContextEntity.getTenant());
		Assert.assertEquals("v1", globalContextEntity.getAttributes().get("k1"));
		Assert.assertEquals(2, globalContextEntity.getAttributes().get("k2"));
		Assert.assertEquals(Arrays.asList("SailPoint", "Hermes"),
				globalContextEntity.getAttributes().get("k3"));

		Assert.assertTrue(DateTime.now().minusDays(1).withTimeAtStartOfDay().isEqual(
				globalContextEntity.getCreated().withTimeAtStartOfDay()));
		Assert.assertTrue(DateTime.now().withTimeAtStartOfDay().isEqual(
				globalContextEntity.getModified().withTimeAtStartOfDay()));

	}
}
