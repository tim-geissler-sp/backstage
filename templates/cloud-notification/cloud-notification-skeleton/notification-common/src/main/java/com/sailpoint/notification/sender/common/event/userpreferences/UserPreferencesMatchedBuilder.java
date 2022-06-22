/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.event.userpreferences;

import com.sailpoint.iris.client.Event;
import com.sailpoint.notification.api.event.dto.Recipient;
import com.sailpoint.notification.sender.common.event.userpreferences.dto.UserPreferencesMatched;
import org.apache.commons.lang.StringUtils;

import java.util.Optional;

/**
 * Builder class for Notification User Preferences Matched Event.
 */
public class UserPreferencesMatchedBuilder {

	private Recipient _recipient;

	private String _medium;

	private String _template;

	private String 	_notificationKey;

	private Event _domainEvent;

	private Optional<String> _brand;

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
		return _brand;
	}

	public UserPreferencesMatchedBuilder withRecipient(Recipient recipient) {
		_recipient = recipient;
		return this;
	}

	public UserPreferencesMatchedBuilder withMedium(String medium) {
		_medium = medium;
		return this;
	}

	public UserPreferencesMatchedBuilder withTemplate(String template) {
		_template = template;
		return this;
	}

	public UserPreferencesMatchedBuilder withNotificationKey(String notificationKey) {
		_notificationKey = notificationKey;
		return this;
	}

	public UserPreferencesMatchedBuilder withDomainEvent(Event domainEvent) {
		_domainEvent = domainEvent;
		return this;
	}

	public UserPreferencesMatchedBuilder withBrand(Optional<String> brand) {
		_brand = brand;
		return this;
	}

	public UserPreferencesMatched build() {
		if (StringUtils.isEmpty(this._notificationKey)) {
			throw new IllegalStateException("The notification key are required");
		} else {
			return new UserPreferencesMatched(this);
		}
	}
}
