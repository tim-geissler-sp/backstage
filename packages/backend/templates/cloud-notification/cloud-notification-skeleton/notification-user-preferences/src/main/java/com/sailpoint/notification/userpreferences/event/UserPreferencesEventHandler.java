/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.event;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.server.EventHandler;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.api.event.RecipientBuilder;
import com.sailpoint.notification.api.event.dto.NotificationMedium;
import com.sailpoint.notification.api.event.dto.Recipient;
import com.sailpoint.notification.sender.common.event.interest.matching.dto.NotificationInterestMatched;
import com.sailpoint.notification.sender.common.event.userpreferences.UserPreferencesMatchedBuilder;
import com.sailpoint.notification.sender.common.event.userpreferences.dto.UserPreferencesMatched;
import com.sailpoint.notification.orgpreferences.repository.TenantPreferencesRepository;
import com.sailpoint.notification.orgpreferences.repository.dto.PreferencesDto;
import com.sailpoint.notification.userpreferences.dto.UserPreferences;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;
import com.sailpoint.notification.userpreferences.service.UserPreferencesDebugService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 *  User Preferences EventHandler. Class for handle iris events during user preferences phase.
 */
@Singleton
public class UserPreferencesEventHandler implements EventHandler {

	private static final Log _log = LogFactory.getLog(UserPreferencesEventHandler.class);

	private UserPreferencesDebugService _userPreferencesDebugService;

	private UserPreferencesRepository _userPreferencesRepository;

	private TenantPreferencesRepository _tenantPreferencesRepository;

	private Provider<EventService> _eventService;

	@Inject
	@VisibleForTesting
	UserPreferencesEventHandler(UserPreferencesDebugService userPreferencesDebugService,
								UserPreferencesRepository userPreferencesRepository,
								Provider<EventService> eventService,
								TenantPreferencesRepository tenantPreferencesRepository) {
		_userPreferencesDebugService = userPreferencesDebugService;
		_userPreferencesRepository = userPreferencesRepository;
		_eventService = eventService;
		_tenantPreferencesRepository = tenantPreferencesRepository;
	}

	@Override
	public void handleEvent(EventHandlerContext context) {
		try {
			Event event = context.getEvent();
			NotificationInterestMatched interestMatched = event.getContent(NotificationInterestMatched.class);
			_log.info("Handling " + event.getType() + " event " + event.getId() +
					" " + interestMatched.toString());

			enabledMediums(interestMatched).forEach(medium -> {
				processDebugEvent(event, interestMatched, medium);
				publishUserPreferencesMatched(event, interestMatched, medium);
			});

		} catch (Exception e) {
			_log.error("Error processing event " + context.getEvent().getType(), e);
		}
	}

	/**
	 * Process debug event.
	 *
	 * @param event Iris event
	 * @param interestMatched interest matched parent event
	 */
	private void processDebugEvent(Event event, NotificationInterestMatched interestMatched, NotificationMedium medium) {
		Optional<String> debugHeader = event.getHeader(UserPreferencesDebugService.REDIS_USER_DEBUG_KEY);
		if (debugHeader.isPresent()) {
			_log.info("Handling debug " + event.getType() + " event id " + event.getId() + " for medium " + medium.toString());
			UserPreferences userPreferences = _userPreferencesDebugService.getUserPreferences(interestMatched.getRecipientId());
			String key = debugHeader.get()+":"+medium.toString();
			_userPreferencesDebugService.writeToStore(key, userPreferences.getRecipient().getEmail());
		}
	}

	/**
	 * Publish UserPreferencesMatched event
	 *
	 * @param event Iris event
	 * @param interestMatched interest matched parent event
	 */
	private void publishUserPreferencesMatched(Event event, NotificationInterestMatched interestMatched, NotificationMedium medium) {
		if(!event.getHeader(UserPreferencesDebugService.REDIS_USER_DEBUG_KEY).isPresent()) {

			Recipient recipient = null;
			Optional<String> brand = Optional.empty();
			UserPreferences userPreferences = _userPreferencesRepository.findByRecipientId(interestMatched.getRecipientId());
			if(userPreferences != null) {
				recipient = userPreferences.getRecipient();
				brand = userPreferences.getBrand();
				//check if email provided in event and different from user's can be the case if Send to Test Address set.
				if(interestMatched.getRecipientEmail() != null &&
						!interestMatched.getRecipientEmail().equals(recipient.getEmail())) {
					_log.info("Replacing recipient email: " + recipient.getEmail() + " with email: " + interestMatched.getRecipientEmail());
					recipient = recipient.derive()
							.withEmail(interestMatched.getRecipientEmail())
							.build();
				}
			} else {
				if(interestMatched.getRecipientEmail() != null) {
					_log.warn("Building recipient with ID and Email in Interest Matched Event");
					recipient = new RecipientBuilder()
							.withEmail(interestMatched.getRecipientEmail())
							.withId(interestMatched.getRecipientId())
							.build();
				}
				else if(interestMatched.getRecipientId() != null && medium != NotificationMedium.EMAIL) {
					_log.warn("Building recipient with ID only.  Using ID present in Interest Matched Notification Event");
					recipient = new RecipientBuilder()
							.withId(interestMatched.getRecipientId())
							.build();
				}
			}

			//If recipient is still null, log an error and end event chain
			if (recipient == null) {
				_log.error("Recipient " + interestMatched.getRecipientId() + " could not be retrieved or constructed");
				return;
			}

			UserPreferencesMatched userPreferencesMatched = new UserPreferencesMatchedBuilder()
					.withRecipient(recipient)
					.withDomainEvent(event)
					.withMedium(medium.toString().toLowerCase())
					.withNotificationKey(interestMatched.getNotificationKey())
					.withBrand(brand)
					.build();
			_eventService.get().publish(IdnTopic.NOTIFICATION, EventType.NOTIFICATION_USER_PREFERENCES_MATCHED,
					userPreferencesMatched);
		}
	}

	/**
	 * Check Interest Matched, Org Preferences, User Preferences if enabled (oprt in/out).
	 * @param interestMatched processing Interest Matcher.
	 * @return true if interest, org, and user preferences enabled.
	 */
	private Set<NotificationMedium> enabledMediums(NotificationInterestMatched interestMatched) {
		//TODO: add User Preferences. For now interest and tenant preference will be used to decide.

		Set<NotificationMedium> enabledMediums = new HashSet<>();

		//Interest Opt-in is in the interestMatched event
		boolean interestEnabled = interestMatched.isEnabled();

		//Tenant Opt-in is derived from dynamo
		RequestContext rc = RequestContext.ensureGet();
		PreferencesDto preferencesDto = _tenantPreferencesRepository.findOneForTenantAndKey(rc.getOrg(), interestMatched.getNotificationKey());
		if (preferencesDto != null) {
			if (preferencesDto.getMediums() != null) {
				//A tenant preference was found so we will obey it
				enabledMediums = new HashSet<>(preferencesDto.getMediums());
			}
			_log.info("Tenant Preference: " + enabledMediums);
		} else {
			//No tenant preference found, so we will gate on interest
			if (interestEnabled) {
				if(interestMatched.getCategoryName() != null) {
					String[] names = interestMatched.getCategoryName().split(",");
					for(String s : names) {
						enabledMediums.add(NotificationMedium.valueOf( (s.trim().toUpperCase()) ));
					}
				} else { //default to email
					enabledMediums.add(NotificationMedium.EMAIL);
				}
			}
			_log.info("Interest Preference: " + enabledMediums);
		}
		return enabledMediums;
	}
}
