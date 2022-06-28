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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for IdentityAttributeChangedEventHandler
 */
public class IdentityAttributeChangedEventHandlerTest {

	private static final String USERS = "users";
	private final String RECIPIENT_ID = "70e7cde5-3473-46ea-94ea-90bc8c605a6c";
	@Mock
	UserPreferencesRepository _userPreferencesRepository;

	@Mock
	UserPreferencesDebugService _userPreferencesDebugService;

	@Mock
	EventHandlerContext _context;
	private IdentityAttributesChangedEventHandler _identityAttributesChangedEventHandler;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		Topic topic = new OrgTopic("notification", "dev", "acme-solar");

		when(_context.getTopic())
				.thenReturn(topic);

		_identityAttributesChangedEventHandler = new IdentityAttributesChangedEventHandler(_userPreferencesRepository, _userPreferencesDebugService);
	}

	@Test
	/**
	 * Happy path test with multiple relevant changes and debug redis key present in header
	 */
	public void handleCreateEventTest() {

		// Setup test IdentityAttributeChangedEvent
		HashMap<String, Object> contentJson = new HashMap<>();
		HashMap<String, Object> identityMap = new HashMap();
		List<Map<String,String>> changes = new LinkedList<>();
		identityMap.put("id", "2c9180877740fd7f01774b662b324f9b");
		identityMap.put("name", "Zechariah.srcadmb349");
		identityMap.put("type", "IDENTITY");

		Map<String, String> attributeChange1 = new HashMap<>();
		attributeChange1.put("attribute", "displayName");
		attributeChange1.put("oldValue", "ZECHARIAH SRCADMB");
		attributeChange1.put("newValue", "zechariah srcadmb");

		Map<String, String> attributeChange2 = new HashMap<>();
		attributeChange2.put("attribute", "email");
		attributeChange2.put("oldValue", "Zechariah.srcadmb349@email.com");
		attributeChange2.put("newValue", "Zechariah.srcadmb122@email.com");

		Map<String, String> attributeChange3 = new HashMap<>();
		attributeChange3.put("attribute", "brand");
		attributeChange3.put("oldValue", "default");
		attributeChange3.put("newValue", "not default");

		changes.add(attributeChange1);
		changes.add(attributeChange2);
		changes.add(attributeChange3);


		contentJson.put("changes",changes);
		contentJson.put("identity", identityMap);

		Event event = EventBuilder.withTypeAndContent(EventType.IDENTITY_ATTRIBUTE_CHANGED, contentJson)
				.addHeader("REDIS_USER_DEBUG_KEY", "redisKey123")
				.build();

		// Setup replies from mocks and have event present
		withEvent(event);

		when(_userPreferencesRepository.findByRecipientId("2c9180877740fd7f01774b662b324f9b")).thenReturn(
				new UserPreferences.UserPreferencesBuilder()
						.withRecipient(new RecipientBuilder()
								.withId("2c9180877740fd7f01774b662b324f9b")
								.withEmail("Zechariah.srcadmb349@email.com")
								.withPhone("512-213-3333")
								.withName("ZECHARIAH SRCADMB").build())
						.withBrand(Optional.ofNullable("default"))
						.build());
		when(_userPreferencesDebugService.getUserPreferences("2c9180877740fd7f01774b662b324f9b"))
				.thenReturn(
						new UserPreferences.UserPreferencesBuilder()
							.withRecipient(new RecipientBuilder()
									.withId("2c9180877740fd7f01774b662b324f9b")
									.build())
								.build());
		//method under test
		_identityAttributesChangedEventHandler.handleEvent(_context);

		//list of asserts/verifies
		ArgumentCaptor<UserPreferences> userPreferencesArgumentCaptor = ArgumentCaptor.forClass(UserPreferences.class);
		verify(_userPreferencesRepository, times(1)).create(userPreferencesArgumentCaptor.capture());
		thenUserPreferencesHasValues(userPreferencesArgumentCaptor.getValue());
		verify(_userPreferencesDebugService, times(1)).getUserPreferences(eq("2c9180877740fd7f01774b662b324f9b"));
		verify(_userPreferencesDebugService, times(1)).writeToStore(any(String.class),eq("2c9180877740fd7f01774b662b324f9b"));

	}

	private void thenUserPreferencesHasValues(UserPreferences userPreferences) {
		Assert.assertEquals("zechariah srcadmb", userPreferences.getRecipient().getName());
		Assert.assertEquals("2c9180877740fd7f01774b662b324f9b", userPreferences.getRecipient().getId());
		Assert.assertEquals("Zechariah.srcadmb122@email.com", userPreferences.getRecipient().getEmail());
		Assert.assertEquals("512-213-3333", userPreferences.getRecipient().getPhone());
		Assert.assertEquals("not default", userPreferences.getBrand().get());
	}

	@Test
	public void missingIdentityMapInPayloadTest() {
		Event event = EventBuilder.withTypeAndContentJson(EventType.IDENTITY_ATTRIBUTE_CHANGED, "{changes:{}}")
				.build();

		withEvent(event);

		_identityAttributesChangedEventHandler.handleEvent(_context);
		verify(_userPreferencesRepository, times(0)).create(any(UserPreferences.class));
	}

	@Test
	public void identityMapMissingIdPayloadTest() {
		Event event = EventBuilder.withTypeAndContentJson(EventType.IDENTITY_ATTRIBUTE_CHANGED, "{identity:{'notId':'abc1'}, attributes:{}}")
				.build();

		withEvent(event);

		_identityAttributesChangedEventHandler.handleEvent(_context);
		verify(_userPreferencesRepository, times(0)).create(any(UserPreferences.class));
	}

	@Test
	// In this case there is no changes object in the payload
	public void changesMissingFromPayloadTest() {
		HashMap<String, Object> contentJson = new HashMap<>();
		HashMap<String, Object> identityMap = new HashMap();
		identityMap.put("id", "2c9180877740fd7f01774b662b324f9b");
		identityMap.put("name", "Zechariah.srcadmb349");
		identityMap.put("type", "IDENTITY");

		contentJson.put("identity", identityMap);
		contentJson.put("changes", null);

		Event event = EventBuilder.withTypeAndContent(EventType.IDENTITY_ATTRIBUTE_CHANGED, contentJson)
				.build();

		withEvent(event);

		_identityAttributesChangedEventHandler.handleEvent(_context);
		verify(_userPreferencesRepository, times(0)).create(any(UserPreferences.class));

	}

	@Test
	public void nullExternalIdTest() {
		HashMap<String, Object> contentJson = new HashMap<>();
		HashMap<String, Object> identityMap = new HashMap();
		List<Map<String,String>> changes = new LinkedList<>();
		identityMap.put("id", null);
		identityMap.put("name", "Zechariah.srcadmb349");
		identityMap.put("type", "IDENTITY");
		Map<String, String> attributeChange = new HashMap<>();
		attributeChange.put("attribute", "displayName");
		attributeChange.put("oldValue", "ZECHARIAH SRCADMB");
		attributeChange.put("newValue", "zechariah srcadmb");
		changes.add(attributeChange);

		contentJson.put("changes",changes);
		contentJson.put("identity", identityMap);

		Event event = EventBuilder.withTypeAndContent(EventType.IDENTITY_ATTRIBUTE_CHANGED, contentJson)
				.build();

		withEvent(event);
		_identityAttributesChangedEventHandler.handleEvent(_context);
	}

	@Test
	public void nullPhoneTest() {

		HashMap<String, Object> contentJson = new HashMap<>();
		HashMap<String, Object> identityMap = new HashMap();
		List<Map<String,String>> changes = new LinkedList<>();
		identityMap.put("id", "2c9180877740fd7f01774b662b324f9b");
		identityMap.put("name", "Zechariah.srcadmb349");
		identityMap.put("type", "IDENTITY");
		Map<String, String> attributeChange = new HashMap<>();
		attributeChange.put("attribute", "phone");
		attributeChange.put("oldValue", "512-213-3333");
		attributeChange.put("newValue", null);
		changes.add(attributeChange);

		contentJson.put("changes",changes);
		contentJson.put("identity", identityMap);

		Event event = EventBuilder.withTypeAndContent(EventType.IDENTITY_ATTRIBUTE_CHANGED, contentJson)
				.build();

		withEvent(event);

		when(_userPreferencesRepository.findByRecipientId("2c9180877740fd7f01774b662b324f9b")).thenReturn(
				new UserPreferences.UserPreferencesBuilder()
						.withRecipient(new RecipientBuilder()
								.withId("2c9180877740fd7f01774b662b324f9b")
								.withEmail("Zechariah.srcadmb349@email.com")
								.withPhone("512-213-3333")
								.withName("ZECHARIAH SRCADMB").build())
						.withBrand(Optional.ofNullable("default"))
						.build());
		_identityAttributesChangedEventHandler.handleEvent(_context);
		verify(_userPreferencesRepository, times(1)).create(any(UserPreferences.class));

	}

	@Test
	// Verify that if null should come in as a newValue, that it doesn't break things.
	public void nullBrandTest() {

		HashMap<String, Object> contentJson = new HashMap<>();
		HashMap<String, Object> identityMap = new HashMap();
		List<Map<String,String>> changes = new LinkedList<>();
		identityMap.put("id", "2c9180877740fd7f01774b662b324f9b");
		identityMap.put("name", "Zechariah.srcadmb349");
		identityMap.put("type", "IDENTITY");
		Map<String, String> attributeChange = new HashMap<>();
		attributeChange.put("attribute", "brand");
		attributeChange.put("oldValue", "default");
		attributeChange.put("newValue", null);
		changes.add(attributeChange);

		contentJson.put("changes",changes);
		contentJson.put("identity", identityMap);

		Event event = EventBuilder.withTypeAndContent(EventType.IDENTITY_ATTRIBUTE_CHANGED, contentJson)
				.build();

		withEvent(event);

		when(_userPreferencesRepository.findByRecipientId("2c9180877740fd7f01774b662b324f9b")).thenReturn(
				new UserPreferences.UserPreferencesBuilder()
						.withRecipient(new RecipientBuilder()
								.withId("2c9180877740fd7f01774b662b324f9b")
								.withEmail("Zechariah.srcadmb349@email.com")
								.withPhone("512-213-3333")
								.withName("ZECHARIAH SRCADMB").build())
						.withBrand(Optional.ofNullable("default"))
						.build());
		_identityAttributesChangedEventHandler.handleEvent(_context);
		verify(_userPreferencesRepository, times(1)).create(any(UserPreferences.class));
	}

	@Test
	// Verify that if null should come in as a newValue, that it doesn't break things.
	public void irrelevantAttributeChangeTest() {

		HashMap<String, Object> contentJson = new HashMap<>();
		HashMap<String, Object> identityMap = new HashMap();
		List<Map<String,String>> changes = new LinkedList<>();
		identityMap.put("id", "2c9180877740fd7f01774b662b324f9b");
		identityMap.put("name", "Zechariah.srcadmb349");
		identityMap.put("type", "IDENTITY");
		Map<String, String> attributeChange = new HashMap<>();
		attributeChange.put("attribute", "manager");
		attributeChange.put("oldValue", "Joe");
		attributeChange.put("newValue", "John");
		changes.add(attributeChange);

		contentJson.put("changes",changes);
		contentJson.put("identity", identityMap);

		Event event = EventBuilder.withTypeAndContent(EventType.IDENTITY_ATTRIBUTE_CHANGED, contentJson)
									.addHeader("REDIS_USER_DEBUG_KEY", "redisKey123")
									.build();

		withEvent(event);

		when(_userPreferencesDebugService.getUserPreferences("2c9180877740fd7f01774b662b324f9b")).thenReturn(
				new UserPreferences.UserPreferencesBuilder()
						.withRecipient(new RecipientBuilder()
								.withId("2c9180877740fd7f01774b662b324f9b")
								.withEmail("Zechariah.srcadmb349@email.com")
								.withPhone("512-213-3333")
								.withName("ZECHARIAH SRCADMB").build())
						.withBrand(Optional.ofNullable("default"))
						.build());

		// Exexcute method under test
		_identityAttributesChangedEventHandler.handleEvent(_context);

		verify(_userPreferencesRepository, times(0)).create(any(UserPreferences.class));
		verify(_userPreferencesDebugService, times(1)).getUserPreferences(eq("2c9180877740fd7f01774b662b324f9b"));
		verify(_userPreferencesDebugService, times(1)).writeToStore(any(String.class),eq("2c9180877740fd7f01774b662b324f9b"));

	}

	private void withEvent(Event event) {
		when(_context.getEvent())
				.thenReturn(event);
	}
}
