/*
 * Copyright (c) 2017. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.rest;

import com.sailpoint.atlas.rest.RestApplication;

/**
 * Created by mark.boyle on 4/3/17.
 */
public class AuditRestApplication extends RestApplication {

	public AuditRestApplication() {
		add(AuditReportsResource.class);
		add(AuditEventsResource.class);
		add(BulkSyncEventsResource.class);
		add(DataManagementResource.class);
	}
}
