/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.orgpreferences.repository.rest;

import com.sailpoint.atlas.rest.RestApplication;
import com.sailpoint.notification.orgpreferences.repository.rest.resource.PreferencesV3Resource;

public class NotificationPreferencesRestApplication extends RestApplication {
	public NotificationPreferencesRestApplication() {
		add(PreferencesV3Resource.class);
	}
}
