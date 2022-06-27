/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.event;

import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.command.CompleteInvocationCommand;
import com.sailpoint.ets.domain.command.DispatchInvocationCommand;
import com.sailpoint.ets.domain.event.TriggerInvokedEvent;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.service.TriggerService;
import com.sailpoint.iris.server.EventHandler;
import com.sailpoint.iris.server.EventHandlerContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

/**
 * TriggerEvents
 */
@Component
@RequiredArgsConstructor(onConstructor_={@Autowired})
public class TriggerEventHandler implements EventHandler {

	private final TriggerService _triggerService;

	@Override
	public void handleEvent(EventHandlerContext context) {
		TriggerInvokedEvent event = context.getEvent().getContent(TriggerInvokedEvent.class);
		if (event.getSubscriptionType() == SubscriptionType.INLINE)  {
			CompleteInvocationCommand cmd = CompleteInvocationCommand.builder()
				.tenantId(new TenantId(event.getTenantId()))
				.requestId(event.getRequestId())
				.invocationId(UUID.fromString(event.getInvocationId()))
				.subscriptionId(UUID.fromString(event.getSubscriptionId()))
				.secret(event.getSecret())
				.build();

			_triggerService.completeInvocation(cmd);
		} else {
			DispatchInvocationCommand cmd = DispatchInvocationCommand.builder()
				.tenantId(new TenantId(event.getTenantId()))
				.triggerId(event.getTriggerId())
				.requestId(event.getRequestId())
				.invocationId(UUID.fromString(event.getInvocationId()))
				.secret(event.getSecret())
				.triggerType(event.getType())
				.subscriptionId(event.getSubscriptionId())
				.subscriptionType(event.getSubscriptionType())
				.subscriptionConfig(event.getSubscriptionConfig())
				.scriptSource(event.getScriptSource())
				.input(event.getInput())
				.headers(context.getEvent().getHeaders().orElse(Collections.EMPTY_MAP))
				.build();

			_triggerService.dispatchInvocation(cmd);
		}
	}
}
