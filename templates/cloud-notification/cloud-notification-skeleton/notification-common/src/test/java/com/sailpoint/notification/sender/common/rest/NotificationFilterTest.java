/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.rest;

import com.sailpoint.atlas.api.common.filters.FilterCompiler;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Test NotificationFilter.
 */
public class NotificationFilterTest {

	@Test
	public void notificationFilterTest() {
		List<NotificationFilter> filters = new ArrayList<>();
		NotificationFilter filter = new NotificationFilter(FilterCompiler.LogicalOperation.EQ,
				"key", 10);

		Assert.assertEquals(10, filter.getValue());
		Assert.assertEquals("key", filter.getProperty());
		Assert.assertEquals(FilterCompiler.LogicalOperation.EQ, filter.getOperation());
		Assert.assertEquals(1, filter.getFilters().size());
		filters.add(filter);

		NotificationFilter filterLocale = new NotificationFilter(FilterCompiler.LogicalOperation.EQ,
				"locale", Locale.FRANCE);
		filters.add(filterLocale);

		NotificationFilter complex = new NotificationFilter(filters);

		Assert.assertEquals(2, complex.getFilters().size());
		Assert.assertNull(complex.getValue());
		Assert.assertNull(complex.getProperty());
		Assert.assertNull(complex.getOperation());

		NotificationFilter valueListFilter = new NotificationFilter(FilterCompiler.LogicalOperation.IN,
				"key", Arrays.asList("one", "two"));
		Assert.assertNotNull(valueListFilter.getFilters());
		Assert.assertEquals(2, valueListFilter.getValueList().size());
		Assert.assertEquals(2, valueListFilter.getFilters().get(0).getValueList().size());
		Assert.assertEquals(FilterCompiler.LogicalOperation.IN, valueListFilter.getOperation());

		NotificationFilter modeFilter = new NotificationFilter(FilterCompiler.LogicalOperation.LIKE, "name",
				"e2e", FilterCompiler.MatchMode.START);
		Assert.assertNotNull(modeFilter.getFilters());
		Assert.assertEquals(FilterCompiler.MatchMode.START, modeFilter.getMode());
		Assert.assertNotNull(modeFilter.getProperty());
		Assert.assertNotNull(modeFilter.getValue());
	}
}
