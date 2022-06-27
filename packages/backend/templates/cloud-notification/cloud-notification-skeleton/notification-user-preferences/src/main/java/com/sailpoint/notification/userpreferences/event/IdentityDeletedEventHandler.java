/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.userpreferences.event;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.server.EventHandler;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.userpreferences.event.dto.IdentityDeletedEvent;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;
import com.sailpoint.notification.userpreferences.service.UserPreferencesDebugService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.Optional;

public class IdentityDeletedEventHandler implements EventHandler {

	private static final Log _log = LogFactory.getLog(IdentityDeletedEventHandler.class);

	private UserPreferencesRepository _userPreferencesRepository;
	private UserPreferencesDebugService _userPreferencesDebugService;

	@Inject
	@VisibleForTesting
	IdentityDeletedEventHandler(UserPreferencesRepository userPreferencesRepository, UserPreferencesDebugService userPreferencesDebugService) {
		_userPreferencesRepository = userPreferencesRepository;
		_userPreferencesDebugService = userPreferencesDebugService;
	}

	@Override
	public void handleEvent(EventHandlerContext eventHandlerContext) {
		Event event = eventHandlerContext.getEvent();
		_log.info("Handling " + event.getType() + " " + event.getContent(Map.class));
		String id;
		try {
			IdentityDeletedEvent identityDeletedEvent = event.getContent(IdentityDeletedEvent.class);
			id = identityDeletedEvent.getIdentity().getId();
		} catch(Exception e) {
			_log.error("Failed to retrieve identity id", e);
			return;
		}

		delete(id);
		processDebugEvent(event, id);
	}

	private void delete(String id) {
		if (id == null) {
			_log.error("External id cannot be null");
			return;
		}
		_userPreferencesRepository.deleteByRecipientId(id);
	}

	/**
	 * Process debug event.
	 *
	 * @param event Iris event.
	 * @param recipientId user id.
	 */
	private void processDebugEvent(Event event, String recipientId) {
		Optional<String> debugHeader = event.getHeader(UserPreferencesDebugService.REDIS_USER_DEBUG_KEY);
		if (debugHeader.isPresent()) {
			_log.info("Handling debug user " + event.getType() + " event id " + event.getId());
			if(_userPreferencesDebugService.getUserPreferences(recipientId) == null) {
				_userPreferencesDebugService.writeToStore(debugHeader.get(), recipientId);
			}
		}
	}
}
