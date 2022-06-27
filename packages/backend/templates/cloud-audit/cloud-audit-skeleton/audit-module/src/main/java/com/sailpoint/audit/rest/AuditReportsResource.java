/*
 * Copyright (C) 2017-2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.rest;

import com.google.inject.Inject;
import com.sailpoint.atlas.security.RequireGroup;
import com.sailpoint.audit.service.AuditReportService;
import com.sailpoint.audit.service.model.AuditReportDetails;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sailpoint.tools.GeneralException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static com.sailpoint.atlas.idn.SecurityGroups.CERT_ADMIN;
import static com.sailpoint.atlas.idn.SecurityGroups.DASHBOARD;
import static com.sailpoint.atlas.idn.SecurityGroups.HELPDESK;
import static com.sailpoint.atlas.idn.SecurityGroups.ORG_ADMIN;
import static com.sailpoint.atlas.idn.SecurityGroups.REPORT_ADMIN;
import static com.sailpoint.atlas.idn.SecurityGroups.ROLE_ADMIN;
import static com.sailpoint.atlas.idn.SecurityGroups.ROLE_SUBADMIN;
import static com.sailpoint.atlas.idn.SecurityGroups.SOURCE_ADMIN;
import static com.sailpoint.atlas.idn.SecurityGroups.SOURCE_SUBADMIN;

/**
 * Created by mark.boyle on 4/3/17.
 */
@Path("auditReports")
public class AuditReportsResource {

	private static Log log = LogFactory.getLog(AuditReportsResource.class);
	@Inject
	AuditReportService _auditService;

	/**
	 * List of completed or in-progress audit reports of given type ('types', 'actions', or 'user')
	 *
	 * @param days     number of days of report history to list
	 * @param username (optional) username to filter on for 'user' report list
	 * @return a list of completed or in-progress audit reports of given type ('types', 'actions', or 'user')
	 * @throws GeneralException
	 */
	@GET
	@Path("list/{reportType}")
	@RequireGroup({CERT_ADMIN, DASHBOARD, HELPDESK, ORG_ADMIN, REPORT_ADMIN, ROLE_ADMIN, ROLE_SUBADMIN, SOURCE_ADMIN, SOURCE_SUBADMIN})
	@Produces(MediaType.APPLICATION_JSON)
	public List<AuditReportDetails> list(@QueryParam("days") int days,
										 @QueryParam("username") String username,
										 @PathParam("reportType") String reportType) throws GeneralException {
		if (reportType.equals("types")) {
			return _auditService.listTypeReports(days);
		}

		if (reportType.equals("user")) {
			return _auditService.getUserReport(days, username);
		}

		throw new GeneralException("Invalid report");
	}
}
