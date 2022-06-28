/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.api.event.dto;

import com.sailpoint.notification.api.event.ExtendedNotificationEventBuilder;

/**
 * Holds the EXTENDED_NOTIFICATION_EVENT event model.
 */
public class ExtendedNotificationEvent {

	private final Recipient _recipient;

	private final String _medium;

	private final String _from;

	private final String _subject;

	private final String _body;

	private final String _replyTo;

	private final Long _orgId;

	private final String _org;

	private final String _notificationKey;

	private final Boolean _isTemplateEvaluated;

	private final String _requestId;

	public ExtendedNotificationEvent(ExtendedNotificationEventBuilder notificationRenderedBuilder) {
		_recipient = notificationRenderedBuilder.getRecipient();
		_medium = notificationRenderedBuilder.getMedium();
		_from = notificationRenderedBuilder.getFrom();
		_subject = notificationRenderedBuilder.getSubject();
		_body = notificationRenderedBuilder.getBody();
		_replyTo = notificationRenderedBuilder.getReplyTo();
		_orgId = notificationRenderedBuilder.getOrgId();
		_org = notificationRenderedBuilder.getOrg();
		_notificationKey = notificationRenderedBuilder.getNotificationKey();
		_requestId = notificationRenderedBuilder.getRequestId();
		_isTemplateEvaluated = notificationRenderedBuilder.isTemplateEvaluated();
	}

	public Recipient getRecipient() {
		return _recipient;
	}

	public String getMedium() {
		return _medium;
	}

	public String getFrom() {
		return _from;
	}

	public String getSubject() {
		return _subject;
	}

	public String getBody() {
		return _body;
	}

	public String getReplyTo() {
		return _replyTo;
	}

	public Long getOrgId() {
		return _orgId;
	}

	public String getOrg() {
		return _org;
	}

	public String getNotificationKey() {
		return _notificationKey;
	}

	public Boolean isTemplateEvaluated() {
		return _isTemplateEvaluated;
	}

	public String getRequestId() {
		return _requestId;
	}

	public ExtendedNotificationEventBuilder derive() {
		return new ExtendedNotificationEventBuilder().withRecipient(_recipient)
				.withMedium(_medium)
				.withFrom(_from)
				.withSubject(_subject)
				.withBody(_body)
				.withReplyTo(_replyTo)
				.withOrg(_org)
				.withOrgId(_orgId)
				.withNotificationKey(_notificationKey)
				.withTemplateEvaluated(_isTemplateEvaluated)
				.withRequestId(_requestId);
	}

	@Override
	public String toString() {
		//for security reason please to not include body, subject
		return "ExtendedNotificationEvent {" +
				" recipient=" + _recipient +
				", medium='" + _medium + '\'' +
				", from='" + _from + '\'' +
				", replyTo='" + _replyTo + '\'' +
				", orgId=" + _orgId +
				", org='" + _org + '\'' +
				", notificationKey='" + _notificationKey + '\'' +
				", isTemplateEvaluated=" + _isTemplateEvaluated +
				", requestId='" + _requestId + '\'' +
				" }";
	}
}
