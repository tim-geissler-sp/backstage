/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.model.version;


import java.time.OffsetDateTime;

/**
 * Entity contain template version information.
 */
public class TemplateVersionInfo {

	private TemplateVersionUserInfo _updatedBy;
	private OffsetDateTime _date;
	private String _note;

	public TemplateVersionInfo() {
		_updatedBy = new TemplateVersionUserInfo();
		_date = OffsetDateTime.now();
		_note = "";
	}

	public TemplateVersionInfo(TemplateVersionUserInfo updatedBy, OffsetDateTime date, String note) {
		_updatedBy = updatedBy;
		_date = date;
		_note = note;
	}

	public TemplateVersionUserInfo getUpdatedBy() {
		return _updatedBy;
	}

	public void setUpdatedBy(TemplateVersionUserInfo updatedBy) {
		_updatedBy = updatedBy;
	}

	public OffsetDateTime getDate() {
		return _date;
	}

	public void setDate(OffsetDateTime date) {
		_date = date;
	}

	public String getNote() {
		return _note;
	}

	public void setNote(String note) {
		this._note = note;
	}
}
