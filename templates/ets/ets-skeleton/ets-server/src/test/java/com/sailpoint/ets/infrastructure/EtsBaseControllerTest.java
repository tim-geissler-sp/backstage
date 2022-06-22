/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure;

import com.google.common.collect.ImmutableMap;
import com.sailpoint.atlas.api.common.filters.FilterCompiler;
import com.sailpoint.atlas.api.common.filters.ListSorter;
import com.sailpoint.cloud.api.client.model.BaseDto;
import com.sailpoint.ets.infrastructure.web.EtsBaseController;
import com.sailpoint.ets.infrastructure.web.EtsFilter;
import com.sailpoint.ets.infrastructure.web.EtsFilterBuilder;
import com.sailpoint.ets.infrastructure.web.EtsQueryOptions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for EtsBaseController
 */
@RunWith(MockitoJUnitRunner.class)
public class EtsBaseControllerTest {

	private TestController _controller;
	private List<TestDto> _dtoList;

	@Before
	public void setUp() {
		_controller = new TestController();
		_dtoList = Arrays.asList(new TestDto("a"), new TestDto("b"), new TestDto("c"));
	}

	@Test
	public void testLimitAndOffset() {
		EtsQueryOptions queryOptions = new EtsQueryOptions(0, 2, null, Collections.emptyList());
		List<TestDto> dtoList = whenSortingAndFilteringWithQueryOption(queryOptions);

		assertEquals(dtoList.size(), 2);
		assertEquals(dtoList.get(0).getId(), "a");
		assertEquals(dtoList.get(1).getId(), "b");


		queryOptions = new EtsQueryOptions(1, 1, null, Collections.emptyList());
		dtoList = whenSortingAndFilteringWithQueryOption(queryOptions);

		assertEquals(dtoList.size(), 1);
		assertEquals(dtoList.get(0).getId(), "b");
	}

	@Test
	public void testSorter() {
		List<ListSorter> listSorters = Collections.singletonList(new ListSorter("id", false));
		List<TestDto> dtoList = whenSortingAndFilteringWithQueryOption(new EtsQueryOptions(0, 3, null, listSorters));

		assertEquals(dtoList.size(), 3);
		assertEquals(dtoList.get(0).getId(), "c");
		assertEquals(dtoList.get(1).getId(), "b");
		assertEquals(dtoList.get(2).getId(), "a");
	}

	@Test
	public void testFilters() {
		EtsFilter filter = new EtsFilter(FilterCompiler.LogicalOperation.EQ, "id", "c");
		EtsFilterBuilder builder = new EtsFilterBuilder();
		EtsQueryOptions queryOptions = new EtsQueryOptions(0, 3, builder.and(Collections.singletonList(filter)), Collections.emptyList());
		List<TestDto> dtoList = whenSortingAndFilteringWithQueryOption(queryOptions);

		assertEquals(dtoList.size(), 1);
		assertEquals(dtoList.get(0).getId(), "c");
	}

	@SuppressWarnings("unchecked")
	private List<TestDto> whenSortingAndFilteringWithQueryOption(EtsQueryOptions queryOptions) {
		Map<String, Comparator<TestDto>> _comparatorMap = ImmutableMap.of("id", Comparator.comparing(TestDto::getId));
		Map<String, String> _dtoPropertyMap = ImmutableMap.of("id", "_id");

		ResponseEntity response = _controller.okResponse(_dtoList, queryOptions, _comparatorMap, _dtoPropertyMap);
		return (List<TestDto>)response.getBody();
	}

	@Getter
	@AllArgsConstructor
	private class TestDto extends BaseDto {
		private String _id;
	}

	private class TestController extends EtsBaseController<TestDto> {
		@Override
		protected boolean isCountHeaderRequested() {
			return true;
		}
	}
}
