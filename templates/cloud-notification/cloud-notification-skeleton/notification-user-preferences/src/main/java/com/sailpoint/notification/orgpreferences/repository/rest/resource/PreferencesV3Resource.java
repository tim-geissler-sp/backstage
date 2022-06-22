/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.orgpreferences.repository.rest.resource;

import com.google.inject.Inject;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.api.common.AtlasBaseV3Resource;
import com.sailpoint.atlas.api.common.filters.FilterBuilder;
import com.sailpoint.atlas.security.RequireRight;
import com.sailpoint.cloud.api.client.model.V3Resource;
import com.sailpoint.cloud.api.client.model.errors.ApiExceptionBuilder;
import com.sailpoint.notification.api.event.dto.NotificationMedium;
import com.sailpoint.notification.interest.matcher.interest.Interest;
import com.sailpoint.notification.interest.matcher.repository.InterestRepository;
import com.sailpoint.notification.orgpreferences.repository.TenantPreferencesRepository;
import com.sailpoint.notification.orgpreferences.repository.dto.PreferencesDto;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@V3Resource
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PreferencesV3Resource extends AtlasBaseV3Resource {

	@Inject
	InterestRepository _interestRepository;

	@Inject
	TenantPreferencesRepository _tenantPreferencesRepository;

	@Override
	protected Object constructQueryOptions(int i, int i1, Object o, List list) {
		return null;
	}

	@Override
	protected FilterBuilder getFilterBuilder() {
		return null;
	}

	/**
	 * Constraints:
	 * If the key doesn't exist return 404
	 *
	 * @param key the notification key
	 * @return PreferencesDto
	 */
	@GET
	@Path("{key}")
	@RequireRight("idn:notification-preferences:read")
	public Response getPreferences(@PathParam("key") String key) {
		//Find the interest for the given key. If interest does not exist
		//it can be assumed that preferences don't either
		Interest interest = _interestRepository.getInterests().stream()
				.filter(i -> i.getNotificationKey().equals(key))
				.findFirst()
				.orElseThrow(() -> new ApiExceptionBuilder().notFound().build());

		//Look for tenant preferences for the given key
		RequestContext rc = RequestContext.ensureGet();
		PreferencesDto preferencesDto  = _tenantPreferencesRepository.findOneForTenantAndKey(rc.getOrg(), key);

		//If no tenant preferences defer to interest
		if(preferencesDto == null) {
			preferencesDto = new PreferencesDto();
			preferencesDto.setKey(key);
			if (interest.isEnabled()) {
				List<NotificationMedium> interestCategories = Arrays.stream(interest.getCategoryName().split("\\s*,\\s*"))
						.map(v->NotificationMedium.valueOf(v.trim().toUpperCase() ))
						.collect(Collectors.toList());
				preferencesDto.setMediums(interestCategories);
			}
		}
		return okResponse(preferencesDto);
	}

	/**
	 * Constraints:
	 * If the key doesn't exist return 404
	 * If the key in the path and payload don't match return bad request
	 * Will overwrite the preferences for the given notification key.
	 *
	 * @param key the Notification Key
	 * @param preferencesDto the PreferencesDto to save
	 * @return PreferencesDto that was saved
	 */
	@PUT
	@Path("{key}")
	@RequireRight("idn:notification-preferences:create")
	public Response savePreferences(@PathParam("key") String key, PreferencesDto preferencesDto) {
		//Find the interest for the given key. If interest does not exist
		//it can be assumed that preferences don't either
		Interest interest = _interestRepository.getInterests().stream()
				.filter(i -> i.getNotificationKey().equals(key))
				.findFirst()
				.orElseThrow(() -> new ApiExceptionBuilder().notFound().build());

		//The key in the Dto is not necessary but if it is provided and doesn't match the key in the URI,
		// then a 400 will be thrown.
		if (preferencesDto.getKey() != null && !key.equals(preferencesDto.getKey())) {
			new ApiExceptionBuilder().badRequest().buildAndThrow();
		} else {
			preferencesDto.setKey(key);
		}

		RequestContext rc = RequestContext.ensureGet();
		_tenantPreferencesRepository.save(rc.getOrg(), preferencesDto);
		return okResponse(preferencesDto);
	}
}