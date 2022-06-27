/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.rest;

import com.google.inject.Inject;
import com.sailpoint.atlas.security.RequireRight;
import com.sailpoint.iris.client.SimpleTopicDescriptor;
import com.sailpoint.iris.client.TopicDescriptor;
import com.sailpoint.iris.client.TopicScope;
import com.sailpoint.notification.template.service.NotificationTemplateDebugService;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Notification template - Debug resource.
 */
@Path("debug")
public class NotificationTemplateDebugResource {

	@Inject
	NotificationTemplateDebugService _notificationTemplateDebugService;

	@POST
	@Path("publish/event/{topicName}/{eventType}")
	@Produces(MediaType.TEXT_PLAIN)
	@RequireRight("idn:notification-debug:create")
	public String publishKafkaEvent(@PathParam("topicName") String topicName,
									@PathParam("eventType") String eventType,
									String eventContext) {
		TopicDescriptor podDescriptor = new SimpleTopicDescriptor(TopicScope.POD, topicName.toLowerCase());
		return _notificationTemplateDebugService.publishKafkaEvent(podDescriptor, eventType, eventContext);
	}
}
