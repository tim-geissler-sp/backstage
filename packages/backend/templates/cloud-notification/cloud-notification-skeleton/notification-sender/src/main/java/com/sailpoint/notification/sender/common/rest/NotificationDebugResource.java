/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.sender.common.rest;

import com.google.inject.Inject;
import com.sailpoint.atlas.security.RequireRight;
import com.sailpoint.notification.api.event.dto.NotificationRendered;
import com.sailpoint.notification.api.event.dto.SlackNotificationRendered;
import com.sailpoint.notification.api.event.dto.TeamsNotificationRendered;
import com.sailpoint.notification.sender.common.service.SenderDebugService;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static java.util.Objects.requireNonNull;

/**
 *This Resource is meant to allow for debugging and testing the service that is primarily based on Kafka -- and Kafka
 * is a black box to debugging/tests.
 */
@Path("debug")
public class NotificationDebugResource {

	@Inject
	SenderDebugService _senderDebugService;

	@POST
	@Path("sendmail")
	@Produces(MediaType.TEXT_PLAIN)
	@RequireRight("idn:notification-debug:create")
	public String sendMail(NotificationRendered notificationRendered) {
		return _senderDebugService.publishMailEvent(notificationRendered);
	}

	@POST
	@Path("sendslack")
	@Produces(MediaType.TEXT_PLAIN)
	@RequireRight("idn:notification-debug:create")
	public String sendSlack(SlackNotificationRendered notificationRendered) {
		return _senderDebugService.publishSlackEvent(notificationRendered);
	}

	@POST
	@Path("sendteams")
	@Produces(MediaType.TEXT_PLAIN)
	@RequireRight("idn:notification-debug:create")
	public String sendTeams(TeamsNotificationRendered notificationRendered) {
		return _senderDebugService.publishTeamsEvent(notificationRendered);
	}

	@GET
	@Path("retrieve/{key}")
	@Produces(MediaType.TEXT_PLAIN)
	@RequireRight("idn:notification-debug:read")
	public String retrieve(@PathParam("key") String key) {
		requireNonNull(key);
		return _senderDebugService.retrieveFromStore(key);
	}
}
