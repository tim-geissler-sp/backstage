/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.rest;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.sailpoint.atlas.security.RequireRight;
import com.sailpoint.iris.client.SimpleTopicDescriptor;
import com.sailpoint.iris.client.TopicDescriptor;
import com.sailpoint.iris.client.TopicScope;
import com.sailpoint.notification.userpreferences.dto.UserPreferences;
import com.sailpoint.notification.userpreferences.service.UserPreferencesDebugService;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * Debug rest service during User Preferences phase.
 */
@Path("debug")
public class NotificationUserPreferencesDebugResource {

	@Inject
	@VisibleForTesting
	UserPreferencesDebugService _userPreferencesDebugService;

	@POST
	@Path("publish/event/{topicName}/{eventType}")
	@Produces(MediaType.TEXT_PLAIN)
	@RequireRight("idn:notification-debug:create")
	public String publishKafkaEvent(@PathParam("topicName") String topicName,
									@PathParam("eventType") String eventType,
									String eventContext) {
		TopicDescriptor descriptor = new SimpleTopicDescriptor(TopicScope.POD, topicName.toLowerCase());
		return _userPreferencesDebugService.publishKafkaEvent(descriptor, eventType, eventContext);
	}

	@POST
	@Path("create")
	@Produces(MediaType.TEXT_PLAIN)
	@RequireRight("idn:notification-debug:create")
	public String createDebugUserPreference() {
		return _userPreferencesDebugService.createDebugUserPreference();
	}

	@GET
	@Path("{tenant}")
	@Produces(MediaType.APPLICATION_JSON)
	@RequireRight("idn:notification-debug:read")
	public List<UserPreferences> listDebugUserPreferences(@PathParam("tenant") String tenant) {
		return _userPreferencesDebugService.list(tenant);
	}

}
