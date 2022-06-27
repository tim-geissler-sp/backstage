/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service.mapping;

import com.sailpoint.audit.AuditEventConstants;
import com.sailpoint.audit.service.mapping.AuditEventActionTypes.AuditActionType;
import com.sailpoint.audit.service.mapping.AuditEventActionTypes.AuditType;
import com.sailpoint.audit.service.mapping.AuditEventActionTypes.CISAuditReportType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuditEventTypeToAction {

	public static Map<String, String> ACTION_TO_TYPE_MAPPING = new HashMap<>();

	public static final Map<String, List<String>> CIS_AUDIT_ACTION_TYPE_MAP = Collections.unmodifiableMap(
			new HashMap<String, List<String>>() {{
				put(CISAuditReportType.PASSWORD_CHANGE.toString(), AuditEventConstants.PASSWORD_ACTIVITY_ACTION_LIST);
				put(CISAuditReportType.SOURCE.toString(), AuditEventConstants.SOURCE_MANAGEMENT_ACTION_LIST);
				// Provisioning events
				put(AuditType.PROVISIONING.toString(), AuditEventConstants.PROVISIONING_ACTION_LIST);
				// Access Request related events
				put(AuditType.ACCESS_REQUEST.toString(), AuditEventConstants.ACCESS_REQUEST_ACTION_LIST);
			}});

	public static final Map<String, List<String>> ES_AUDIT_ACTION_TYPE_MAP = Collections.unmodifiableMap(
			new HashMap<String, List<String>>() {{
				// Provisioning events
				put(AuditType.PROVISIONING.toString(), AuditEventConstants.PROVISIONING_ACTION_LIST);
				// Access Request related events
				put(AuditType.ACCESS_REQUEST.toString(), AuditEventConstants.ACCESS_REQUEST_ACTION_LIST);
				//User registration, KBA, Strong Auth related events
				put(AuditType.USER_MANAGEMENT.toString(), AuditEventConstants.USER_MANAGEMENT_ACTION_LIST);
				//Certification actions
				put(AuditType.CERTIFICATION.toString(), AuditEventConstants.CERTIFICATIONS_ACTION_LIST);
				put(AuditType.PASSWORD_ACTIVITY.toString(), AuditEventConstants.PASSWORD_ACTIVITY_ACTION_LIST);
				put(AuditType.SOURCE_MANAGEMENT.toString(), AuditEventConstants.SOURCE_MANAGEMENT_ACTION_LIST);

				//Access request items ACCESS_ITEM actions
				put(AuditType.ACCESS_ITEM.toString(), AuditEventConstants.ACCESS_ITEM_ACTION_LIST);

				put(AuditActionType.AUTH.toString(), AuditEventConstants.AUTH_ACTION_LIST);

				put(AuditActionType.SSO.toString(), AuditEventConstants.SSO_ACTION_LIST);

				put(AuditType.IDENTITY_MANAGEMENT.toString(), AuditEventConstants.IDENTITY_MANAGMENT_ACTION_LIST);

				put(AuditType.SYSTEM_CONFIG.toString(), AuditEventConstants.SYSTEM_CONFIG_ACTION_LIST);
			}});

	static {
		CIS_AUDIT_ACTION_TYPE_MAP.forEach((k, v) -> {
			v.forEach(action -> {
				String type = k.toString();
				ACTION_TO_TYPE_MAPPING.put(action, type);
			});
		});
	}

	static {
		ES_AUDIT_ACTION_TYPE_MAP.forEach((k, v) -> {
			v.forEach(action -> {
				String type = k.toString();
				ACTION_TO_TYPE_MAPPING.put(action, type);
			});
		});
	}
}
