/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.manager.rest.resouce.model;

import java.time.OffsetDateTime;

/**
 * Class that represents a version data for Template DTO.
 */
public class TemplateVersionDto {
	private String _version;
	private TemplateVersionUserInfoDto _createdBy;
	private OffsetDateTime _created;
	private String _note;

	public TemplateVersionDto() {
		_version = "";
		_createdBy = new TemplateVersionUserInfoDto();
		_created = OffsetDateTime.now();
		_note = "";
	}

	public TemplateVersionDto(TemplateVersionUserInfoDto createdBy, OffsetDateTime date, String note) {
		_createdBy = createdBy;
		_created = date;
		_note = note;
	}

	public TemplateVersionDto(TemplateVersionUserInfoDto createdBy, OffsetDateTime date, String note, String id) {
		_createdBy = createdBy;
		_created = date;
		_note = note;
		_version = id;
	}

	public TemplateVersionUserInfoDto getCreatedBy() {
		return _createdBy;
	}

	public void setCreatedBy(TemplateVersionUserInfoDto updatedBy) {
		this._createdBy = updatedBy;
	}

	public OffsetDateTime getCreated() {
		return _created;
	}

	public void setCreated(OffsetDateTime date) {
		this._created = date;
	}

	public String getNote() {
		return _note;
	}

	public void setNote(String note) {
		this._note = note;
	}

	public String getVersion() {
		return _version;
	}

	public void setVersion(String id) {
		_version = id;
	}
}
