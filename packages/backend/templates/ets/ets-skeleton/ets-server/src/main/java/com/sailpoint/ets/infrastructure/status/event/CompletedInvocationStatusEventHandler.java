/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.status.event;

import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.command.status.CompleteInvocationStatusCommand;
import com.sailpoint.ets.domain.event.InvocationCompletedEvent;
import com.sailpoint.ets.domain.event.InvocationCompletedTestEvent;
import com.sailpoint.ets.service.TriggerService;
import com.sailpoint.iris.server.EventHandler;
import com.sailpoint.iris.server.EventHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.sailpoint.ets.infrastructure.event.EtsEventHandlingConfig.INVOCATION_COMPLETED_EVENT;
import static com.sailpoint.ets.infrastructure.event.EtsEventHandlingConfig.INVOCATION_COMPLETED_TEST_EVENT;

/**
 * Completed invocations status in status table based on InvocationCompletedEvent.
 */
@Component
@RequiredArgsConstructor(onConstructor_={@Autowired})
@CommonsLog
public class CompletedInvocationStatusEventHandler implements EventHandler {

	private final TriggerService _triggerService;

	@Override
	public void handleEvent(EventHandlerContext context) {
		if (INVOCATION_COMPLETED_EVENT.equals(context.getEvent().getType())) {
			InvocationCompletedEvent event = context.getEvent().getContent(InvocationCompletedEvent.class);
			log.info("Completed InvocationStatus for id " + event.getInvocationId());
			handleInvocationCompleted(event);
		} else if (INVOCATION_COMPLETED_TEST_EVENT.equals(context.getEvent().getType())) {
			InvocationCompletedTestEvent event = context.getEvent().getContent(InvocationCompletedTestEvent.class);
			log.info("Completed InvocationStatus for id " + event.getInvocationId());
			handleInvocationCompleted(event);
		}
	}

	private void handleInvocationCompleted(InvocationCompletedEvent event) {
		CompleteInvocationStatusCommand cmd = CompleteInvocationStatusCommand.builder()
			.invocationId(UUID.fromString(event.getInvocationId()))
			.tenantId(new TenantId(event.getTenantId()))
			.output(event.getOutput())
			.build();
		_triggerService.completeInvocationStatus(cmd);
	}
}
