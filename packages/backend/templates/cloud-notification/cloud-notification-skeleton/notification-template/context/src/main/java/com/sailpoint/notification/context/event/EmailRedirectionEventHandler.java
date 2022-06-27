/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.context.event;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.sailpoint.utilities.JsonUtil;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.server.EventHandler;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.context.service.GlobalContextDebugService;
import com.sailpoint.notification.context.service.GlobalContextService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static com.sailpoint.notification.context.service.GlobalContextService.EMAIL_OVERRIDE;

/**
 * Event handler for EMAIL_REDIRECTION_ENABLED and EMAIL_REDIRECTION_DISABLED
 */
public class EmailRedirectionEventHandler implements EventHandler {

	private final Log _log = LogFactory.getLog(EmailRedirectionEventHandler.class);

	private GlobalContextService _globalContextService;

	private GlobalContextDebugService _globalContextDebugService;

	@VisibleForTesting
	@Inject
	public EmailRedirectionEventHandler(GlobalContextService globalContextService, GlobalContextDebugService globalContextDebugService) {
		_globalContextService = globalContextService;
		_globalContextDebugService = globalContextDebugService;
	}

	@Override
	public void handleEvent(EventHandlerContext eventHandlerContext) {
		Event event = eventHandlerContext.getEvent();
		_log.info("Handling event of type " + event.getType());

		String tenant = event.ensureHeader(EventHeaders.ORG);

		if (event.getType().equals(EventType.EMAIL_REDIRECTION_ENABLED)) {
			processDebugEvent(event, event.getContent(String.class));
			_globalContextService.saveAttribute(tenant, EMAIL_OVERRIDE, event.getContent(String.class));
		} else if (event.getType().equals(EventType.EMAIL_REDIRECTION_DISABLED)) {
			_globalContextService.removeAttribute(tenant, EMAIL_OVERRIDE);
			processDebugEvent(event, JsonUtil.toJson(_globalContextService.getDefaultContext(tenant)));
		}


	}

	private void processDebugEvent(Event event, String debugInfo) {
		// Debug event
		_log.info("Handling debug " + event.getType() + " event id " + event.getId());
		event.getHeader(GlobalContextDebugService.REDIS_CONTEXT_DEBUG_KEY).ifPresent(header ->
			_globalContextDebugService.writeToStore(header, debugInfo));
	}
}
