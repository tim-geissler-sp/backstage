/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.email.domain;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@DynamoDBTable(tableName = "hermes_tenant_custom_sender_emails")
public class TenantSenderEmail {

	private String _tenant;
	private String _id;
	private String _email;
	private String _verificationStatus;
	private String _displayName;

	@DynamoDBHashKey(attributeName = "tenant")
	public String getTenant() {
		return _tenant;
	}

	public void setTenant(String tenant) {
		_tenant = tenant;
	}

	@DynamoDBRangeKey(attributeName = "id")
	public String getId() {
		return _id;
	}

	public void setId(String id) {
		_id = id;
	}

	@DynamoDBIndexHashKey(attributeName = "email", globalSecondaryIndexName = "email-index")
	public String getEmail() {
		return _email;
	}

	public void setEmail(String email) {
		_email = email;
	}

	public String getVerificationStatus() {
		return _verificationStatus;
	}

	public void setVerificationStatus(String verificationStatus) {
		_verificationStatus = verificationStatus;
	}

	public String getDisplayName() {
		return _displayName;
	}

	public void setDisplayName(String displayName) {
		_displayName = displayName;
	}

	@Override
	public String toString() {
		return "TenantSenderEmail [tenant=" + _tenant + ", email=" + _email + ", verificationStatus=" +
				_verificationStatus + "]";
	}
}
