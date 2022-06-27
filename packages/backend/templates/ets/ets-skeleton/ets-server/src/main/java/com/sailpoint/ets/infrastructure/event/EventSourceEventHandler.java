/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.event;

import com.google.gson.JsonSyntaxException;
import com.sailpoint.atlas.boot.core.web.TenantIdentifier;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.command.InvokeTriggerCommand;
import com.sailpoint.ets.domain.trigger.TriggerRepo;
import com.sailpoint.ets.exception.ValidationException;
import com.sailpoint.ets.infrastructure.util.MetricsReporter;
import com.sailpoint.ets.infrastructure.util.TriggerEventLogUtil;
import com.sailpoint.ets.service.TriggerService;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.server.EventHandler;
import com.sailpoint.iris.server.EventHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Event handler that handles events from event sources that are defined in trigger repo
 */
@CommonsLog
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class EventSourceEventHandler implements EventHandler {

	private final TriggerRepo _triggerRepo;

	private final TriggerService _triggerService;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handleEvent(EventHandlerContext eventHandlerContext) {
		Event event =  eventHandlerContext.getEvent();

		@Deprecated
		String tenantIdentifier;
		@Deprecated
		String requestId;
		// We will move to passing along all event headers from our current mode of persisting only tenantIdentifier and requestId.
		Map headers;
		try {
			headers = event.getHeaders().get();
			String pod = event.ensureHeader(EventHeaders.POD);
			String org = event.ensureHeader(EventHeaders.ORG);
			tenantIdentifier = new TenantIdentifier(pod, org).toString();
		} catch (Exception e) {
			log.error("headers are missing for this event: " + e.getMessage(), e);
			return;
		}

		requestId = event.getHeader(EventHeaders.REQUEST_ID)
			.orElse(UUID.randomUUID().toString());
		headers.putIfAbsent(EventHeaders.REQUEST_ID, requestId);

		// Make sure the corresponding trigger exists for the event type and that the tenant has already subscribed to it
		_triggerRepo.findIdByEventSource(eventHandlerContext.getTopic().getName(), event.getType()).ifPresent(triggerId -> {
			try {

				TenantId tenantId = new TenantId(tenantIdentifier);
				Map<String, Object> input = event.getContent(Map.class);

				InvokeTriggerCommand cmd = InvokeTriggerCommand.builder()
					.tenantId(tenantId)
					.requestId(requestId)
					.triggerId(triggerId)
					.input(input)
					.context(Collections.emptyMap())
					.headers(headers)
					.build();

				_triggerService.invokeTrigger(cmd);

			} catch (JsonSyntaxException e) {
				log.error(TriggerEventLogUtil.logExceptionWithTriggerAndTenantId( "event contentJson parsing failed: " + e.getMessage(),
						triggerId,
						tenantIdentifier,
						e));
				MetricsReporter.reportEventHandlerFailure(e, triggerId);
			} catch (ValidationException e) {
				log.warn(TriggerEventLogUtil.logExceptionWithTriggerAndTenantId( "event contentJson validation against schema failed: " + e.getMessage(),
						triggerId,
						tenantIdentifier,
						e));
				MetricsReporter.reportEventHandlerFailure(e, triggerId);
			} catch (Exception e) {
				log.error(TriggerEventLogUtil.logExceptionWithTriggerAndTenantId( "invocation failed: " + e.getMessage(),
						triggerId,
						tenantIdentifier,
						e));
				MetricsReporter.reportEventHandlerFailure(e, triggerId);
				throw e;
			}
		});
	}

}
