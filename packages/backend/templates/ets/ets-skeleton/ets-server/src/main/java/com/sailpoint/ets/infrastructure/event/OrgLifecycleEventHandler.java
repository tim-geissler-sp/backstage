/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.event;

import com.sailpoint.atlas.boot.core.web.TenantIdentifier;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.command.DeleteTenantCommand;
import com.sailpoint.ets.service.TriggerService;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.server.EventHandler;
import com.sailpoint.iris.server.EventHandlerContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Event handlers that handle org-lifecycle events.
 */
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class OrgLifecycleEventHandler implements EventHandler {

	private final TriggerService _triggerService;

	/**
	 * Event handler that cleans up an org's invocation and subscription states upon ORG_DELETE event.
	 *
	 * @param context The event handler context.
	 */
	@Override
	public void handleEvent(EventHandlerContext context) {
		DeleteTenantCommand cmd = DeleteTenantCommand.builder()
			.tenantId(getTenantId(context.getEvent()))
			.build();

		_triggerService.deleteTenant(cmd);
	}

	private static TenantId getTenantId(Event event) {
		String pod = event.ensureHeader(EventHeaders.POD);
		String org = event.ensureHeader(EventHeaders.ORG);
		return new TenantId(new TenantIdentifier(pod, org).toString());
	}
}
