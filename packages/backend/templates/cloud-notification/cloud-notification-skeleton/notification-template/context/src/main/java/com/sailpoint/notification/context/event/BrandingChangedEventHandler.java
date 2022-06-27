/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
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
import com.sailpoint.notification.context.common.model.BrandConfig;
import com.sailpoint.notification.context.common.util.BrandConfigMapper;
import com.sailpoint.notification.context.service.GlobalContextDebugService;
import com.sailpoint.notification.context.service.GlobalContextService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static java.util.Objects.requireNonNull;

/**
 *  Event handler for BRANDING events
 */
public class BrandingChangedEventHandler implements EventHandler {

	private final Log _log = LogFactory.getLog(BrandingChangedEventHandler.class);

	private GlobalContextService _globalContextService;

	private GlobalContextDebugService _globalContextDebugService;

	@VisibleForTesting
	@Inject
	public BrandingChangedEventHandler(GlobalContextService globalContextService, GlobalContextDebugService globalContextDebugService) {
		_globalContextService = globalContextService;
		_globalContextDebugService = globalContextDebugService;
	}

	@Override
	public void handleEvent(EventHandlerContext eventHandlerContext) {
		Event event = eventHandlerContext.getEvent();
		String tenant = requireNonNull(event.getHeader(EventHeaders.ORG).get());
		_log.info("Handling event " +  event.getType());

		if(event.getType().equals(EventType.BRANDING_CREATED) || event.getType().equals(EventType.BRANDING_UPDATED)) {
			_log.info("Event content " + event.getContentJson());
			BrandConfig config = event.getContent(BrandConfig.class);
			_globalContextService.saveBrandingAttributes(tenant, BrandConfigMapper.brandingConfigToMap(config));
			if (event.getHeader(GlobalContextDebugService.REDIS_CONTEXT_DEBUG_KEY).isPresent()) {
				processDebugEvent(event, JsonUtil.toJson(_globalContextService.findOneByTenant(tenant).get()));
			}
		} else if(event.getType().equals(EventType.BRANDING_DELETED)) {
			String name = event.getContent(String.class);
			_globalContextService.deleteBranding(tenant, name);
			if (event.getHeader(GlobalContextDebugService.REDIS_CONTEXT_DEBUG_KEY).isPresent()) {
				processDebugEvent(event, name);
			}
		}
	}

	private void processDebugEvent(Event event, String debugInfo) {
		// Debug event
		_log.info("Handling debug " + event.getType() + " event id " + event.getId());
		String header = event.getHeader(GlobalContextDebugService.REDIS_CONTEXT_DEBUG_KEY).get();
		_globalContextDebugService.writeToStore(header, debugInfo);
	}
}
