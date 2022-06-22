/*
 *
 *  * Copyright (c) 2019.  SailPoint Technologies, Inc. All rights reserved.
 *
 */
package com.sailpoint.audit.service.model;

/**
 *  Audit event DTO retrieved during raw SQL query
 */
public class BulkUploadDTO extends AuditEventDTO {
	private String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public void setCreated(String created) {
		super.setCreated(created);
	}
}
