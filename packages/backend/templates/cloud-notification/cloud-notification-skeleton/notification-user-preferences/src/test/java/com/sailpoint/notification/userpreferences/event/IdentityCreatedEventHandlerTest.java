/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.userpreferences.event;

import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.iris.client.OrgTopic;
import com.sailpoint.iris.client.Topic;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.api.event.RecipientBuilder;
import com.sailpoint.notification.userpreferences.dto.UserPreferences;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;
import com.sailpoint.notification.userpreferences.service.UserPreferencesDebugService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for IdentityCreatedEventHandler
 */
public class IdentityCreatedEventHandlerTest {

	private final String RECIPIENT_ID = "70e7cde5-3473-46ea-94ea-90bc8c605a6c";
	@Mock
	UserPreferencesRepository _userPreferencesRepository;

	@Mock
	UserPreferencesDebugService _userPreferencesDebugService;

	@Mock
	EventHandlerContext _context;
	private IdentityCreatedEventHandler _identityCreatedEventHandler;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		Topic topic = new OrgTopic("notification", "dev", "acme-solar");

		when(_context.getTopic())
				.thenReturn(topic);

		_identityCreatedEventHandler = new IdentityCreatedEventHandler(_userPreferencesRepository, _userPreferencesDebugService);
	}

	@Test
	public void handleCreateEventTest() {

		HashMap<String, Object> contentJson = new HashMap<>();
		HashMap<String, Object> identityMap = new HashMap();

		identityMap.put("id", RECIPIENT_ID);
		identityMap.put("name", "john.doe");
		identityMap.put("type", "IDENTITY");

		Map<String, Object> attributes = new HashMap<>();
		attributes.put("firstname", "John");
		attributes.put("lastname", "Doe");
		attributes.put("email", "john.doe@gmail.com");
		attributes.put("displayName", "John Doe");
		attributes.put("department", "Sales");
		attributes.put("isManager", false);
		attributes.put("phone", "512 512 5125");
		attributes.put("brand", "brand1");
		attributes.put("manager", Collections.singletonMap("id", "ee769173319b41d19ccec6c235423237b"));

		contentJson.put("attributes",attributes);
		contentJson.put("identity", identityMap);

		Event event = EventBuilder.withTypeAndContent(EventType.IDENTITY_CREATED, contentJson)
				.addHeader("REDIS_USER_DEBUG_KEY", "redisKey123")
				.build();

		withEvent(event);

		when(_userPreferencesDebugService.getUserPreferences(RECIPIENT_ID)).thenReturn(new UserPreferences.UserPreferencesBuilder()
				.withRecipient(new RecipientBuilder()
						.withId(RECIPIENT_ID)
						.build())
				.build());


		_identityCreatedEventHandler.handleEvent(_context);
		ArgumentCaptor<UserPreferences> userPreferencesArgumentCaptor = ArgumentCaptor.forClass(UserPreferences.class);
		verify(_userPreferencesRepository, times(1)).create(userPreferencesArgumentCaptor.capture());
		thenUserPreferencesHasValues(userPreferencesArgumentCaptor.getValue());

		verify(_userPreferencesDebugService, times(1)).getUserPreferences(eq(RECIPIENT_ID));
		verify(_userPreferencesDebugService, times(1)).writeToStore(any(String.class),eq(RECIPIENT_ID));

	}

	private void thenUserPreferencesHasValues(UserPreferences userPreferences) {
		Assert.assertEquals("John Doe", userPreferences.getRecipient().getName());
		Assert.assertEquals(RECIPIENT_ID, userPreferences.getRecipient().getId());
		Assert.assertEquals("john.doe@gmail.com", userPreferences.getRecipient().getEmail());
		Assert.assertEquals("512 512 5125", userPreferences.getRecipient().getPhone());
		Assert.assertEquals("brand1", userPreferences.getBrand().get());
	}

	@Test
	public void emptyIdentityMapInPayloadTest() {
		Event event = EventBuilder.withTypeAndContentJson(EventType.IDENTITY_ATTRIBUTE_CHANGED, "{identity:{}}")
				.build();

		withEvent(event);

		_identityCreatedEventHandler.handleEvent(_context);
		verify(_userPreferencesRepository, times(0)).create(any(UserPreferences.class));
	}

	@Test
	public void nullIdentityMapInPayloadTest() {
		Event event = EventBuilder.withTypeAndContentJson(EventType.IDENTITY_ATTRIBUTE_CHANGED, "{identity:null}")
				.build();

		withEvent(event);

		_identityCreatedEventHandler.handleEvent(_context);
		verify(_userPreferencesRepository, times(0)).create(any(UserPreferences.class));
	}

	@Test
	public void identityMapMissingIdPayloadTest() {
		Event event = EventBuilder.withTypeAndContentJson(EventType.IDENTITY_ATTRIBUTE_CHANGED, "{identity:{'notId':'abc1'}, attributes:{}}")
				.build();

		withEvent(event);

		_identityCreatedEventHandler.handleEvent(_context);
		verify(_userPreferencesRepository, times(0)).create(any(UserPreferences.class));
	}

	@Test
	public void nullExternalIdTest() {

		HashMap<String, Object> contentJson = new HashMap<>();
		HashMap<String, Object> identityMap = new HashMap();

		identityMap.put("id", null);
		identityMap.put("name", "john.doe");
		identityMap.put("type", "IDENTITY");

		Map<String, Object> attributes = new HashMap<>();
		attributes.put("firstname", "John");
		attributes.put("lastname", "Doe");
		attributes.put("email", "john.doe@gmail.com");
		attributes.put("displayName", "John Doe");
		attributes.put("phone", "512 512 5125");
		attributes.put("brand", "brand1");

		contentJson.put("attributes",attributes);
		contentJson.put("identity", identityMap);
		Event event = EventBuilder.withTypeAndContent(EventType.IDENTITY_CREATED, contentJson)
				.build();

		withEvent(event);
		_identityCreatedEventHandler.handleEvent(_context);
	}

	@Test
	public void nullPhoneTest() {

		HashMap<String, Object> contentJson = new HashMap<>();
		HashMap<String, Object> identityMap = new HashMap();

		identityMap.put("id", RECIPIENT_ID);
		identityMap.put("name", "john.doe");
		identityMap.put("type", "IDENTITY");

		Map<String, Object> attributes = new HashMap<>();
		attributes.put("firstname", "John");
		attributes.put("lastname", "Doe");
		attributes.put("email", "john.doe@gmail.com");
		attributes.put("displayName", "John Doe");
		attributes.put("phone", null);
		attributes.put("brand", "brand1");

		contentJson.put("attributes",attributes);
		contentJson.put("identity", identityMap);
		Event event = EventBuilder.withTypeAndContent(EventType.IDENTITY_CREATED, contentJson)
				.build();
		withEvent(event);
		_identityCreatedEventHandler.handleEvent(_context);
		verify(_userPreferencesRepository, times(1)).create(any(UserPreferences.class));

	}

	@Test
	public void nullBrandTest() {

		HashMap<String, Object> contentJson = new HashMap<>();
		HashMap<String, Object> identityMap = new HashMap();

		identityMap.put("id", RECIPIENT_ID);
		identityMap.put("name", "john.doe");
		identityMap.put("type", "IDENTITY");

		Map<String, Object> attributes = new HashMap<>();
		attributes.put("firstname", "John");
		attributes.put("lastname", "Doe");
		attributes.put("email", "john.doe@gmail.com");
		attributes.put("displayName", "John Doe");
		attributes.put("phone", "512-111-2333");
		attributes.put("brand", null);

		contentJson.put("attributes",attributes);
		contentJson.put("identity", identityMap);
		Event event = EventBuilder.withTypeAndContent(EventType.IDENTITY_CREATED, contentJson)
				.build();
		withEvent(event);
		_identityCreatedEventHandler.handleEvent(_context);
		verify(_userPreferencesRepository, times(1)).create(any(UserPreferences.class));
	}

	private void withEvent(Event event) {
		when(_context.getEvent())
				.thenReturn(event);
	}
}
