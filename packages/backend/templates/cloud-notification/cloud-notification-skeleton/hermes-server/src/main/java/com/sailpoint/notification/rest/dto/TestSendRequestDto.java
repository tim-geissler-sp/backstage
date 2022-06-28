/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.rest.dto;

import com.sailpoint.cloud.api.client.model.BaseDto;
import com.sailpoint.notification.template.common.model.TemplateMediumDto;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Locale;
import java.util.Map;

/**
 * Dto for Test Notification Request
 */
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
public class TestSendRequestDto extends BaseDto {

	/**
	 * The notification key
	 */
	private String _key;

	/**
	 * The notification medium
	 */
	private TemplateMediumDto _medium;

	/**
	 * The notification locale
	 */
	private Locale _locale;

	/**
	 * The template evaluation context
	 */
	private Map<String, Object> _context;
}
