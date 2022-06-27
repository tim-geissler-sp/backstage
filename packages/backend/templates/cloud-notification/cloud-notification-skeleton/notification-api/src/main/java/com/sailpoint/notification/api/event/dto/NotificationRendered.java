/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.api.event.dto;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 * NotificationRendered domain object represents a rendered email notification
 */
@Data
@Builder
public class NotificationRendered implements Notification {

	private Recipient _recipient;
	private String _medium;
	private String _from;
	private String _subject;
	private String _body;
	private String _replyTo;
	private String _notificationKey;
	private Object _domainEvent;

	public NotificationRenderedBuilder derive() {
		return NotificationRendered.builder()
				.recipient(_recipient)
				.medium(_medium)
				.from(_from)
				.subject(_subject)
				.body(_body)
				.replyTo(_replyTo)
				.notificationKey(_notificationKey)
				.domainEvent(_domainEvent);
	}

	@Override
	public String toString() {
		//for security reason please to not include domain event, subject, body
		return "NotificationRendered {" +
				" recipient=" + _recipient +
				", medium='" + _medium + '\'' +
				", from='" + _from + '\'' +
				", replyTo='" + _replyTo + '\'' +
				", notificationKey='" + _notificationKey + '\'' +
				" }";
	}
}
