/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.model.slack;

import com.sailpoint.notification.api.event.dto.SlackNotificationAutoApprovalData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Entity that represents a slack notification template.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SlackTemplate {
	private String _text;
	private String _blocks;
	private String _attachments;
	private String _notificationType;
	private String _approvalId;
	private String _requestId;
	private Boolean _isSubscription;
	private SlackNotificationAutoApprovalData _autoApprovalData;
	private Map<String, Object> _customFields;
}
