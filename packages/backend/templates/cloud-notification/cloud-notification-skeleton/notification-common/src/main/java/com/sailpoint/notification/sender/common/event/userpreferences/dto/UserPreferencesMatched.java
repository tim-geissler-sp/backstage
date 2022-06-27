/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.event.userpreferences.dto;

import com.sailpoint.iris.client.Event;
import com.sailpoint.notification.api.event.dto.Recipient;
import com.sailpoint.notification.sender.common.event.userpreferences.UserPreferencesMatchedBuilder;

import java.util.Optional;

/**
 * Class represent Notification User Preferences Matched Event.
 * Event is result of user preferences matching step in cross product notification flow.
 */
public class UserPreferencesMatched {

	private final Recipient _recipient;

	private final String _medium;

	private final String _template;

	private final String _notificationKey;

	private final Event _domainEvent;

	private final Optional<String> _brand;

	public UserPreferencesMatched(UserPreferencesMatchedBuilder builder) {
		_recipient = builder.getRecipient();
		_medium = builder.getMedium();
		_template = builder.getTemplate();
		_domainEvent = builder.getDomainEvent();
		_notificationKey = builder.getNotificationKey();
		_brand = builder.getBrand();
	}

	public Recipient getRecipient() {
		return _recipient;
	}

	public String getMedium() {
		return _medium;
	}

	public String getTemplate() {
		return _template;
	}

	public String getNotificationKey() {
		return _notificationKey;
	}

	public Event getDomainEvent() {
		return _domainEvent;
	}

	public Optional<String> getBrand() {
		if (_brand != null) {
			return _brand;
		} else {
			return Optional.empty();
		}
	}

	public UserPreferencesMatchedBuilder derive() {
		return new UserPreferencesMatchedBuilder()
				.withRecipient(_recipient)
				.withMedium(_medium)
				.withTemplate(_template)
				.withNotificationKey(_notificationKey)
				.withDomainEvent(_domainEvent)
				.withBrand(_brand);
	}

	@Override
	public String toString() {
		//for security reason please to not include domain event
		return "UserPreferencesMatched {" +
				" recipient=" + _recipient +
				", medium='" + _medium + '\'' +
				", template='" + _template + '\'' +
				", notificationKey='" + _notificationKey + '\'' +
				", brand='" + _brand + '\'' +
				" }";
	}
}
