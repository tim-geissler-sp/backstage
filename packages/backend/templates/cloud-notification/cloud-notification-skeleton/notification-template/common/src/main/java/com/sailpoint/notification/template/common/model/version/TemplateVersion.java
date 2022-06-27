/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.model.version;

import com.sailpoint.notification.template.common.model.NotificationTemplate;

/**
 * Entity that represent a notification template version.
 */
public class TemplateVersion {

	public static String HERMES_CONFIG_ENABLE_VERSION_SUPPORT = "HERMES_CONFIG_ENABLE_VERSION_SUPPORT";

	private String _versionId;
	private NotificationTemplate _notificationTemplate;
	private TemplateVersionInfo _templateVersionInfo;

	public TemplateVersion(String version,
						   NotificationTemplate notificationTemplate,
						   TemplateVersionInfo templateVersionInfo) {

		_versionId = version;
		_notificationTemplate = notificationTemplate;
		_templateVersionInfo = templateVersionInfo;
	}

	public String getVersionId() {
		return _versionId;
	}

	public void setVersionId(String versionId) {
		this._versionId = versionId;
	}

	public NotificationTemplate getNotificationTemplate() {
		return _notificationTemplate;
	}

	public void setNotificationTemplate(NotificationTemplate _notificationTemplate) {
		this._notificationTemplate = _notificationTemplate;
	}

	public TemplateVersionInfo getTemplateVersionInfo() {
		return _templateVersionInfo;
	}

	public void setTemplateVersionInfo(TemplateVersionInfo _templateVersionInfo) {
		this._templateVersionInfo = _templateVersionInfo;
	}
}
