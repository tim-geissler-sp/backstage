/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.rest.dto;

import lombok.Builder;
import lombok.Data;

/**
 * EmailTemplateDto
 */
@Data
@Builder
public class EmailTemplateDto {
    private String _subject;
    private String _header;
    private String _body;
    private String _footer;
    private String _from;
    private String _replyTo;
}
