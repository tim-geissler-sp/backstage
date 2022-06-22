/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.rest;

import com.sailpoint.atlas.rest.RestApplication;

public class NotificationUserPreferencesRestApplication extends RestApplication {

	public NotificationUserPreferencesRestApplication() {
		add(NotificationUserPreferencesDebugResource.class);
	}
}