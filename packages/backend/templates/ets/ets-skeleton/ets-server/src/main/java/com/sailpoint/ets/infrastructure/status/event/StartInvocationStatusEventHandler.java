/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.status.event;

import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.command.status.CreateInvocationStatusCommand;
import com.sailpoint.ets.domain.event.TriggerInvokedEvent;;
import com.sailpoint.ets.domain.trigger.TriggerId;
import com.sailpoint.ets.service.TriggerService;
import com.sailpoint.iris.server.EventHandler;
import com.sailpoint.iris.server.EventHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Start invocations status in status table based on TriggerInvokedEvent.
 */
@Component
@RequiredArgsConstructor(onConstructor_={@Autowired})
@CommonsLog
public class StartInvocationStatusEventHandler implements EventHandler {

	private final TriggerService _triggerService;

	@Override
	public void handleEvent(EventHandlerContext context) {
		TriggerInvokedEvent event = context.getEvent().getContent(TriggerInvokedEvent.class);
		log.info("Create InvocationStatus for id " + event.getInvocationId());
		CreateInvocationStatusCommand cmd = CreateInvocationStatusCommand.builder()
			.invocationId(UUID.fromString(event.getInvocationId()))
			.tenantId(new TenantId(event.getTenantId()))
			.triggerId(new TriggerId(event.getTriggerId()))
			.type(event.getType())
			.invocationType(event.getInvocationType())
			.subscriptionId(UUID.fromString(event.getSubscriptionId()))
			.subscriptionName(event.getSubscriptionName()==null ? "" : event.getSubscriptionName() )
			.subscriptionType(event.getSubscriptionType())
			.subscriptionConfig(event.getSubscriptionConfig())
			.context(event.getContext())
			.input(event.getInput())
			.build();
		_triggerService.createCreateInvocationStatus(cmd);
	}
}
