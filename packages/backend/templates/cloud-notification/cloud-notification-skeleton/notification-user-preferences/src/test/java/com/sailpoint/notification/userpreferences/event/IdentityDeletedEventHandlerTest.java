/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.event;

import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.iris.client.OrgTopic;
import com.sailpoint.iris.client.Topic;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;
import com.sailpoint.notification.userpreferences.service.UserPreferencesDebugService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for IdentityDeletedEventHandler
 */
public class IdentityDeletedEventHandlerTest {

	@Mock
	UserPreferencesRepository _userPreferencesRepository;

	@Mock
	EventHandlerContext _context;

	@Mock
	UserPreferencesDebugService _userPreferencesDebugService;

	private IdentityDeletedEventHandler _identityDeletedEventHandler;

	private final String RECIPIENT_ID = "70e7cde5-3473-46ea-94ea-90bc8c605a6c";

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		Topic topic = new OrgTopic("notification", "dev", "acme-solar");

		when(_context.getTopic())
				.thenReturn(topic);

		_identityDeletedEventHandler = new IdentityDeletedEventHandler(_userPreferencesRepository, _userPreferencesDebugService);
	}

	@Test
	public void handleDeleteEventTest() {
		HashMap<String, Object> identityMap = new HashMap();

		identityMap.put("id", RECIPIENT_ID);
		identityMap.put("name", "john.doe");
		identityMap.put("type", "IDENTITY");

		Map contentJson = Collections.singletonMap("identity", identityMap);

		Event event = EventBuilder.withTypeAndContent(EventType.IDENTITY_DELETED, contentJson)
				.build();
		withEvent(event);

		_identityDeletedEventHandler.handleEvent(_context);

		ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
		verify(_userPreferencesRepository, times(1)).deleteByRecipientId(idCaptor.capture());
		assertEquals(RECIPIENT_ID, idCaptor.getValue());
	}

	@Test
	public void emptyDeletedPayloadTest() {
		Event event = EventBuilder.withTypeAndContentJson(EventType.IDENTITY_DELETED, "")
				.build();
		withEvent(event);

		_identityDeletedEventHandler.handleEvent(_context);
		verify(_userPreferencesRepository, times(0)).deleteByRecipientId(any(String.class));
	}

	private void withEvent(Event event) {
		when(_context.getEvent())
				.thenReturn(event);
	}
}
