/*
 *
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 *
 */
package com.sailpoint.audit.service.mapping;

public class AuditEventActionTypes {
	/**
	 * This list is used to ensure we only use IDN specified groupings for audit actions, and ignore any unknown
	 * values in the 'instance' field coming from CIS.  The 'audit types' implementation is not ideal due to our lack
	 * of control over CIS.
	 */
	public enum AuditActionType {
		AUTH,
		SSO,
	}

	public enum CISAuditReportType {
		PROVISIONING,
		PASSWORD_CHANGE,
		SOURCE,
		ACCESS_REQUEST
	}

	public enum AuditType {
		PROVISIONING,
		PASSWORD_ACTIVITY,
		SOURCE_MANAGEMENT,
		ACCESS_REQUEST,
		USER_MANAGEMENT,
		CERTIFICATION,
		ACCESS_ITEM,
		SYSTEM_CONFIG,
		IDENTITY_MANAGEMENT
	}

}
