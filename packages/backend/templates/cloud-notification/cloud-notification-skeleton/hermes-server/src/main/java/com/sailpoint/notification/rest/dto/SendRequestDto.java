/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.rest.dto;

import com.sailpoint.cloud.api.client.model.BaseDto;
import com.sailpoint.notification.template.common.model.TemplateMediumDto;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateDtoDefault;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Locale;
import java.util.Map;

/**
 * Dto for Notification Request
 */
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
public class SendRequestDto extends BaseDto {
	/**
	 * The recipient ID
	 */
	private String _recipientId;

	/**
	 * The notification medium
	 */
	private TemplateMediumDto _medium;

	/**
	 * The notification key
	 */
	private String _key;

	/**
	 * The notification locale
	 */
	private Locale _locale;

	/**
	 * The email template
	 */
	private EmailTemplateDto _emailTemplate;

	/**
	 * The slack template
	 */
	private SlackTemplateDto _slackTemplate;

	/**
	 * The teams template
	 */
	private TeamsTemplateDto _teamsTemplate;

	/**
	 * The template evaluation context
	 */
	private Map<String, Object> _context;
}
