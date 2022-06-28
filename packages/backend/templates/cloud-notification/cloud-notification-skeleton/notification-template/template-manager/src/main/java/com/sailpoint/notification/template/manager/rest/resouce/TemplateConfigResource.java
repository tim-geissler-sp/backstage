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
import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.atlas.security.RequireRight;
import com.sailpoint.cloud.api.client.model.errors.ApiExceptionBuilder;
import com.sailpoint.notification.api.event.dto.NotificationMedium;
import com.sailpoint.notification.sender.common.rest.NotificationFilter;
import com.sailpoint.notification.sender.common.rest.NotificationQueryOptions;
import com.sailpoint.notification.template.common.manager.TemplateRepositoryManager;
import com.sailpoint.notification.template.common.model.NotificationTemplate;
import com.sailpoint.notification.template.common.model.TemplateMediumDto;
import com.sailpoint.notification.template.common.model.version.TemplateVersion;
import com.sailpoint.notification.template.common.model.version.TemplateVersionInfo;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateBulkDeleteDto;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateDto;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateDtoDefault;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateDtoMapper;
import com.sailpoint.notification.template.manager.rest.service.TemplateTranslationService;
import com.sailpoint.utilities.StringUtil;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.reverseOrder;

/**
 * REST service for provide CRUD API for manage templates.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TemplateConfigResource extends BaseTemplateV3Resource<TemplateDto> {

	@Inject
	private TemplateRepositoryManager _templateRepositoryManager;

	@Inject
	private TemplateTranslationService _templateTranslateService;

	@Inject
	private FeatureFlagService _featureFlagService;
	//TODO: FF for control HTML sanitize, can be removed once confirmed working OK in production.
	private final static String HERMES_TEMPLATES_HTML_SANITIZE_ENABLED = "HERMES_TEMPLATES_HTML_SANITIZE_ENABLED";
	//TODO: FF for control slack persistent in DynamoDB. For now we do not allow customized slack templates.
	public final static String HERMES_TEMPLATES_SLACK_PERSISTENT_ENABLED = "HERMES_TEMPLATES_SLACK_PERSISTENT_ENABLED";

	//TODO: FF for control teams persistent in DynamoDB. For now we do not allow customized teams templates.
	public final static String HERMES_TEMPLATES_TEAMS_PERSISTENT_ENABLED = "HERMES_TEMPLATES_TEAMS_PERSISTENT_ENABLED";


	private static final Set<String> QUERYABLE_FIELDS = Sets.newHashSet( "key", "medium", "locale", "name", "description");

	private static final Set<String> SORTABLE_FIELDS = Sets.newHashSet( "key","name", "medium");

	private static final Map<String, Comparator<TemplateDto>> COMPARATORS_MAP = ImmutableMap.of(
			"key", Comparator.comparing(TemplateDto::getKey),
			"medium", Comparator.comparing(TemplateDto::getMedium),
			"name", Comparator.comparing(TemplateDto::getName));


	@Override
	protected Set<String> getQueryableFields() {
		return QUERYABLE_FIELDS;
	}

	@Override
	protected Set<String> getSortableFields() {
		return SORTABLE_FIELDS;
	}

	@GET
	@RequireRight("idn:notification-templates:read")
	public Response listTemplates(@HeaderParam(value = "Accept-Language") String localeString) {
		final NotificationQueryOptions queryOptions = getQueryOptions();
		ensureValidOperation(queryOptions);
		return getResponse(queryOptions, localeString);
	}

	@GET
	@Path("{id}")
	@RequireRight("idn:notification-templates:read")
	public Response getTemplate(@HeaderParam(value = "Accept-Language") String localeString, @PathParam("id") String templateId) {
		RequestContext rc = RequestContext.ensureGet();
		TemplateVersion version = _templateRepositoryManager.getOneByIdAndTenant(rc.ensureOrg(), templateId);
		if(version == null) {
			return Response.status(Response.Status.NOT_FOUND)
					.build();
		}
		TemplateDto templateDto= TemplateDtoMapper.toTemplateDTO(version, isNeedsToBeSanitizer());
		if(isEmailTemplate(templateDto)){
			templateDto = _templateTranslateService.getCustomTemplateTranslation(templateDto, localeString);
		}
		return okResponse(templateDto);
	}

	@POST
	@RequireRight("idn:notification-templates:create")
	public Response saveTemplate(TemplateDto template) {

		//check for required fields
		if(StringUtil.isNullOrEmpty(template.getKey()) || template.getLocale() == null || template.getMedium() == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.build();
		}
		//check for medium type and FF
		if(!isSupportPersistent(template)) {
			return Response.status(Response.Status.BAD_REQUEST)
					.build();
		}

		RequestContext rc = RequestContext.ensureGet();

		//verify default template with the same name exist
		if(_templateRepositoryManager.getDefaultRepository().findOneByTenantAndKey(rc.getOrg(), template.getKey()) == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.build();
		}

		NotificationTemplate notificationTemplate = TemplateDtoMapper.toNotificationTemplate(rc.getOrg(), template);

		TemplateVersionInfo versionInfo = new TemplateVersionInfo();
		versionInfo.setUpdatedBy(getUserInfo(rc));

		TemplateVersion version = _templateRepositoryManager.save(notificationTemplate, versionInfo);
		TemplateDto result = TemplateDtoMapper.toTemplateDTO(version, isNeedsToBeSanitizer());
		result.setModified(result.getCreated());
		return createdResponse(result);
	}

	@POST
	@Path("bulk-delete")
	@RequireRight("idn:notification-templates:delete")
	public Response bulkDeleteTemplate(List<TemplateBulkDeleteDto> bulkDeleteDots) {

		if (bulkDeleteDots == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.build();
		}
		if (bulkDeleteDots.size() == 0) {
			return Response.status(Response.Status.BAD_REQUEST)
					.build();
		}

		List<Map<String, String>> batchDelete = new ArrayList<>();

		for (TemplateBulkDeleteDto b : bulkDeleteDots) {
			Map<String, String> values = new HashMap<>();

			if (StringUtil.isNotNullOrEmpty(b.getKey())
					&& StringUtil.isNotNullOrEmpty(b.getLocale())
					&& b.getMedium() != null) {
				values.put("key", b.getKey());
				values.put("medium", b.getMedium().name());
				values.put("locale", b.getLocale());
			} else if (StringUtil.isNotNullOrEmpty(b.getKey())) {
				values.put("key", b.getKey());
			} else {
				new ApiExceptionBuilder()
						.cause(new IllegalArgumentException("You must provide all required bulk delete parameters"))
						.badRequest()
						.buildAndThrow();
			}
			batchDelete.add(values);
		}

		if (deleteTemplate(batchDelete)) {
			return Response.status(Response.Status.NO_CONTENT)
					.build();
		} else {
			return Response.status(Response.Status.BAD_REQUEST)
					.build();
		}

	}

	@Override
	protected Comparator<TemplateDto> getComparator(final List<ListSorter> sorterList) {
		Comparator<TemplateDto> comparator = Comparator.comparing(TemplateDto::getKey);
		if(sorterList.size() > 0) {
			comparator = COMPARATORS_MAP.get(sorterList.get(0).getProperty());
			if (!sorterList.get(0).isAscending()) {
					comparator = reverseOrder(comparator);
			}
			for(int i=1; i< sorterList.size(); i++) {
				ListSorter ls = sorterList.get(i);
				Comparator<TemplateDto> temp = COMPARATORS_MAP.get(ls.getProperty());
				if(!ls.isAscending()) {
					temp = reverseOrder(temp);
				}
				comparator = comparator.thenComparing(temp);
			}
		}
		return comparator;
	}

	/**
	 * Nulk delete custom templates by name, medium, locale.
	 * @param batchDelete  batch of values.
	 * @return true if successful.
	 */
	private boolean deleteTemplate(List<Map<String, String>> batchDelete) {
		RequestContext rc = RequestContext.ensureGet();
		return _templateRepositoryManager.bulkDeleteAllByTenantAndKeyAndMediumAndLocale(rc.getOrg(),
				batchDelete);
	}

	/**
	 * Get templates versions based on provided query options.
	 * @param queryOptions query options
	 * @return list of templates.
	 */
	private List<TemplateVersion> getTemplates(NotificationQueryOptions queryOptions) {
		RequestContext rc = RequestContext.ensureGet();
		List<TemplateVersion> templates;
		if(queryOptions.getFilter().isPresent()) {
			FilterParameters params = getFilterParameters(queryOptions.getFilter().get());

			if(params.keys != null) {
				//There can be more than one key in case of IN filter.
				//If there's just one key, we will use getLatestByTenantAndKeyAndMediumAndLocale
				//Else we will getAllLatestByTenant and filter.

				if(params.keys.size() == 1) {
					templates = _templateRepositoryManager.getLatestByTenantAndKeyAndMediumAndLocale(rc.getOrg(),
							params.keys.stream().findFirst().get(), params.medium, params.locale);
				} else {
					templates = _templateRepositoryManager.getAllLatestByTenant(rc.getOrg())
							.stream()
							.filter(t ->
									params.keys.contains(t.getNotificationTemplate().getKey())
											&& (params.medium == null || params.medium.equals(t.getNotificationTemplate().getMedium()))
											&& (params.locale == null || params.locale.equals(t.getNotificationTemplate().getLocale()))
							)
							.collect(Collectors.toList());
				}
			} else {
				if(params.name != null || params.description != null){
					templates = _templateRepositoryManager.getAllLatestByTenant(rc.getOrg());
					return templates;
				}
				//If key isn't provided in the filter, it is assumed medium and locale are.
				templates = _templateRepositoryManager.getAllLatestByTenantAndMediumAndLocale(rc.getOrg(),
						params.medium, params.locale);
			}
		} else {
			templates = _templateRepositoryManager.getAllLatestByTenant(rc.getOrg());
		}
		return templates;
	}

	/**
	 * Get Response from repo based on query parameters.
	 * @param queryOptions query options from REST request.
	 * @param localeString Accepted language from REST request
	 * @return response.
	 */
	private Response getResponse(final NotificationQueryOptions queryOptions, String localeString) {
		List<TemplateDto> result = getTemplates(queryOptions).stream()
				.map(v->TemplateDtoMapper.toTemplateDTO(v, isNeedsToBeSanitizer()))
				.map(t -> {
					//translate email template only
					if (isEmailTemplate(t)) {
						return _templateTranslateService.getCustomTemplateTranslation(t, localeString);
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

	private boolean isNeedsToBeSanitizer() {
		return _featureFlagService.getBoolean(HERMES_TEMPLATES_HTML_SANITIZE_ENABLED, false);
	}

	private boolean isSupportPersistent(TemplateDto template) {
		if( template.getMedium() != null )
		{
			if (TemplateMediumDto.SLACK.equals(template.getMedium()))
			{
				return _featureFlagService.getBoolean(HERMES_TEMPLATES_SLACK_PERSISTENT_ENABLED, false);
			} else if(TemplateMediumDto.TEAMS.equals(template.getMedium()))
			{
				return _featureFlagService.getBoolean(HERMES_TEMPLATES_TEAMS_PERSISTENT_ENABLED, false);
			}
			return true;
		}
		return false;

	}

	/**
	 * Evaluated if template is email template
	 * @param template template.
	 * @return true if is email template.
	 */
	private boolean isEmailTemplate(TemplateDto template) {
		return template.getMedium().toString().equalsIgnoreCase(NotificationMedium.EMAIL.toString());
	}

	private List<TemplateDto> filterTemplates(List<TemplateDto> templates, NotificationQueryOptions queryOptions){
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
