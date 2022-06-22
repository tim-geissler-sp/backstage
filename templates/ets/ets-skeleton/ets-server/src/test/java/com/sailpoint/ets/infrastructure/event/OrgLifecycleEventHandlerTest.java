/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.event;

import com.sailpoint.atlas.boot.core.web.TenantIdentifier;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.command.DeleteTenantCommand;
import com.sailpoint.ets.service.TriggerService;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.server.EventHandlerContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TenantEvents.
 */
@RunWith(MockitoJUnitRunner.class)
public class OrgLifecycleEventHandlerTest {

	@Mock
	private EventHandlerContext _context;

	@Mock
	private TriggerService _triggerService;

	private OrgLifecycleEventHandler _events;
	private DeleteTenantCommand _cmd;

	@Before
	public void setUp() {
		_events = new OrgLifecycleEventHandler(_triggerService);
		_cmd = DeleteTenantCommand.builder()
			.tenantId(new TenantId(new TenantIdentifier("dev", "acme-solar").toString()))
			.build();
	}

	@Test
	public void orgDeleted() {
		givenEvent(EventBuilder.withTypeAndContent("ORG_DELETED", Collections.emptyMap())
				.addHeader(EventHeaders.POD, "dev")
				.addHeader(EventHeaders.ORG, "acme-solar")
			.build());

		_events.handleEvent(_context);
		verify(_triggerService).deleteTenant(eq(_cmd));
	}

	private void givenEvent(Event event) {
		when(_context.getEvent())
				.thenReturn(event);
	}
}
