/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.interest.matcher.rest;

import com.google.inject.Inject;
import com.sailpoint.atlas.security.RequireRight;
import com.sailpoint.iris.client.SimpleTopicDescriptor;
import com.sailpoint.iris.client.TopicDescriptor;
import com.sailpoint.iris.client.TopicScope;
import com.sailpoint.notification.interest.matcher.service.InterestMatcherDebugService;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Debug rest service during Interest Matcher phase.
 */
@Path("debug")
public class NotificationInterestMatcherDebugResource {

	@Inject
	InterestMatcherDebugService _interestMatcherDebugService;

	@POST
	@Path("publish/event/{topicName}/{eventType}")
	@Produces(MediaType.TEXT_PLAIN)
	@RequireRight("idn:notification-debug:create")
	public String publishKafkaEvent(@PathParam("topicName") String topicName,
									@PathParam("eventType") String eventType,
									String eventContext) {
		TopicDescriptor descriptor = new SimpleTopicDescriptor(TopicScope.POD, topicName.toLowerCase());
		return _interestMatcherDebugService.publishKafkaEvent(descriptor,
				eventType, eventContext, InterestMatcherDebugService.REDIS_INTEREST_DEBUG_KEY);
	}
	@POST
	@Path("publish/event/full/{topicName}/{eventType}")
	@Produces(MediaType.TEXT_PLAIN)
	@RequireRight("idn:notification-debug:create")
	public String publishKafkaEventFullPath(@PathParam("topicName") String topicName,
									@PathParam("eventType") String eventType,
									String eventContext) {
		TopicDescriptor descriptor = new SimpleTopicDescriptor(TopicScope.POD, topicName.toLowerCase());
		return _interestMatcherDebugService.publishKafkaEvent(descriptor,
				eventType, eventContext, "REDIS_DEBUG_KEY");
	}
}
