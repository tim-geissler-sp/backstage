/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.manager.rest;

import com.sailpoint.atlas.rest.RestApplication;
import com.sailpoint.notification.template.manager.rest.resouce.TemplateConfigResource;
import com.sailpoint.notification.template.manager.rest.resouce.TemplateDefaultResource;
import com.sailpoint.notification.template.manager.rest.resouce.TemplateVersionConfigResource;

/**
 * Notification template manager rest applications for default templates.
 */
public class NotificationTemplateManagerRestApplicationDefault extends RestApplication {

	public NotificationTemplateManagerRestApplicationDefault() {
		add(TemplateDefaultResource.class);
	}
}