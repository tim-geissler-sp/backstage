/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.context.rest.resource;

import com.google.inject.Inject;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.security.RequireRight;
import com.sailpoint.iris.client.SimpleTopicDescriptor;
import com.sailpoint.iris.client.TopicDescriptor;
import com.sailpoint.iris.client.TopicScope;
import com.sailpoint.notification.context.common.model.GlobalContext;
import com.sailpoint.notification.context.common.repository.GlobalContextRepository;
import com.sailpoint.notification.context.service.GlobalContextDebugService;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Notification template - Debug resource.
 */
@Path("debug")
public class GlobalContextDebugResource {

	private static final String DEBUG_ENTRY = "debug_entry";

	@Inject
	GlobalContextDebugService _globalContextDebugService;

	@Inject
	GlobalContextRepository _globalContextRepository;

	@POST
	@Path("publish/event/{topicName}/{eventType}")
	@Produces(MediaType.TEXT_PLAIN)
	@RequireRight("idn:notification-debug:create")
	public String publishKafkaEvent(@PathParam("topicName") String topicName,
									@PathParam("eventType") String eventType,
									String eventContext) {
		TopicDescriptor descriptor = new SimpleTopicDescriptor(TopicScope.POD, topicName.toLowerCase());
		return _globalContextDebugService.publishKafkaEvent(descriptor, eventType, eventContext);
	}

	@GET
	@Path("publish/entry")
	@Produces(MediaType.TEXT_PLAIN)
	@RequireRight("idn:notification-debug:create")
	public String publishEntry() {
		final RequestContext requestContext = RequestContext.ensureGet();
		final String tenant = requestContext.getOrg();
		final String testValue = "test-" + UUID.randomUUID();

		Optional<GlobalContext> maybeGlobalContext = _globalContextRepository.findOneByTenant(tenant);
		GlobalContext globalContext = maybeGlobalContext.orElse(new GlobalContext(tenant));
		Map<String, Object> attributes = Optional.ofNullable(globalContext.getAttributes()).orElse(new HashMap<>());

		attributes.put(DEBUG_ENTRY, testValue);
		globalContext.setAttributes(attributes);

		_globalContextRepository.save(globalContext);
		return testValue;
	}
}
