/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web;

import com.sailpoint.atlas.api.common.filters.ListSorter;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

/**
 * Http query options for Trigger Service  V3 compliance
 */
@Getter
@AllArgsConstructor
public class EtsQueryOptions implements Serializable {

	private final Integer _offset;
	private final Integer _limit;
	private final EtsFilter _filter;
	private final List<ListSorter> _sorterList;
}
