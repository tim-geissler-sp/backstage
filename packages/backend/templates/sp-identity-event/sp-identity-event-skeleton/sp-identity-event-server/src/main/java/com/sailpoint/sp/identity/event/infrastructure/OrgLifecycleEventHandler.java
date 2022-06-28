/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.infrastructure;

import com.sailpoint.atlas.RequestContext;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.server.EventHandler;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.sp.identity.event.IdentityEventConfig;
import com.sailpoint.sp.identity.event.domain.TenantId;
import com.sailpoint.sp.identity.event.domain.command.DeleteTenantCommand;
import com.sailpoint.sp.identity.event.domain.service.IdentityEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.sailpoint.sp.identity.event.IdentityEventConfig.ORG_DELETED;

/**
 * Event handler that handles event from org_lifecycle topic.
 *
 * @see IdentityEventConfig#registerEventHandlers()
 */
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class OrgLifecycleEventHandler implements EventHandler {

	private final IdentityEventService _identityEventService;

	@Override
	public void handleEvent(EventHandlerContext context) throws Exception {
		if (ORG_DELETED.equals(context.getEvent().getType())) {
			DeleteTenantCommand cmd = DeleteTenantCommand.builder()
				.tenantId(getTenantId(context))
				.build();

			_identityEventService.deleteTenant(cmd);
		}
	}

	/**
	 * Get TenantId from current RequestContext.
	 *
	 * @param ctx EventHandlerContext.
	 * @return TenantId.
	 */
	private TenantId getTenantId(EventHandlerContext ctx) {
		return RequestContext.get()
			.flatMap(RequestContext::getTenantId)
			.map(TenantId::new)
			.orElseThrow(() -> new IllegalStateException("No TenantId found for org " + ctx.getEvent().getHeader(EventHeaders.ORG)));
	}
}
