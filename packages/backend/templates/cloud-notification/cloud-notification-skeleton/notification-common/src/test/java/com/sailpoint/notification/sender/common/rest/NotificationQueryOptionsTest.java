/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.rest;

import com.sailpoint.atlas.api.common.filters.FilterCompiler;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * Test NotificationQueryOptionsTest.
 */
public class NotificationQueryOptionsTest {

	@Test
	public void notificationQueryOptionsTest() {
        NotificationFilter filter = new NotificationFilter(FilterCompiler.LogicalOperation.EQ,
                "key", 10);
        NotificationQueryOptions options = new NotificationQueryOptions(1, 100, filter, Collections.emptyList());

        assertEquals(FilterCompiler.LogicalOperation.EQ, options.getFilter().get().getOperation());
        assertEquals(Integer.valueOf(1), options.getOffset());
		assertEquals(Integer.valueOf(100), options.getLimit());
		assertEquals("key", options.getFilter().get().getProperty());
	}
}
