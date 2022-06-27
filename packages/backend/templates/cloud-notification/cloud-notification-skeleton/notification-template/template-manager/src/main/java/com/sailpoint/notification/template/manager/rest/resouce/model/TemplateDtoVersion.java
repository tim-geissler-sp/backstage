/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.manager.rest.resouce.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;

/**
 * Class that represents a configuration Template DTO with Version Info.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TemplateDtoVersion extends TemplateDto {

	private TemplateVersionDto _versionInfo;

	public TemplateVersionDto getVersionInfo() {
		return _versionInfo;
	}

	public void setVersionInfo(TemplateVersionDto versionInfo) {
		_versionInfo = versionInfo;
	}
}