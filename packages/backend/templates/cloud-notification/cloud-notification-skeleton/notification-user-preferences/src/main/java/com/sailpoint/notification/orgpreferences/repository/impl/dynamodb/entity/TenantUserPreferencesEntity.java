/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.orgpreferences.repository.impl.dynamodb.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

/**
 * A Tenant Preferences persistence entity
 */
@DynamoDBTable(tableName = "hermes_tenant_user_prefs")
public class TenantUserPreferencesEntity  extends BasePreferencesEntity {

	public final static String USER_NAME = "user_id";
	public final static String TENANT_USER_INDEX_NAME = "tenantUserIndex";

	private String _userId;

	@DynamoDBIndexRangeKey(attributeName = USER_NAME,
			localSecondaryIndexName = TENANT_USER_INDEX_NAME)
	public String getUserId() {
		return _userId;
	}

	public void setUserId(String userId) {
		_userId = userId;
	}

}
