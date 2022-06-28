/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.persistence;

import sailpoint.object.AuditEvent;

/**
 * POJO for encapsulating the properties of an AuditEvent retrieved from S3 Persistence.
 * It includes the AuditEvent, the JSON of the event, and any meta-data resulting from
 * persisting the AuditEvent in the S3 service.
 */
public class S3AuditEventEnvelope {

	String s3ObjectKey;

	String tenantId;

	String auditEventId;

	String sha256Hash;

	String md5Checksum;

	String auditEventJson;

	AuditEvent auditEvent;

	// Did the AuditEvent already exist in S3?
	boolean alreadyExistedInS3 = false;

	// Did the previous copy have a different MD5/SHA check sum?
	boolean previousDiffered = false;

	public String getS3ObjectKey() {
		return s3ObjectKey;
	}

	public void setS3ObjectKey(String s3ObjectKey) {
		this.s3ObjectKey = s3ObjectKey;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getAuditEventId() {
		return auditEventId;
	}

	public void setAuditEventId(String auditEventId) {
		this.auditEventId = auditEventId;
	}

	public String getSha256Hash() {
		return sha256Hash;
	}

	public void setSha256Hash(String sha256Hash) {
		this.sha256Hash = sha256Hash;
	}

	public String getAuditEventJson() {
		return auditEventJson;
	}

	public void setAuditEventJson(String auditEventJson) {
		this.auditEventJson = auditEventJson;
	}

	public AuditEvent getAuditEvent() {
		return auditEvent;
	}

	public void setAuditEvent(AuditEvent auditEvent) {
		this.auditEvent = auditEvent;
	}

	public String getMd5Checksum() {
		return md5Checksum;
	}

	public void setMd5Checksum(String md5Checksum) {
		this.md5Checksum = md5Checksum;
	}

	public boolean isAlreadyExistedInS3() { return alreadyExistedInS3;	}

	public void setAlreadyExistedInS3(boolean alreadyExistedInS3) { this.alreadyExistedInS3 = alreadyExistedInS3; }

	public boolean isPreviousDiffered() { return previousDiffered; }

	public void setPreviousDiffered(boolean previousDiffered) { this.previousDiffered = previousDiffered; }

}
