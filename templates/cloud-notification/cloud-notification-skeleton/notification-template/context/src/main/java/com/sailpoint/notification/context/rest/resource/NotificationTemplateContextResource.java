/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.context.rest.resource;

import com.google.inject.Inject;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.api.common.ResponseHelper;
import com.sailpoint.atlas.security.RequireRight;
import com.sailpoint.cloud.api.client.model.V3Resource;
import com.sailpoint.cloud.api.client.model.errors.ApiExceptionBuilder;
import com.sailpoint.notification.context.common.model.GlobalContext;
import com.sailpoint.notification.context.common.repository.GlobalContextRepository;
import com.sailpoint.notification.context.common.util.GlobalContextMapper;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * Notification template - Debug resource.
 */
@Path("/v3/notification-template-context")
@Produces(MediaType.APPLICATION_JSON)
@V3Resource
public class NotificationTemplateContextResource implements ResponseHelper {

	@Inject
	GlobalContextRepository _globalContextRepository;

	@GET
	@RequireRight("idn:notification-templates:read")
	public Response get() {

		final String tenant = RequestContext.ensureGet().getOrg();
		Optional<GlobalContext> globalContextOptional = _globalContextRepository.findOneByTenant(tenant);

		if (!globalContextOptional.isPresent()) {
			new ApiExceptionBuilder()
					.notFound()
					.buildAndThrow();
		}

		return okResponse(GlobalContextMapper.toNotificationTemplateContextDto(globalContextOptional.get()));
	}
}
