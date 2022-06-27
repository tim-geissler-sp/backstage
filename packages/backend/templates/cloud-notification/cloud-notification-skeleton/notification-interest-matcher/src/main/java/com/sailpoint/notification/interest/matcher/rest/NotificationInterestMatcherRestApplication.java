/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.interest.matcher.rest;

import com.sailpoint.atlas.rest.RestApplication;

public class NotificationInterestMatcherRestApplication extends RestApplication {

	public NotificationInterestMatcherRestApplication() {
		add(NotificationInterestMatcherDebugResource.class);
	}
}

