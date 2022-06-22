/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.rest.dto;

import lombok.Builder;
import lombok.Data;

/**
 * TeamsTemplateDto
 */
@Data
@Builder
public class TeamsTemplateDto {
    private String _title;
    private String _text;
    private String _messageJson;
    private String _notificationType;
    private String _approvalId;
    private String _requestId;
    private Boolean _isSubscription;
}
