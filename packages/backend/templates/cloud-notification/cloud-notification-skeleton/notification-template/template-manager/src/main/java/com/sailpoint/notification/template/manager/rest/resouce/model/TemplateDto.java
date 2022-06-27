/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.manager.rest.resouce.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


import java.time.OffsetDateTime;

/**
 * Class that represents a configuration Template DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TemplateDto extends TemplateDtoDefault {

	private String _id;

	private OffsetDateTime _created;

	private OffsetDateTime _modified;

	public String getId() {
		return _id;
	}

	public void setId(String id) {
		_id = id;
	}

	public OffsetDateTime getCreated() {
		return _created;
	}

	public void setCreated(OffsetDateTime created) {
		_created = created;
	}

	public OffsetDateTime getModified() {
		return _modified;
	}

	public void setModified(OffsetDateTime modified) {
		_modified = modified;
	}
}
