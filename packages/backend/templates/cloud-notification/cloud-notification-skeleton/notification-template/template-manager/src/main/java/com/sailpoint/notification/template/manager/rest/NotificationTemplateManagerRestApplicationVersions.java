/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.manager.rest;

import com.google.inject.Inject;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.rest.RestApplication;
import com.sailpoint.notification.template.manager.rest.resouce.TemplateConfigResource;
import com.sailpoint.notification.template.manager.rest.resouce.TemplateVersionConfigResource;

/**
 * Notification template manager rest applications for template versions.
 */
public class NotificationTemplateManagerRestApplicationVersions extends RestApplication {

	public NotificationTemplateManagerRestApplicationVersions() {
		add(TemplateVersionConfigResource.class);
	}
}