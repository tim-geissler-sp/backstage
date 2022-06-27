/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.userpreferences.repository.impl.dynamodb.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

/**
 * An entity for persistence
 */
@DynamoDBTable(tableName = "hermes_user_prefs")
public class UserPreferencesEntity {

	private String _tenant;

	private String _recipientId;

	private String _name;

	private String _phone;

	private String _email;

	private String _brand;

	@DynamoDBHashKey(attributeName = "tenant")
	public String getTenant() {
		return _tenant;
	}

	public void setTenant(String tenant) {
		_tenant = tenant;
	}

	@DynamoDBRangeKey(attributeName = "recipient_id")
	public String getRecipientId() {
		return _recipientId;
	}

	public void setRecipientId(String recipientId) {
		_recipientId = recipientId;
	}

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		_name = name;
	}

	public String getPhone() {
		return _phone;
	}

	public void setPhone(String phone) {
		_phone = phone;
	}

	public String getEmail() {
		return _email;
	}

	public void setEmail(String email) {
		_email = email;
	}

	public String getBrand() {
		return _brand;
	}

	public void setBrand(String brand) {
		_brand = brand;
	}
}
