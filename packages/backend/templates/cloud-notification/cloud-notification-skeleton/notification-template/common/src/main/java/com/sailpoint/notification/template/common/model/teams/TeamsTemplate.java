/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.model.teams;

import com.sailpoint.notification.api.event.dto.TeamsNotificationAutoApprovalData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Entity that represents a teams notification template.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamsTemplate {
	private String _title;
	private String _text;
	private String _messageJson;
	private Boolean _isSubscription;
	private String _approvalId;
	private String _requestId;
	private String _notificationType;
	private Map<String, Object> _customFields;
	private TeamsNotificationAutoApprovalData _autoApprovalData;
}
