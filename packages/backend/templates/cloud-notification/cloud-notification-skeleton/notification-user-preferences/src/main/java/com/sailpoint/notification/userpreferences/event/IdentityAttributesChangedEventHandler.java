/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.userpreferences.event;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.server.EventHandler;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.userpreferences.dto.UserPreferences;
import com.sailpoint.notification.userpreferences.event.dto.AttributeChange;
import com.sailpoint.notification.userpreferences.event.dto.IdentityAttributesChangedEvent;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;
import com.sailpoint.notification.userpreferences.service.UserPreferencesDebugService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 *  Handler for IdentityAttributesChangedEvent events.  See SP-identity repo or the associated unit test for
 *  examples of these events.  Handling involves updating hermes_user_pref
 */
public class IdentityAttributesChangedEventHandler implements EventHandler {

	private static final Log _log = LogFactory.getLog(IdentityAttributesChangedEventHandler.class);

	public static final String IDENTITY_MAP_EMAIL = "email";
	public static final String IDENTITY_MAP_PHONE = "phone";
	public static final String IDENTITY_MAP_DISPLAY_NAME = "displayName";
	public static final String BRAND = "brand";

	private static final List<String> RELEVANT_ATTRIBUTES = Arrays.asList(new String[]{IDENTITY_MAP_DISPLAY_NAME, IDENTITY_MAP_PHONE, IDENTITY_MAP_EMAIL, BRAND});

	private UserPreferencesRepository _userPreferencesRepository;
	private UserPreferencesDebugService _userPreferencesDebugService;

	@Inject
	@VisibleForTesting
	IdentityAttributesChangedEventHandler(UserPreferencesRepository userPreferencesRepository, UserPreferencesDebugService userPreferencesDebugService) {
		_userPreferencesRepository = userPreferencesRepository;
		_userPreferencesDebugService = userPreferencesDebugService;
	}

	@Override
	public void handleEvent(EventHandlerContext eventHandlerContext) {
		Event event = eventHandlerContext.getEvent();
		_log.info("Handling " + event.getType());
		String id;
		List<AttributeChange> changes;
		try {
			IdentityAttributesChangedEvent identityAttributesChangedEvent = event.getContent(IdentityAttributesChangedEvent.class);
			id = identityAttributesChangedEvent.getIdentity().getId();
			changes = identityAttributesChangedEvent.getChanges();
		} catch(Exception e) {
			_log.error("Failed to retrieve identity id/changes", e);
			return;
		}

		updateUserPreference(id, changes);
		processDebugEvent(event, id);
	}

	private UserPreferences updateUserPreference(String id, List<AttributeChange> changes) {
		if (id == null) {
			_log.warn("id cannot be null");
			return null;
		}
		if (changes == null) {
			_log.warn("changes cannot be null");
			return null;
		}

		List<AttributeChange> relevantChanges = new ArrayList<>();
		for(AttributeChange change : changes) {
			if (RELEVANT_ATTRIBUTES.contains(change.getAttribute())) {
				relevantChanges.add(change);
			}
		}
		if (relevantChanges.size() == 0) {
			_log.debug("no relevant changes");
			return null;
		}

		UserPreferences user = _userPreferencesRepository.findByRecipientId(id);
		if (user == null)
		{
			_log.error(String.format("cannot update identity with id %s since it could not be found", id));
			return null;
		}

		for(AttributeChange change : changes)
		{
			switch(change.getAttribute())
			{
				case IDENTITY_MAP_DISPLAY_NAME:
					user.getRecipient().setName(normalizeAttribute(change.getNewValue()));
					break;
				case IDENTITY_MAP_EMAIL:
					user.getRecipient().setEmail(normalizeAttribute(change.getNewValue()));
					break;
				case IDENTITY_MAP_PHONE:
					user.getRecipient().setPhone(normalizeAttribute(change.getNewValue()));
					break;
				case BRAND:
					user = user.derive().withBrand(Optional.ofNullable((String)change.getNewValue())).build();
					break;
			}
		}
		_userPreferencesRepository.create(user);
		return user;
	}

	private String normalizeAttribute( Object newValue) {
		if (newValue != null) {
			return newValue.toString();
		}
		return null;
	}

	/**
	 * Process debug event.
	 *
	 * @param event Iris event.
	 * @param externalId id of the recieptent.
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
