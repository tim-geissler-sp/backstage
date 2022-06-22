/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.manager.rest.resouce;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.api.common.filters.FilterCompiler;
import com.sailpoint.atlas.api.common.filters.ListSorter;
import com.sailpoint.atlas.security.RequireRight;
import com.sailpoint.notification.api.event.dto.NotificationMedium;
import com.sailpoint.notification.sender.common.rest.NotificationFilter;
import com.sailpoint.notification.sender.common.rest.NotificationQueryOptions;
import com.sailpoint.notification.template.common.model.NotificationTemplate;
import com.sailpoint.notification.template.common.repository.TemplateRepositoryDefault;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateDtoDefault;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateDtoMapper;
import com.sailpoint.notification.template.manager.rest.service.TemplateTranslationService;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.reverseOrder;

/**
 * REST service for provide read only API for access default templates.
 */
@Path("/")
public class TemplateDefaultResource extends BaseTemplateV3Resource<TemplateDtoDefault> {

	@Inject
	private TemplateRepositoryDefault _templateRepositoryDefault;

	@Inject
	private TemplateTranslationService _templateTranslateService;

	private static final Set<String> QUERYABLE_FIELDS = Sets.newHashSet( "key", "medium", "locale", "name", "description");

	private static final Set<String> SORTABLE_FIELDS = Sets.newHashSet( "key", "medium", "locale", "name");

	private static final Map<String, Comparator<TemplateDtoDefault>> COMPARATORS_MAP = ImmutableMap.of(
			"key", Comparator.comparing(TemplateDtoDefault::getKey),
			"medium", Comparator.comparing(TemplateDtoDefault::getMedium),
			"locale", Comparator.comparing(t->t.getLocale().toLanguageTag()),
			"name", Comparator.comparing(TemplateDtoDefault::getName));

	@Override
	protected Set<String> getQueryableFields() {
		return QUERYABLE_FIELDS;
	}

	@Override
	protected Set<String> getSortableFields() {
		return SORTABLE_FIELDS;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@RequireRight("idn:notification-template-defaults:read")
	public Response listAllTemplates(@HeaderParam(value = "Accept-Language") String localeString) {
		final NotificationQueryOptions queryOptions = getQueryOptions();
		ensureValidOperation(queryOptions);
		return getResponse(queryOptions, localeString);
	}

	@Override
	protected Comparator<TemplateDtoDefault> getComparator(final List<ListSorter> sorterList) {
		//default comparator
		Comparator<TemplateDtoDefault> comparator = Comparator.comparing(TemplateDtoDefault::getKey);
		if(sorterList.size() > 0) {
			comparator = COMPARATORS_MAP.get(sorterList.get(0).getProperty());
			if(!sorterList.get(0).isAscending()) {
				comparator = reverseOrder(comparator);
			}
			for(int i=1; i< sorterList.size(); i++) {
				ListSorter ls = sorterList.get(i);
				Comparator<TemplateDtoDefault> temp = COMPARATORS_MAP.get(ls.getProperty());
				if(!ls.isAscending()) {
					temp = reverseOrder(temp);
				}
				comparator = comparator.thenComparing(temp);
			}
		}
		return comparator;
	}

	/**
	 * Get Response from default repo based on query parameters.
	 * @param queryOptions query options from REST request.
	 * @param localeString Accepted language from REST request
	 * @return response.
	 */
	private Response getResponse(final NotificationQueryOptions queryOptions, String localeString) {
		List<TemplateDtoDefault> result = _templateRepositoryDefault.findAll()
			.stream()
			.filter(t -> queryOptions.getFilter()
					.map(f->(isTemplate(t, f) && isTenant(t)))
					.orElse(isTenant(t)))
			.map(TemplateDtoMapper::toTemplateDTODefault)
				.map(t -> {
					//translate email template only
					if (t.getMedium().toString().equalsIgnoreCase(NotificationMedium.EMAIL.toString())) {
						return _templateTranslateService.getDefaultTemplateTranslation(t, localeString);
					}
					return t;
				})
			.collect(Collectors.toList());
		//filter template if filters is either co/sw
		if (queryOptions.getFilter().isPresent()) {
			if(queryOptions.getFilter().get().getOperation() == FilterCompiler.LogicalOperation.LIKE) {
				result = filterTemplates(result, queryOptions);
			}
		}

		return okResponse(result, queryOptions);
	}

	/**
	 * Evaluated if template equals provided filter
	 * @param template template.
	 * @param filter filter.
	 * @return true if equals.
	 */
	private boolean isTemplate(NotificationTemplate template, NotificationFilter filter) {
		FilterParameters param = getFilterParameters(filter);
		return (param.keys == null || param.keys.contains(template.getKey()))
				&& (param.medium == null || template.getMedium().equalsIgnoreCase(param.medium))
				&& (param.locale == null || template.getLocale().equals(param.locale));
	}

	private boolean isTenant(NotificationTemplate template) {
		RequestContext rc = RequestContext.ensureGet();
		return (rc.getOrg().equals(template.getTenant()) || template.getTenant() == null);
	}

	private List<TemplateDtoDefault> filterTemplates(List<TemplateDtoDefault> templates, NotificationQueryOptions queryOptions){
		NotificationFilter filter = queryOptions.getFilter().get();
		String filterProperty = filter.getProperty();
		String filterValue = filter.getValue().toString();

		//support operation sw (start with)
		if (filter.getMode() == FilterCompiler.MatchMode.START) {
			templates = templates.stream()
					.filter(template -> {
						if (filterProperty.equals("name")){
							return template.getName().startsWith(filterValue);
						} else if (filterProperty.equals("description")) {
							return template.getDescription().startsWith(filterValue);
						}
						return true;
					})
					.collect(Collectors.toList());
		// support operation co (contains)
		}else if(filter.getMode() == FilterCompiler.MatchMode.ANYWHERE) {
			templates = templates.stream()
					.filter(template -> {
						if (filterProperty.equals("name")){
							return template.getName().contains(filterValue);
						} else if (filterProperty.equals("description")) {
							return template.getDescription().contains(filterValue);
						}
						return true;
					})
					.collect(Collectors.toList());
		}
		return templates;
	}
}
