/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.rest;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.security.RequireRight;
import com.sailpoint.notification.userpreferences.service.OrgLifecycleDebugService;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Debug rest service during User Preferences phase.
 */
@Path("debug")
public class OrgLifecycleDebugResource {

	@Inject
	@VisibleForTesting
	OrgLifecycleDebugService _orgLifecycleDebugService;

	@POST
	@Path("publish/event/{topicName}/{eventType}")
	@Produces(MediaType.TEXT_PLAIN)
	@RequireRight("idn:notification-debug:create")
	public String publishKafkaEvent(@PathParam("topicName") String topicName,
									@PathParam("eventType") String eventType,
									String eventContext) {
		return _orgLifecycleDebugService.publishKafkaEvent(
				IdnTopic.valueOf(topicName),
				eventType,
				eventContext);
	}

}
