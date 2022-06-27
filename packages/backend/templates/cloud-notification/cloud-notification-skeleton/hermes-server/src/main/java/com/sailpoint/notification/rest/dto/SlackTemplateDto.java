/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.rest.dto;

import lombok.Builder;
import lombok.Data;

/**
 * SlackTemplateDto
 */
@Data
@Builder
public class SlackTemplateDto {
    private String _text;
    private String _blocks;
    private String _attachments;
    private Boolean _isSubscription;
    private String _notificationType;
    private String _approvalId;
    private String _requestId;
}
