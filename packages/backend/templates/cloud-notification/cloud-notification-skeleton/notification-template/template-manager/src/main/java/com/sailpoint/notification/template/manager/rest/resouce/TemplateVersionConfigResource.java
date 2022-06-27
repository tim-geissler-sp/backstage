/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.manager.rest.resouce;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.api.common.filters.ListSorter;
import com.sailpoint.atlas.security.RequireRight;
import com.sailpoint.notification.sender.common.rest.NotificationQueryOptions;
import com.sailpoint.notification.template.common.manager.TemplateRepositoryManager;
import com.sailpoint.notification.template.common.model.version.TemplateVersion;
import com.sailpoint.notification.template.common.model.version.TemplateVersionUserInfo;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateDtoMapper;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateDtoVersion;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateVersionUserInfoDto;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST service for provide CRUD API for manage version for given template.
 */
@Path("/")
public class TemplateVersionConfigResource extends BaseTemplateV3Resource<TemplateDtoVersion> {

	private static final Set<String> QUERYABLE_FIELDS = Sets.newHashSet("versionId");

	@Inject
	private TemplateRepositoryManager _templateRepositoryManager;

	@Override
	protected Set<String> getQueryableFields() {
		return QUERYABLE_FIELDS;
	}

	@Override
	protected Set<String> getSortableFields() {
		return QUERYABLE_FIELDS;
	}

	@GET
	@Path("{key}/{medium}/{locale}")
	@Produces(MediaType.APPLICATION_JSON)
	@RequireRight("idn:notification-templates:read")
	public Response listVersionsTemplates(@PathParam("key") String key, @PathParam("medium") String medium, @PathParam("locale") String locale) {
		final NotificationQueryOptions queryOptions = getQueryOptions();
		ensureValidOperation(queryOptions);
		return getResponse(queryOptions, key, medium.toUpperCase(), locale);
	}

	@POST
	@Path("restore/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RequireRight("idn:notification-templates:update")
	public Response restoreVersion(@PathParam("id") String templateId,
								   TemplateVersionUserInfoDto user) {
		RequestContext rc = RequestContext.ensureGet();
		TemplateVersionUserInfo userInfo = new TemplateVersionUserInfo();
		if(user == null) {
			userInfo = getUserInfo(rc);
		} else {
			userInfo.setId(user.getId());
			userInfo.setName(user.getName());
		}

		TemplateVersion result = _templateRepositoryManager.restoreVersionByIdAndTenant(templateId, rc.ensureOrg(), userInfo);
		if(result == null) {
			return Response.status(Response.Status.NOT_FOUND)
					.build();
		}
		return Response.status(Response.Status.CREATED)
				.entity(TemplateDtoMapper.toTemplateDTOVersion(result))
				.build();
	}

	@DELETE
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RequireRight("idn:notification-templates:delete")
	public Response deleteOneVersion(@PathParam("id") String templateId) {
		RequestContext rc = RequestContext.ensureGet();
		boolean result = _templateRepositoryManager.deleteOneByIdAndTenant(rc.ensureOrg(), templateId);
		if(!result) {
			return Response.status(Response.Status.NOT_FOUND)
					.build();
		}
		return Response.status(Response.Status.ACCEPTED)
				.build();
	}

	@Override
	protected Comparator<TemplateDtoVersion> getComparator(final List<ListSorter> sorterList) {
		Comparator<TemplateDtoVersion> comparator = Comparator.comparing(t -> t.getVersionInfo().getVersion());
		if(sorterList.size() > 0) {
			if (!sorterList.get(0).isAscending()) {
				comparator = comparator.reversed();
			}
		}
		return comparator;
	}

	/**
	 * Get Response from repo based on query parameters.
	 * @param queryOptions query options from REST request.
	 * @return response.
	 */
	private Response getResponse(final NotificationQueryOptions queryOptions, String key, String medium, String locale) {
		RequestContext rc = RequestContext.ensureGet();

		final String versionId = queryOptions.getFilter().isPresent() ? getFilterParameters(queryOptions.getFilter().get()).versionId : "all";

		List<TemplateDtoVersion> templates = _templateRepositoryManager
					.getAllVersions(rc.ensureOrg(), key, medium, Locale.forLanguageTag(locale))
					.stream()
					.filter(t->versionId.equals("all") || t.getVersionId().equals(versionId))
					.map(TemplateDtoMapper::toTemplateDTOVersion)
					.collect(Collectors.toList());

		return okResponse(templates, queryOptions);
	}
}
