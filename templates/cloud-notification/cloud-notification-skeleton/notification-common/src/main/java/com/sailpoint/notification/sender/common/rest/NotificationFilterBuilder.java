/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.rest;

import com.sailpoint.atlas.api.common.filters.FilterBuilder;
import com.sailpoint.atlas.api.common.filters.FilterCompiler;

import java.util.List;

/**
 * FilterBuilder implementations for support V3 API in hermes.
 * Implementations doesn't support complex logic.
 */
public class NotificationFilterBuilder implements FilterBuilder<NotificationFilter> {

	@Override
	public NotificationFilter and(List<NotificationFilter> filters) {
		return new NotificationFilter(filters);
	}

	@Override
	public NotificationFilter or(List<NotificationFilter> filters) {
		throw new IllegalArgumentException("OR is unsupported.");
	}

	@Override
	public NotificationFilter not(NotificationFilter filter) {
		throw new IllegalArgumentException("NOT is unsupported.");
	}

	@Override
	public NotificationFilter newFilter(FilterCompiler.LogicalOperation op, String property, Object valueObject) {
		return new NotificationFilter(op, property, valueObject);
	}

	@Override
	public NotificationFilter newFilter(FilterCompiler.LogicalOperation op, String property, Object valueObject, FilterCompiler.MatchMode mode) {
		if(!FilterCompiler.LogicalOperation.LIKE.toString().equalsIgnoreCase(op.toString())){
			throw new IllegalArgumentException("Match Mode is only supported with the CO and SW operator.");
		}
		return new NotificationFilter(op, property, valueObject, mode);
	}

	@Override
	public NotificationFilter ignoreCase(NotificationFilter filter) {
		throw new IllegalArgumentException("Case sensitivity is not supported.");
	}

	@Override
	public NotificationFilter newFilterWithValueList(FilterCompiler.LogicalOperation op, String property, List<Object> valueList) {
		if(!FilterCompiler.LogicalOperation.IN.equals(op)) {
			throw new IllegalArgumentException("Value lists are only supported with the IN operator.");
		}

		return new NotificationFilter(op, property, valueList);
	}
}
