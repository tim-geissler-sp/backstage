/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.service;

import com.google.inject.Provider;
import com.sailpoint.atlas.OrgData;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import com.sailpoint.atlas.security.AdministratorSecurityContext;
import com.sailpoint.iris.client.Event;
import com.sailpoint.notification.api.event.RecipientBuilder;
import com.sailpoint.notification.userpreferences.dto.UserPreferences;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for UserPreferencesDebugService.
 */
public class UserPreferencesDebugServiceTest {

	@Mock
	Provider<EventService> _esProvider;

	@Mock
	EventService _eventService;

	@Mock
	Provider<RedisPool> _redisPoolProvider;

	@Mock
	RedisPool _redisPool;

	@Mock
	UserPreferencesRepository _userPreferencesRepository;

	private UserPreferencesDebugService _userPreferencesDebugService;


	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		when(_esProvider.get()).thenReturn(_eventService);
		when(_redisPoolProvider.get()).thenReturn(_redisPool);

		_userPreferencesDebugService = new UserPreferencesDebugService(_esProvider,
				_redisPoolProvider, _userPreferencesRepository);
	}

	@Test
	public void testPublishKafkaEvent() {
		Assert.assertNotNull(_userPreferencesDebugService.publishKafkaEvent(IdnTopic.NOTIFICATION, "test_event", "{}"));
		verify(_eventService, times(1)).publishAsync(eq(IdnTopic.NOTIFICATION), any(Event.class));
	}

	@Test
	public void getUserPreferencesTest() {
		// Given UserPreference entry
		final String recipientId = UUID.randomUUID().toString();
		givenUserPreferencesFindByRecipientIdMock(recipientId);

		// When
		UserPreferences userPreferences = _userPreferencesDebugService.getUserPreferences(recipientId);

		Assert.assertNotNull(userPreferences);
		Assert.assertEquals(recipientId, userPreferences.getRecipient().getId());
	}

	@Test
	public void createDebugUserPreferenceTest() {
		givenRequestContext();

		// When create entry
		final String id = _userPreferencesDebugService.createDebugUserPreference();

		// Then verify
		verify(_userPreferencesRepository, times(1)).create(any(UserPreferences.class));
		Assert.assertNotNull(id);
	}

	@Test
	public void listUserPreferencesTest() {
		givenRequestContext();

		// Given UserPreference entry
		final String recipientId = UUID.randomUUID().toString();
		givenUserPreferencesFindAllByTenantMock(recipientId);

		// When
		List<UserPreferences> userPreferencesList = _userPreferencesDebugService.list("dev__acme-solar");

		// Then
		Assert.assertNotNull(userPreferencesList);
		Assert.assertEquals(1, userPreferencesList.size());
		Assert.assertNotNull(userPreferencesList.get(0).getRecipient());
		Assert.assertEquals(recipientId, userPreferencesList.get(0).getRecipient().getId());
	}

	private void givenRequestContext() {
		OrgData orgData = new OrgData();
		orgData.setPod("dev");
		orgData.setOrg("acme-solar");

		RequestContext requestContext = new RequestContext();
		requestContext.setSecurityContext(new AdministratorSecurityContext());
		requestContext.setOrgData(orgData);
		RequestContext.set(requestContext);
	}

	private void givenUserPreferencesFindAllByTenantMock(String recipientId) {
		when(_userPreferencesRepository.findAllByTenant(anyString())).thenAnswer(invocation -> {

			UserPreferences userPreferences = new UserPreferences.UserPreferencesBuilder()
					.withRecipient(new RecipientBuilder()
							.withId(recipientId)
							.withEmail("debug@email.com")
							.withPhone("debug-phone")
							.build())
					.build();

			return Arrays.asList(userPreferences);
		});
	}

	private void givenUserPreferencesFindByRecipientIdMock(String recipientId) {
		when(_userPreferencesRepository.findByRecipientId(recipientId)).thenAnswer(invocation -> {

			UserPreferences userPreferences = new UserPreferences.UserPreferencesBuilder()
					.withRecipient(new RecipientBuilder()
							.withId(recipientId)
							.withEmail("debug@email.com")
							.withPhone("debug-phone")
							.build())
					.build();

			return userPreferences;
		});
	}
}