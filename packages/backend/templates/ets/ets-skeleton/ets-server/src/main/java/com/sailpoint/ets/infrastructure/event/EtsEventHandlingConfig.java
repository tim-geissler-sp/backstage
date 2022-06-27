/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.event;

import com.sailpoint.atlas.boot.event.EventRegistry;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.ets.domain.trigger.TriggerRepo;
import com.sailpoint.ets.infrastructure.status.event.CompletedInvocationStatusEventHandler;
import com.sailpoint.ets.infrastructure.status.event.FailedInvocationStatusEventHandler;
import com.sailpoint.ets.infrastructure.status.event.StartInvocationStatusEventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * Event handling configuration that registers event handlers to handle events with specific types from specific topics
 */
@Configuration
@RequiredArgsConstructor(onConstructor_={@Autowired})
public class EtsEventHandlingConfig {

	public static final String INVOCATION_COMPLETED_EVENT = "InvocationCompletedEvent";
	public static final String INVOCATION_COMPLETED_TEST_EVENT = "InvocationCompletedTestEvent";

	public static final String INVOCATION_FAILED_EVENT = "InvocationFailedEvent";
	public static final String INVOCATION_FAILED_TEST_EVENT = "InvocationFailedTestEvent";

	private final EventRegistry _eventRegistry;

	private final TriggerRepo _triggerRepo;

	@PostConstruct
	public void registerEventHandlers() {

		// Scanning through triggers and register event sources
		_triggerRepo.findAll().forEach(t -> {
			if (t.getEventSources() != null) {
				t.getEventSources().forEach( es -> {
					_eventRegistry.register(IdnTopic.valueOf(es.getTopic()), es.getEventType(), EventSourceEventHandler.class);
				});
			}
		});

		_eventRegistry.register(IdnTopic.ORG_LIFECYCLE, "ORG_DELETED", OrgLifecycleEventHandler.class);

		_eventRegistry.register(IdnTopic.TRIGGER, "TriggerInvokedEvent", TriggerEventHandler.class);
		_eventRegistry.register(IdnTopic.TRIGGER, "TriggerInvokedEvent", StartInvocationStatusEventHandler.class);

		_eventRegistry.register(IdnTopic.TRIGGER_ACK, INVOCATION_COMPLETED_EVENT, CompletedInvocationStatusEventHandler.class);
		_eventRegistry.register(IdnTopic.TRIGGER_ACK, INVOCATION_COMPLETED_TEST_EVENT, CompletedInvocationStatusEventHandler.class);

		_eventRegistry.register(IdnTopic.TRIGGER_ACK, INVOCATION_FAILED_EVENT, FailedInvocationStatusEventHandler.class);
		_eventRegistry.register(IdnTopic.TRIGGER_ACK, INVOCATION_FAILED_TEST_EVENT, FailedInvocationStatusEventHandler.class);

	}

}
