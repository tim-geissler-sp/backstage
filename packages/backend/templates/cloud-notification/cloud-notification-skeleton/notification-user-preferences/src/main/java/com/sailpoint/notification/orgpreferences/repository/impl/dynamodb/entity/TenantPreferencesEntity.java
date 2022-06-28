/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.orgpreferences.repository.impl.dynamodb.entity;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

/**
 * A Tenant Preferences persistence entity
 */
@DynamoDBTable(tableName = "hermes_tenant_prefs")
public class TenantPreferencesEntity extends BasePreferencesEntity {
}
