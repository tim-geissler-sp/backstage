/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.event.interest.matching;

import com.sailpoint.iris.client.Event;
import com.sailpoint.notification.sender.common.event.interest.matching.dto.NotificationInterestMatched;
import org.apache.commons.lang.StringUtils;

/**
 * Builder class for Notification Interest Matched Event.
 */
public class NotificationInterestMatchedBuilder {

	private String _notificationId;

	private String _recipientId;

	private String _interestName;

	private String _categoryName;

	private Event _domainEvent;

	private String _recipientEmail;

	private String _notificationKey;

	private boolean _enabled;

	public String getNotificationId() {
		return _notificationId;
	}

	public String getRecipientId() {
		return _recipientId;
	}

	public String getRecipientEmail() {
		return _recipientEmail;
	}

	public String getInterestName() {
		return _interestName;
	}

	public String getCategoryName() {
		return _categoryName;
	}

	public Event getDomainEvent() {
		return _domainEvent;
	}

	public String getNotificationKey() {
		return _notificationKey;
	}

	public boolean isEnabled() {
		return _enabled;
	}

	/**
	 * Constructs a new builder with mandatory parameters:
	 * @param notificationId notification id
	 * @param domainEvent copy of original event
	 */
	public NotificationInterestMatchedBuilder(String notificationId, Event domainEvent) {
		_notificationId = notificationId;
		_domainEvent = domainEvent;
		_enabled = true;
	}

	public NotificationInterestMatchedBuilder withRecipientId(String recipientId) {
		_recipientId = recipientId;
		return this;
	}

	public NotificationInterestMatchedBuilder withRecipientEmail(String recipientEmail) {
		_recipientEmail = recipientEmail;
		return this;
	}

	public NotificationInterestMatchedBuilder withInterestName(String interestName) {
		_interestName = interestName;
		return this;
	}

	public NotificationInterestMatchedBuilder withCategoryName(String categoryName) {
		_categoryName = categoryName;
		return this;
	}

	public NotificationInterestMatchedBuilder withNotificationKey(String notificationKey) {
		_notificationKey = notificationKey;
		return this;
	}

	public NotificationInterestMatchedBuilder withEnabled(boolean enabled) {
		_enabled = enabled;
		return this;
	}

	public NotificationInterestMatched build() {
		if (StringUtils.isEmpty(this._notificationKey)) {
			throw new IllegalStateException("The notification key is required");
		} else {
			return new NotificationInterestMatched(this);
		}
	}
}
