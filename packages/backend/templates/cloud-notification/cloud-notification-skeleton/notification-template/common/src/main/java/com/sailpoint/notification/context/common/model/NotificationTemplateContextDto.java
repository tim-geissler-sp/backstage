/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.context.common.model;

import com.sailpoint.cloud.api.client.model.BaseDto;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * NotificationTemplateContextDto model.
 */
public class NotificationTemplateContextDto extends BaseDto {

	private Map<String, Object> _attributes;

	private OffsetDateTime _created;

	private OffsetDateTime _modified;

	public Map<String, Object> getAttributes() {
		return _attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		_attributes = attributes;
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
