/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web;

import com.sailpoint.atlas.api.common.filters.FilterBuilder;
import com.sailpoint.atlas.api.common.filters.FilterCompiler;

import java.util.List;

/**
 * Http filter builder for Trigger Service V3 compliance
 */
public class EtsFilterBuilder implements FilterBuilder<EtsFilter> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EtsFilter and(List<EtsFilter> filters) {
		return new EtsFilter(filters);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EtsFilter or(List<EtsFilter> filters) {
		throw new IllegalArgumentException("OR is unsupported.");
	}

	/**
	 * {@inheritDoc}
	 * Note that this method will change the alter input parameter to be negated
	 */
	@Override
	public EtsFilter not(EtsFilter filter) {
		filter.negate();
		return filter;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EtsFilter newFilter(FilterCompiler.LogicalOperation op, String property, Object valueObject) {
		return new EtsFilter(op, property, valueObject);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EtsFilter newFilter(FilterCompiler.LogicalOperation op, String property, Object valueObject, FilterCompiler.MatchMode mode) {
		throw new IllegalArgumentException("Match modes are not supported.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EtsFilter ignoreCase(EtsFilter filter) {
		throw new IllegalArgumentException("Case sensitivity is not supported.");
	}
}
