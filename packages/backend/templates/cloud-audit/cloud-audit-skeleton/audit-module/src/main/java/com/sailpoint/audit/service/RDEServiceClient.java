/*
 * Copyright (c) 2017. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.atlas.idn.RestClientProvider;
import com.sailpoint.atlas.idn.ServiceNames;
import com.sailpoint.audit.service.model.ReportDTO;
import com.sailpoint.mantisclient.BaseRestClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mark.boyle on 4/24/17.
 */
@Singleton
public class RDEServiceClient {

	private static Log log = LogFactory.getLog(RDEServiceClient.class);

	@Inject
	RestClientProvider _restClientProvider;

	/**
	 * Return the report details from RDE for a given report name
	 *
	 * @param reportName name of the report to find
	 * @param days  (default: 7) number of days of audit events to include.
	 * @return ReportDTO
	 */
	public ReportDTO reportResult(String reportName, Integer days ) {
		BaseRestClient rdeClient = _restClientProvider.getRestClient(ServiceNames.RDE);
		ReportDTO reportConfig = getReportMap(reportName, days,null);
		try {
			return rdeClient.postJson(ReportDTO.class, "reporting/reports/result", reportConfig);
		} catch(RuntimeException ex ){
			log.error("RDE Server unavailable " + ex.toString());
			return reportConfig;
		}
	}

	/**
	 * Run report for Audits {'name': 'Audit Report','reportName':'audit-type-report-XX'} This is the
	 *  expected structure when running reports for audits. Note: 'name' is set to 'Audit Report'
	 * @param reportName
	 * @param days
	 * @param arguments
	 * @return
	 */
	public ReportDTO runReport(String reportName, Integer days, Map arguments){
		BaseRestClient rdeClient = _restClientProvider.getRestClient(ServiceNames.RDE);
		ReportDTO reportConfig = getReportMap(reportName, days, arguments);
		reportConfig.setName(AuditReportService.AUDIT_REPORT);
		try {
			return rdeClient.postJson(ReportDTO.class, "reporting/reports/run", reportConfig);
		} catch(RuntimeException ex ){
			log.error("RDE Server unavailable " + ex.toString());
			return reportConfig;
		}
	}

	/**
	 * Return the report config map specifying which report to run and additional parameters.
	 *
	 * @param reportName The name of the report.
	 * @param numDays number of days parameters to pass in.
	 * @param arguments additional filter arguments to pass to report runner.
	 * @return ReportConfig object for the given report and parameters.
	 */
	private ReportDTO getReportMap(String reportName,  Integer numDays,
								   Map arguments) {
		ReportDTO reportConfig = new ReportDTO();
		reportConfig.setName(reportName);
		reportConfig.setReportName(reportName);
		reportConfig.setTaskDefName(reportName);
		Map<String, Object> args = new HashMap<>();
		args.put("reportDefName", reportName);
		args.put("numDays",numDays.toString());
		if( arguments != null ) {
			args.putAll(arguments);
		}
		reportConfig.setArguments(args);
		return reportConfig;
	}
}
