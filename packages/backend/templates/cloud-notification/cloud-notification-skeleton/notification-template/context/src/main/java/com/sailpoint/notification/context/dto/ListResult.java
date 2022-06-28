/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.context.dto;

import java.util.List;

/**
 * DTO for standardized REST resources that return a List of objects.
 */
public class ListResult<T> {

	private List<T> _items;

	private int _count;

	public ListResult(List<T> items, int count) {
		_items = items;
		_count = count;
	}

	public ListResult(List<T> items) {
		_items = items;
		_count = items.size();
	}

	public List<T> getItems() {
		return _items;
	}

	public void setItems(List<T> items) {
		_items = items;
	}

	public int getCount() {
		return _count;
	}

	public void setCount(int count) {
		_count = count;
	}

}
