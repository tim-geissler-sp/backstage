/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.manager.rest.resouce.model;

/**
 * Class that represents a user version info for Template DTO.
 */
public class TemplateVersionUserInfoDto {
	private String _id;
	private String _name;

	public TemplateVersionUserInfoDto() {
		_id = null;
		_name = "API";
	}

	public TemplateVersionUserInfoDto(String id, String name) {
		_id = id;
		_name = name;
	}

	public String getId() {
		return _id;
	}

	public void setId(String id) {
		this._id = id;
	}

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		this._name = name;
	}
}
