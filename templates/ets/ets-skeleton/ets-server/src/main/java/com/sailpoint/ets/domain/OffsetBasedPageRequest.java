/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.Serializable;

/**
 * Spring Data Jpa offset-based pagination & sort option
 */
@EqualsAndHashCode
@ToString
public class OffsetBasedPageRequest implements Pageable, Serializable {

	private static final long serialVersionUID = 3993905481182153239L;

	private final int _limit;
	private final int _offset;
	private final Sort _sort;

	/**
	 * Creates a new {@link OffsetBasedPageRequest} with sort parameters applied.
	 *
	 * @param offset zero-based offset.
	 * @param limit  the size of the elements to be returned, i.e. page size.
	 * @param sort   sort option.
	 */
	public OffsetBasedPageRequest(int offset, int limit, Sort sort) {
		if (offset < 0) {
			throw new IllegalArgumentException("Offset index must not be less than zero!");
		}

		if (limit < 1) {
			throw new IllegalArgumentException("Limit must not be less than one!");
		}

		this._limit = limit;
		this._offset = offset;
		this._sort = sort;
	}

	/**
	 * Creates a new {@link OffsetBasedPageRequest} with no sorting.
	 *
	 * @param offset zero-based offset.
	 * @param limit  the size of the elements to be returned, i.e. page size.
	 */
	public OffsetBasedPageRequest(int offset, int limit) {
		this(offset, limit, Sort.unsorted());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getPageNumber() {
		return _offset / _limit;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getPageSize() {
		return _limit;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getOffset() {
		return _offset;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Sort getSort() {
		return _sort;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Pageable next() {
		return new OffsetBasedPageRequest(_offset + _limit, _limit, _sort);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Pageable previousOrFirst() {
		return hasPrevious() ? previous() : first();
	}

	private Pageable previous() {
		return new OffsetBasedPageRequest(_offset - _limit, _limit, _sort);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Pageable first() {
		return new OffsetBasedPageRequest(0, _limit, _sort);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasPrevious() {
		return _offset > _limit;
	}
}
