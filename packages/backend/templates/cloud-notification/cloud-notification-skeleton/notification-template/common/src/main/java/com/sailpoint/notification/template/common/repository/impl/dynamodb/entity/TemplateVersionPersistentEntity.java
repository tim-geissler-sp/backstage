/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.repository.impl.dynamodb.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;

import java.util.Date;


/**
 * Class for persist template version information in DynamoDB.
 */
@DynamoDBDocument
public class TemplateVersionPersistentEntity {

	private TemplateVersionUserInfoPersistentEntity _updatedBy;
	private Date _date;
	private String _note;

	public TemplateVersionPersistentEntity() {
	}

	public TemplateVersionUserInfoPersistentEntity getUpdatedBy() {
		return _updatedBy;
	}

	public void setUpdatedBy(TemplateVersionUserInfoPersistentEntity updatedBy) {
		this._updatedBy = updatedBy;
	}

	public Date getDate() {
		return _date;
	}

	public void setDate(Date date) {
		this._date = date;
	}

	public String getNote() {
		return _note;
	}

	public void setNote(String note) {
		this._note = note;
	}
}
