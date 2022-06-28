/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.model.version;

/**
 * Info about user created template version.
 */
public class TemplateVersionUserInfo {

	private String _id;
	private String _name;

	public TemplateVersionUserInfo() {
		_name = "API";
		_id = null;
	}

	public TemplateVersionUserInfo(String id, String name) {
		_id = id;
		_name = name;
	}

	public String getId() {
		return _id;
	}

	public void setId(String _id) {
		this._id = _id;
	}

	public String getName() {
		return _name;
	}

	public void setName(String _name) {
		this._name = _name;
	}
}
