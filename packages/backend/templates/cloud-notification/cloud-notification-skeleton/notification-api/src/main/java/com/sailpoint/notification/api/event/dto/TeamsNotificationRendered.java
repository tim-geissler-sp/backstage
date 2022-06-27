/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.api.event.dto;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.Map;

/**
 * Holds the NOTIFICATION_RENDERED event model specific for MS Teams.
 */
@Data
@ToString
@Builder
public class TeamsNotificationRendered implements Notification {
	private Recipient _recipient;
	private String _title;
	private String _text;
	private String _messageJSON;
	private String _notificationKey;
	private Object _domainEvent;
	private Boolean _isSubscription;
	private String _approvalId;
	private String _requestId;
	private String _notificationType;
	private String _org;
	private TeamsNotificationAutoApprovalData _autoApprovalData;
	private Map<String, Object> _customFields;

	public TeamsNotificationRendered.TeamsNotificationRenderedBuilder derive() {
		return TeamsNotificationRendered.builder()
				.recipient(_recipient)
				.text(_text)
				.title(_title)
				.messageJSON(_messageJSON)
				.notificationKey(_notificationKey)
				.domainEvent(_domainEvent)
				.isSubscription(_isSubscription)
				.approvalId(_approvalId)
				.requestId(_requestId)
				.notificationType(_notificationType)
				.customFields(_customFields)
				.org(_org)
				.autoApprovalData(_autoApprovalData);
	}
}
