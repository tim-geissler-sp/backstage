/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.rest;

import com.sailpoint.atlas.rest.RestApplication;

/**
 * Org Lifecycle REST application
 */
public class OrgLifecycleRestApplication extends RestApplication {

	public OrgLifecycleRestApplication() {
		add(OrgLifecycleDebugResource.class);
	}
}