/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.sender;

import com.sailpoint.atlas.rest.RestApplication;
import com.sailpoint.notification.sender.common.rest.NotificationDebugResource;

public class NotificationSenderRestApplication extends RestApplication {

	public NotificationSenderRestApplication() {
		add(NotificationDebugResource.class);
	}
}
