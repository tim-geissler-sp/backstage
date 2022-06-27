/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.context.rest;

import com.sailpoint.atlas.rest.RestApplication;
import com.sailpoint.notification.context.rest.resource.GlobalContextDebugResource;
import com.sailpoint.notification.context.rest.resource.NotificationTemplateContextResource;

/**
 * The notification template debug resource.
 */
public class GlobalContextRestApplication extends RestApplication {

	public GlobalContextRestApplication() {
		add(GlobalContextDebugResource.class);
		add(NotificationTemplateContextResource.class);
	}
}
