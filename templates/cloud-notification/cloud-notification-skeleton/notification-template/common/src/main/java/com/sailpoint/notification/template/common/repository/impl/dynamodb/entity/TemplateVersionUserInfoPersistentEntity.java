/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.repository.impl.dynamodb.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;

/**
 * Info about user created template version.
 */
@DynamoDBDocument
public class TemplateVersionUserInfoPersistentEntity {

	private String _id;
	private String _name;

	public TemplateVersionUserInfoPersistentEntity() {
		_name = "API";
		_id = null;
	}

	public TemplateVersionUserInfoPersistentEntity(String id, String name) {
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
