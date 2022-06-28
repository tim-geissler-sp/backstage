/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web;

import com.sailpoint.atlas.api.common.filters.FilterBuilder;
import com.sailpoint.atlas.api.common.filters.ListSorter;
import com.sailpoint.atlas.boot.api.common.AtlasBootBaseV3Controller;
import com.sailpoint.atlas.util.ReflectionUtil;
import com.sailpoint.cloud.api.client.model.BaseDto;
import org.springframework.http.ResponseEntity;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base controller for ETS that includes definition for filters, query params and custom http response
 */
public abstract class EtsBaseController<T extends BaseDto> extends AtlasBootBaseV3Controller<EtsFilter, EtsQueryOptions> {

	@Override
	protected EtsQueryOptions constructQueryOptions(int offset, int limit, EtsFilter etsFilter, List<ListSorter> list) {
		return new EtsQueryOptions(offset, limit, etsFilter, list);
	}

	@Override
	protected FilterBuilder<EtsFilter> getFilterBuilder() {
		return new EtsFilterBuilder();
	}

	/**
	 * Get an OK response with the list of results based on specified query options.
	 * Optionally add X-Total-Count header, if requested.
	 *
	 * @param list           the list of base dto
	 * @param queryOptions   ths query options
	 * @param comparatorMap  the mapping between field name of the dto and the comparator of that field
	 * @param dtoPropertyMap the mapping between the field name and the actual property name of that field
	 * @return {@link ResponseEntity}
	 */
	public ResponseEntity<List<T>> okResponse(List<T> list,
											  EtsQueryOptions queryOptions,
											  Map<String, Comparator<T>> comparatorMap,
											  Map<String, String> dtoPropertyMap) {
		return okResponse(list.stream(), queryOptions, comparatorMap, dtoPropertyMap);
	}

	/**
	 * Get an OK response with the list of results based on specified query options
	 * Optionally add X-Total-Count header, if requested.
	 *
	 * @param stream         the stream of elements
	 * @param queryOptions   the query options
	 * @param comparatorMap  the mapping between field name of the dto and the comparator of that field
	 * @param dtoPropertyMap the mapping between the field name and the actual property name of that field
	 * @return {@link ResponseEntity}
	 */
	protected ResponseEntity<List<T>> okResponse(Stream<T> stream,
												 EtsQueryOptions queryOptions,
												 Map<String, Comparator<T>> comparatorMap,
												 Map<String, String> dtoPropertyMap) {
		List<T> queryResult = stream
			.sorted(getComparator(queryOptions.getSorterList(), comparatorMap))
			.filter(t -> queryOptions.getFilter() == null || matchFilter(t, queryOptions.getFilter(), dtoPropertyMap))
			.collect(Collectors.toList());

		final int totalCount = queryResult.size();

		final List<T> page = queryResult
			.stream()
			.skip(queryOptions.getOffset())
			.limit(queryOptions.getLimit())
			.collect(Collectors.toList());

		return isCountHeaderRequested() ? okResponse(page, totalCount) : ResponseEntity.ok(page);
	}

	/**
	 * Build an OK response of specified list with X-Total-Count header if requested.
	 *
	 * @param list list of dto
	 * @return {@link ResponseEntity}
	 */
	public ResponseEntity<List<T>> okResponse(List<T> list) {
		if (isCountHeaderRequested()) {
			return okResponse(list, list.size());
		} else {
			return ResponseEntity.ok(list);
		}
	}

	/**
	 * Build comparator chain from the sorter list to sort the dto list
	 *
	 * @param sorterList    the sorter list parsed from http query params
	 * @param comparatorMap the mapping between field name of the dto and the comparator of that field
	 * @return the comparator chain that describes the criteria to sort the list
	 */
	protected Comparator<T> getComparator(List<ListSorter> sorterList, Map<String, Comparator<T>> comparatorMap) {

		Comparator<T> comparator = null;
		for (int i = 0; i < sorterList.size(); i++) {
			ListSorter listSorter = sorterList.get(i);
			Comparator<T> next = comparatorMap.get(listSorter.getProperty());
			if (!listSorter.isAscending()) {
				next = next.reversed();
			}
			comparator = i == 0 ? next : comparator.thenComparing(next);
		}

		return comparator == null ? (c1, c2) -> 0 : comparator;
	}

	/**
	 * Compare the dto object with the filter by checking if the object matches the criteria defined in the filter
	 *
	 * @param t            the dto that is being compared with the filter
	 * @param filter       the filter parsed from http query params
	 * @param fieldNameMap the mapping between the field name and the actual property name of that field
	 * @return true if the dto matches the filter criteria
	 */
	private boolean matchFilter(T t, EtsFilter filter, Map<String, String> fieldNameMap) {
		for (EtsFilter f : filter.getFilters()) {
			Object dtoValue = ReflectionUtil.getPrivateField(f.getValue().getClass(), t, fieldNameMap.get(f.getProperty()));

			if (!dtoValue.equals(f.getValue())) {
				return false;
			}
		}
		return true;
	}
}
