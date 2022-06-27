/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.sender.common.test;

import com.sailpoint.atlas.OrgData;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.security.AdministratorSecurityContext;

public class TestUtil {

	public static RequestContext setDummyRequestContext() {
		OrgData orgData = new OrgData();
		orgData.setPod("dev");
		orgData.setOrg("acme-solar");

		RequestContext requestContext = new RequestContext();
		requestContext.setSecurityContext(new AdministratorSecurityContext());
		requestContext.setOrgData(orgData);
		RequestContext.set(requestContext);

		return requestContext;
	}
}
