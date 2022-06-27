/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.event.interest.matching.dto;

import com.sailpoint.iris.client.Event;
import com.sailpoint.notification.sender.common.event.interest.matching.NotificationInterestMatchedBuilder;

/**
 * Class represent Notification Interest Matched Event.
 * Event is result of interest matching step in cross product notification flow.
 */
public class NotificationInterestMatched {

	private final String _notificationId;

	private final String _recipientId;

	private final String _recipientEmail;

	private final String _interestName;

	private final String _categoryName;

	private final Event _domainEvent;

	private final String _notificationKey;

	private boolean _enabled;

	public NotificationInterestMatched(NotificationInterestMatchedBuilder builder) {
		_notificationId = builder.getNotificationId();
		_recipientId = builder.getRecipientId();
		_domainEvent = builder.getDomainEvent();
		_interestName = builder.getInterestName();
		_categoryName = builder.getCategoryName();
		_recipientEmail = builder.getRecipientEmail();
		_notificationKey = builder.getNotificationKey();
		_enabled = builder.isEnabled();
	}

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

	public NotificationInterestMatchedBuilder derive() {
		return new NotificationInterestMatchedBuilder(_notificationId, _domainEvent)
				.withRecipientId(_recipientId)
				.withRecipientEmail(_recipientEmail)
				.withCategoryName(_categoryName)
				.withInterestName(_interestName)
				.withNotificationKey(_notificationKey)
				.withEnabled(_enabled);
	}

	@Override
	public String toString() {
		//for security reason please to not include domain event
		return "NotificationInterestMatched {" +
				" notificationId='" + _notificationId + '\'' +
				", recipientId='" + _recipientId + '\'' +
				", recipientEmail='" + _recipientEmail + '\'' +
				", interestName='" + _interestName + '\'' +
				", categoryName='" + _categoryName + '\'' +
				", notificationKey='" + _notificationKey + '\'' +
				", enabled='" + _enabled + '\'' +
				" }";
	}
}
