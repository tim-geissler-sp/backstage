/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.rest;

import com.sailpoint.atlas.api.common.filters.ListSorter;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * NotificationQueryOption implementations for support V3 API in hermes.
 */
public class NotificationQueryOptions implements Serializable {

	private final Integer _offset;
	private final Integer _limit;
	private final NotificationFilter _filter;
	private final List<ListSorter> _sorterList;

	public NotificationQueryOptions(int offset, int limit, NotificationFilter filter, List<ListSorter> sorterList) {
		_offset = offset;
		_limit = limit;
		_filter = filter;
		_sorterList = sorterList;
	}

	public Integer getOffset() {
		return _offset;
	}

	public Integer getLimit() {
		return _limit;
	}

	public Optional<NotificationFilter> getFilter() {
		return Optional.ofNullable(_filter);
	}

	public List<ListSorter> getSorterList() {
		return _sorterList;
	}

	public void addSorter(ListSorter listSorter) {
		_sorterList.add(listSorter);
	}

}
