/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.rest;

import com.sailpoint.atlas.rest.RestApplication;

/**
 * The notification template debug resource.
 */
public class NotificationTemplateRestApplication extends RestApplication {

	public NotificationTemplateRestApplication() {
		add(NotificationTemplateDebugResource.class);
	}
}
