/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.rest;

import com.sailpoint.atlas.api.common.filters.FilterCompiler;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationFilter implementations for support V3 API in hermes.
 */
@Getter
public class NotificationFilter {

	private final String _property;
	private final Object _value;
	private final List<Object> _valueList;
	private final FilterCompiler.LogicalOperation _operation;

	private final List<NotificationFilter> _filters;
	private final FilterCompiler.MatchMode _mode;

	public NotificationFilter(FilterCompiler.LogicalOperation op,
						String property,
						Object value) {
		_operation = op;
		_property = property;
		_value = value;
		_valueList = null;
		_filters = new ArrayList<>();
		_filters.add(this);
		_mode = null;
	}

	public NotificationFilter(FilterCompiler.LogicalOperation op,
							  String property,
							  Object value,
							  FilterCompiler.MatchMode mode) {
		_operation = op;
		_property = property;
		_value = value;
		_valueList = null;
		_filters = new ArrayList<>();
		_filters.add(this);
		_mode = mode;
	}

	public NotificationFilter(FilterCompiler.LogicalOperation op,
							  String property,
							  List<Object> valueList) {
		_operation = op;
		_property = property;
		_value = null;
		_valueList = valueList;
		_filters = new ArrayList<>();
		_filters.add(this);
		_mode = null;
	}

	public NotificationFilter(List<NotificationFilter> filters) {
		_operation = null;
		_property = null;
		_value = null;
		_valueList = null;
		_filters = filters;
		_mode = null;
	}

	public String getProperty() {
		return _property;
	}

	public Object getValue() {
		return _value;
	}

	public List<Object> getValueList() {
		return _valueList;
	}

	public FilterCompiler.LogicalOperation getOperation() {
		return _operation;
	}

	public List<NotificationFilter> getFilters() {
		return _filters;
	}


}
