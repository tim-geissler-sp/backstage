/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification;

import com.sailpoint.atlas.rest.RestApplication;
import com.sailpoint.notification.rest.SendResource;
import com.sailpoint.notification.rest.VerifiedFromAddressResource;

public class NotificationRestApplication extends RestApplication {

	public NotificationRestApplication() {
		add(SendResource.class);
		add(VerifiedFromAddressResource.class);
	}
}
