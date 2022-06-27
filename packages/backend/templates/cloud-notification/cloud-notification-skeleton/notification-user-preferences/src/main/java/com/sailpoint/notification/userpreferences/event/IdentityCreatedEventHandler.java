/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.userpreferences.event;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.server.EventHandler;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.api.event.RecipientBuilder;
import com.sailpoint.notification.userpreferences.dto.UserPreferences;
import com.sailpoint.notification.userpreferences.event.dto.IdentityCreatedEvent;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;
import com.sailpoint.notification.userpreferences.service.UserPreferencesDebugService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.Optional;

/**
 *  Handler for IdentityCreatedEvent from sp-identity-event; these events should be published when the app encounters
 *  a new identity. When we see a new identity we want to write a new user preference records.
 */
public class IdentityCreatedEventHandler implements EventHandler {

	private static final Log _log = LogFactory.getLog(IdentityCreatedEventHandler.class);

	public static final String IDENTITY_EMAIL = "email";
	public static final String IDENTITY_PHONE = "phone";
	public static final String IDENTITY_DISPLAY_NAME = "displayName";
	public static final String BRAND = "brand";

	private UserPreferencesRepository _userPreferencesRepository;
	private UserPreferencesDebugService _userPreferencesDebugService;

	@Inject
	@VisibleForTesting
	IdentityCreatedEventHandler(UserPreferencesRepository userPreferencesRepository, UserPreferencesDebugService userPreferencesDebugService) {
		_userPreferencesRepository = userPreferencesRepository;
		_userPreferencesDebugService = userPreferencesDebugService;
	}

	@Override
	public void handleEvent(EventHandlerContext eventHandlerContext) {
		Event event = eventHandlerContext.getEvent();
		_log.info("Handling " + event.getType());
		String id;
		Map attributes;
		try {
			IdentityCreatedEvent identityCreatedEvent = event.getContent(IdentityCreatedEvent.class);
			id = identityCreatedEvent.getIdentity().getId();
			attributes = identityCreatedEvent.getAttributes();
		} catch(Exception e) {
			_log.error("Failed to retrieve identity id/attributes", e);
			return;
		}

		createUserPreference(id, attributes);
		processDebugEvent(event, id);
	}

	private UserPreferences createUserPreference(String id, Map<String, Object> attributesMap) {

		if (id == null) {
			_log.error("External id cannot be null");
			return null;
		}
		UserPreferences user = new UserPreferences.UserPreferencesBuilder()
									.withRecipient((new RecipientBuilder()).withId(id)
											.withEmail(normalizeAttribute(attributesMap, IDENTITY_EMAIL))
											.withName(normalizeAttribute(attributesMap, IDENTITY_DISPLAY_NAME))
											.withPhone(normalizeAttribute(attributesMap, IDENTITY_PHONE))
											.build())
									.withBrand(Optional.ofNullable(normalizeAttribute(attributesMap, BRAND)))
									.build();
		_userPreferencesRepository.create(user);
		return user;
	}

	private String normalizeAttribute(Map<String, Object> map, String key) {
		if (map.get(key) != null) {
			return map.get(key).toString();
		}
		return null;
	}

	/**
	 * Process debug event.
	 *
	 * @param event Iris event.
	 * @param externalId id of the recipient.
	 */
	private void processDebugEvent(Event event, String externalId) {
		Optional<String> debugHeader = event.getHeader(UserPreferencesDebugService.REDIS_USER_DEBUG_KEY);
		if (debugHeader.isPresent()) {
			_log.info("Handling debug user " + event.getType() + " event id " + event.getId());
			UserPreferences userPreferences = _userPreferencesDebugService.getUserPreferences(externalId);
			_userPreferencesDebugService.writeToStore(debugHeader.get(), userPreferences.getRecipient().getId());
		}
	}
}
