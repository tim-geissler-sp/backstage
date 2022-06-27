/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.api.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;

/**
 * DTO representing message to be sent to Slack Integration Service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class SlackNotification {
	private Recipient _recipient;
	private String _text;
	private String _blocks;
	private String _attachments;
	private String _notificationType;
	private String _org;
	private String _approvalId;
	private String _requestId;
	private Boolean _isSubscription;
	private SlackNotificationAutoApprovalData _autoApprovalData;
	private Map<String, Object> _customFields;
}
