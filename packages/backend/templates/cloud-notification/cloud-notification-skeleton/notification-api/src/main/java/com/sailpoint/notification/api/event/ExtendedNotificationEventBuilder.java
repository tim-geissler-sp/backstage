/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.api.event;

import com.sailpoint.notification.api.event.dto.ExtendedNotificationEvent;
import com.sailpoint.notification.api.event.dto.Recipient;

/**
 * Class for build ExtendedNotificationEvent
 */
public class ExtendedNotificationEventBuilder {

	private Recipient _recipient;

	private String _medium;

	private String _from;

	private String _subject;

	private String _body;

	private String _replyTo;

	private Long _orgId;

	private String _org;

	private String _notificationKey;

	private Boolean _isTemplateEvaluated;

	private String _requestId;

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

	public ExtendedNotificationEventBuilder withRecipient(Recipient recipient) {
		_recipient = recipient;
		return this;
	}

	public ExtendedNotificationEventBuilder withMedium(String medium) {
		_medium = medium;
		return this;
	}

	public ExtendedNotificationEventBuilder withFrom(String from) {
		_from = from;
		return this;
	}

	public ExtendedNotificationEventBuilder withSubject(String subject) {
		_subject = subject;
		return this;
	}

	public ExtendedNotificationEventBuilder withBody(String body) {
		_body = body;
		return this;
	}

	public ExtendedNotificationEventBuilder withReplyTo(String replyTo) {
		_replyTo = replyTo;
		return this;
	}

	public ExtendedNotificationEventBuilder withOrgId(Long orgId) {
		this._orgId = orgId;
		return this;
	}

	public ExtendedNotificationEventBuilder withOrg(String org) {
		this._org = org;
		return this;
	}

	public ExtendedNotificationEventBuilder withNotificationKey(String notificationKey) {
		this._notificationKey = notificationKey;
		return this;
	}

	public ExtendedNotificationEventBuilder withTemplateEvaluated(Boolean isTemplateEvaluated) {
		this._isTemplateEvaluated = isTemplateEvaluated;
		return this;
	}

	public ExtendedNotificationEventBuilder withRequestId(String requestId) {
		this._requestId = requestId;
		return this;
	}

	public ExtendedNotificationEvent build() {
		return new ExtendedNotificationEvent(this);
	}


}
