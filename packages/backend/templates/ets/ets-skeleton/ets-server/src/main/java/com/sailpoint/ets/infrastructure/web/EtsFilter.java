/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web;

import com.sailpoint.atlas.api.common.filters.FilterCompiler;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Http filter for Trigger Service V3 compliance
 */
@Getter
public class EtsFilter {

	private final String _property;
	private final Object _value;
	private final FilterCompiler.LogicalOperation _operation;
	private final List<EtsFilter> _filters;

	@Setter(AccessLevel.PUBLIC)
	private boolean _isNot;


	public EtsFilter(FilterCompiler.LogicalOperation op, String property, Object value) {
		_operation = op;
		_property = property;
		_value = value;
		_filters = new ArrayList<>();
		_filters.add(this);
	}

	public EtsFilter(List<EtsFilter> filters) {
		_operation = null;
		_property = null;
		_value = null;
		_filters = filters;
	}

	public static EtsFilter eq(String property, Object value) {
		return new EtsFilter(FilterCompiler.LogicalOperation.EQ, property, value);
	}

	@Override
	public String toString() {
		return (_isNot ? "not " : "") + _property + " " + _operation + " " + _value;
	}

	public void negate() {
		setNot(!_isNot);
	}
}
