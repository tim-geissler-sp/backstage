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
 * DTO representing message to be sent to MS Teams Integration Service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class TeamsNotification {
	private Recipient _recipient;
	private String _title;
	private String _text;
	private String _messageJSON;
	private Boolean _isSubscription;
	private String _approvalId;
	private String _requestId;
	private String _notificationType;
	private Map<String, Object> _customFields;
	private String _org;
	private TeamsNotificationAutoApprovalData _autoApprovalData;
}
