/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.api.event.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

/**
 * Holds the NOTIFICATION_RENDERED event model specific for slack.
 */
@Data
@ToString
@Builder
public class SlackNotificationRendered implements Notification {
	private Recipient _recipient;
	private String _text;
	private String _blocks;
	private String _attachments;
	private String _notificationKey;
	private String _notificationType;
	private String _org;
	private String _approvalId;
	private String _requestId;
	private Object _domainEvent;
	private Boolean _isSubscription;
	private SlackNotificationAutoApprovalData _autoApprovalData;
	private Map<String, Object> _customFields;

	public SlackNotificationRenderedBuilder derive() {
		return SlackNotificationRendered.builder()
				.recipient(_recipient)
				.text(_text)
				.attachments(_attachments)
				.blocks(_blocks)
				.notificationKey(_notificationKey)
				.notificationType(_notificationType)
				.org(_org)
				.approvalId(_approvalId)
				.requestId(_requestId)
				.domainEvent(_domainEvent)
				.isSubscription(_isSubscription)
				.autoApprovalData(_autoApprovalData)
				.customFields(_customFields);
	}

}
