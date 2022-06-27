/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.rest;

import com.sailpoint.atlas.api.common.filters.FilterCompiler;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

/**
 * Test NotificationFilterBuilder.
 */
public class NotificationFilterBuilderTest {

	@Test
	public void notificationFilterBuilderTest() {
		NotificationFilterBuilder builder = new NotificationFilterBuilder();
		NotificationFilter filter = builder.newFilter(FilterCompiler.LogicalOperation.EQ,
				"key", 10);

		Assert.assertEquals(10, filter.getValue());
		Assert.assertEquals("key", filter.getProperty());
		Assert.assertEquals(FilterCompiler.LogicalOperation.EQ, filter.getOperation());

		try {
			builder.and(new ArrayList<>());
		} catch (IllegalArgumentException e) {
			Assert.fail();
		}

		try {
			builder.or(null);
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("OR is unsupported.", e.getMessage());
		}

		try {
			builder.ignoreCase(null);
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("Case sensitivity is not supported.", e.getMessage());
		}

		try {
			builder.newFilter(FilterCompiler.LogicalOperation.EQ,
					"key", 10, null);
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("Match Mode is only supported with the CO and SW operator.", e.getMessage());
		}
	}
}
