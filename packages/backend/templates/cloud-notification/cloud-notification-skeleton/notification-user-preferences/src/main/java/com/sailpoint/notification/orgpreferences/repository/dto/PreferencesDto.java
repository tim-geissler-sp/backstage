/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.orgpreferences.repository.dto;


import com.sailpoint.cloud.api.client.model.BaseDto;
import com.sailpoint.notification.api.event.dto.NotificationMedium;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * A Preferences Dto
 */
public class PreferencesDto extends BaseDto {

	private String _key;

	private List<NotificationMedium> _mediums;

	private OffsetDateTime _modified;

	public String getKey() {
		return _key;
	}

	public void setKey(String key) {
		_key = key;
	}

	public List<NotificationMedium> getMediums() {
		return _mediums;
	}

	public void setMediums(List<NotificationMedium> mediums) {
		_mediums = mediums;
	}

	public OffsetDateTime getModified() {
		return _modified;
	}

	public void setModified(OffsetDateTime modified) {
		_modified = modified;
	}

	@Override
	public String toString() {
		return "PreferencesDto{" +
				"notificationKey='" + _key + '\'' +
				", mediums='" + _mediums + '\'' +
				", modified=" + _modified +
				'}';
	}
}