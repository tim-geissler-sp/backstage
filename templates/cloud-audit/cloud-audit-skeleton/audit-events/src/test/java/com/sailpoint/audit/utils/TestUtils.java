/*
 *
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 *
 */

package com.sailpoint.audit.utils;

import com.mysql.jdbc.Driver;
import com.sailpoint.atlas.OrgData;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.security.AdministratorSecurityContext;
import com.sailpoint.audit.service.model.AuditEventDTO;
import com.sailpoint.mantis.platform.db.JdbcConnectionInfo;
import com.sailpoint.mantis.platform.db.MantisOrgData;
import com.sailpoint.atlas.search.model.event.Event;

import java.util.Date;

public class TestUtils {
	public static RequestContext setDummyRequestContext() {
		JdbcConnectionInfo connectionInfo = new JdbcConnectionInfo();
		connectionInfo.setDriver(Driver.class.getName());
		connectionInfo.setUrl("url");
		connectionInfo.setUser("user");
		connectionInfo.setPassword("password");

		OrgData orgData = new OrgData();
		orgData.setPod("dev");
		orgData.setOrg("acme-solar");
		orgData.setTenantId("tenantId");
		MantisOrgData.setConnectionInfo(orgData, connectionInfo);

		RequestContext requestContext = new RequestContext();
		requestContext.setOrgData(orgData);
		requestContext.setSecurityContext(new AdministratorSecurityContext());
		RequestContext.set(requestContext);

		return requestContext;
	}

	public static RequestContext setDummyRequestContext(String tenantId, String orgName, String podName) {

		RequestContext requestContext = new RequestContext();
		requestContext.setOrgData(new OrgData(podName, orgName));
		requestContext.getOrgData().setTenantId(tenantId);

		requestContext.setSecurityContext(new AdministratorSecurityContext());
		RequestContext.set(requestContext);

		return requestContext;
	}

	public static RequestContext setDummyRequestContext(String orgName) {

		OrgData orgData = new OrgData();
		orgData.setPod("dev");
		orgData.setOrg(orgName);
		orgData.setTenantId("tenantId");

		RequestContext requestContext = new RequestContext();
		requestContext.setOrgData(orgData);
		requestContext.setSecurityContext(new AdministratorSecurityContext());
		RequestContext.set(requestContext);

		return requestContext;
	}

	public static AuditEventDTO getTestEvent(String id, String org, String pod) {
		AuditEventDTO auditEventDTO = new AuditEventDTO();
		auditEventDTO.setAction("USER_ACTIVATE");
		auditEventDTO.setTarget("target");
		auditEventDTO.setSource("source");
		auditEventDTO.setType("type");

		return auditEventDTO;
	}

	public static Event getTestEvent() {
		Event event = new Event();
		event.setOrg("acme-solar");
		event.setPod("testpod");
		event.setAction("SOURCE_CREATE");
		event.setCreated(new Date());
		event.setType("SOURCE_MANAGEMENT");

		return event;
	}
}
