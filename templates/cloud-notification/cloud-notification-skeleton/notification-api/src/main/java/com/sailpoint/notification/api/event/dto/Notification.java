/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.api.event.dto;

/**
 * Interface for describe NOTIFICATION_RENDERED event model.
 */
public interface Notification {
	/**
	 * Get Recipient for notification.
	 * @return notification recipient.
	 */
	Recipient getRecipient();

	/**
	 * Set Recipient for notification.
	 */
	void setRecipient(Recipient recipient);

	/**
	 * Get Domain Event.
	 * @return DomainEvent as an Object.
	 * Set Recipient for notification.
	 */
	Object getDomainEvent();
}
