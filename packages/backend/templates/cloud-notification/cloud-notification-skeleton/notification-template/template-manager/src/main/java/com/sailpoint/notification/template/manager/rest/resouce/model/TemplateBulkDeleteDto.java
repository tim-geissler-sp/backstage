/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.manager.rest.resouce.model;

import com.sailpoint.notification.template.common.model.TemplateMediumDto;

/**
 * Template Delete Dto. Dto used for bulk-delete templates.
 */
public class TemplateBulkDeleteDto {
	private String _key;
	private TemplateMediumDto _medium;
	private String _locale;

	public TemplateBulkDeleteDto() {
		_key = null;
		_medium = null;
		_locale = null;
	}

	public TemplateBulkDeleteDto(String key) {
		_key = key;
		_medium = null;
		_locale = null;
	}

	public String getKey() {
		return _key;
	}

	public void setKey(String name) {
		_key = name;
	}

	public TemplateMediumDto getMedium() {
		return _medium;
	}

	public void setMedium(TemplateMediumDto medium) {
		_medium = medium;
	}

	public String getLocale() {
		return _locale;
	}

	public void setLocale(String locale) {
		_locale = locale;
	}
}
