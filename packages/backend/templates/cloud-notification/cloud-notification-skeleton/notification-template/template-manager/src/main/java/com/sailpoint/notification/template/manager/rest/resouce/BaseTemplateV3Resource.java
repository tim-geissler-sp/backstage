/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.manager.rest.resouce;

import com.google.inject.Inject;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.api.common.AtlasBaseV3Resource;
import com.sailpoint.atlas.api.common.filters.FilterBuilder;
import com.sailpoint.atlas.api.common.filters.FilterCompiler;
import com.sailpoint.atlas.api.common.filters.ListSorter;
import com.sailpoint.cloud.api.client.model.BaseDto;
import com.sailpoint.cloud.api.client.model.V3Resource;
import com.sailpoint.cloud.api.client.model.errors.ApiExceptionBuilder;
import com.sailpoint.notification.sender.common.rest.NotificationFilter;
import com.sailpoint.notification.sender.common.rest.NotificationFilterBuilder;
import com.sailpoint.notification.sender.common.rest.NotificationQueryOptions;
import com.sailpoint.notification.template.common.model.version.TemplateVersionUserInfo;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateDtoDefault;
import com.sailpoint.notification.userpreferences.dto.UserPreferences;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Base abstract class for V3 REST services manage templates.
 */
@V3Resource
public abstract class BaseTemplateV3Resource<T extends BaseDto> extends AtlasBaseV3Resource<NotificationFilter, NotificationQueryOptions> {

	@Inject
	private UserPreferencesRepository _userPreferencesRepository;

	protected int getMaxPageSize() {
		return MAX_PAGE_SIZE;
	}

	@Override
	protected NotificationQueryOptions constructQueryOptions(int offset, int limit, NotificationFilter filter, List<ListSorter> sorters) {
		return new NotificationQueryOptions(offset, limit, filter, sorters);
	}

	@Override
	protected FilterBuilder<NotificationFilter> getFilterBuilder() {
		return new NotificationFilterBuilder();
	}

	/**
	 * Get comparator based on provided Query Options
	 * @param sorterList Sorter List
	 * @return comparator
	 */
	protected abstract Comparator<T> getComparator(final List<ListSorter> sorterList);

	TemplateVersionUserInfo getUserInfo(RequestContext rc) {

		TemplateVersionUserInfo result =  new TemplateVersionUserInfo();

		Optional<String> identity = rc.getSecurityContext().getIdentity();

		if(identity.isPresent()) {
			UserPreferences user = _userPreferencesRepository.findByRecipientId(identity.get());
			if(user != null && user.getRecipient()!= null) {
				result.setId(user.getRecipient().getId());
				result.setName(user.getRecipient().getName());
			}
		}
		return result;
	}

	/**
	 * Sort, Limit response based on query parameters and provided comparator
	 * @param list list of BaseDto.
	 * @param queryOptions query options.
	 * @return Response.
	 */
	Response okResponse(List<T> list, final NotificationQueryOptions queryOptions) {
		//sort if needed
		if(queryOptions.getSorterList().size() > 0) {
			list.sort(getComparator(queryOptions.getSorterList()));
		}

		//filter out by limits
		int totalCount = list.size();

		list = list.stream()
				.skip(queryOptions.getOffset())
				.limit(queryOptions.getLimit())
				.collect(Collectors.toList());

		return okResponse(list, totalCount);
	}

	/**
	 * ensure valid operation
	 * @param queryOptions query options
	 */
	void ensureValidOperation(final NotificationQueryOptions queryOptions) {
		queryOptions.getFilter().ifPresent(f -> {
			if (f.getFilters() == null) {
				ensureOperation(f.getOperation());
			} else {
				f.getFilters().forEach(fs -> ensureOperation(fs.getOperation()));
			}
		});
	}

	/**
	 * Internal utility class for hold filter parameters.
	 */
	static class FilterParameters {
		Set<String> keys;
		String medium;
		Locale locale;
		String versionId;
		String name;
		String description;

		FilterParameters() {
			versionId = "all";
		}
	}

	/**
	 * Get FilterParameters from provided filter.
	 * @param filter notification filter.
	 * @return filter params.
 	 */
	FilterParameters getFilterParameters(NotificationFilter filter) {
		FilterParameters param = new FilterParameters();
		filter.getFilters().forEach(f-> {
			switch (f.getProperty()) {
				case "key":
					// Key can have a list value for IN filter
					if (f.getValueList() != null) {
						param.keys = new HashSet(f.getValueList());
					} else {
						param.keys = Collections.singleton((String)f.getValue());
					}
					break;
				case "medium":
					param.medium = f.getValue().toString().toUpperCase();
					break;
				case "locale":
					param.locale = Locale.forLanguageTag((String)f.getValue());
					break;
				case "versionId":
					param.versionId = (String)f.getValue();
					break;
				case "name":
					param.name = (String)f.getValue();
					break;
				case "description":
					param.description = (String)f.getValue();
					break;
			}
		});
		return param;
	}

	/**
	 * ensure operation is only EQ or IN, or LIKE which includes co and sw.
	 *
	 * @param op - logical operation
	 */
	private void ensureOperation(FilterCompiler.LogicalOperation op) {
		if (!(op == FilterCompiler.LogicalOperation.EQ ||
				op == FilterCompiler.LogicalOperation.IN || op == FilterCompiler.LogicalOperation.LIKE)) {
			new ApiExceptionBuilder()
					.cause(new IllegalArgumentException("Only EQ, IN, CO, and SW operations are supported"))
					.badRequest()
					.buildAndThrow();
		}
	}
}
