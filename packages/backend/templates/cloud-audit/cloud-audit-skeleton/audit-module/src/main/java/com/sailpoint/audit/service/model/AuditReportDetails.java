/*
 * Copyright (c) 2017. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.service.model;

import com.sailpoint.audit.service.AuditReportService;
import sailpoint.object.Attributes;

import java.util.Date;

/**
 * Created by mark.boyle on 4/4/17.
 */
public class AuditReportDetails {
	private String id;
	private String name;
	private Date date;
	private String status;
	private long duration;
	private long rows;
	private String type;
	private String reportName;
	private String availableFormats;
	private Attributes<String, Object> attributes;


	public AuditReportDetails(ReportDTO reportDTO, Attributes<String, Object> attrs ) {
		id = reportDTO.getId();
		name = reportDTO.getName();
		reportName = reportDTO.getReportName();
		if( reportName == null ){
			reportName = name;
		}

		date = new Date(reportDTO.getDate());
		rows = reportDTO.getRows();

		status = reportDTO.getStatus();
		duration = reportDTO.getDuration();
		availableFormats = reportDTO.getAvailableFormats();

		attributes = attrs;
		parseNameAndType(reportName);
	}

	private void parseNameAndType(String name) {
		if (name.startsWith(AuditReportService.ReportPrefix.AUDIT_TYPE_REPORT.toString())) {
			this.name = name.replace(AuditReportService.ReportPrefix.AUDIT_TYPE_REPORT.toString(), "");
			type = "by type";
		} else if (name.startsWith(AuditReportService.ReportPrefix.AUDIT_USER_REPORT.toString())) {
			this.name = name.replace(AuditReportService.ReportPrefix.AUDIT_USER_REPORT.toString(), "");
			type = "by user";
		} else {
			this.name = name;
			type = "unknown";
		}
	}
	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Date getDate() {
		return date;
	}

	public String getStatus() {
		return status;
	}

	public Long getDuration() {
		return duration;
	}

	public Long getRows() {
		return rows;
	}

	public String getType() {
		return type;
	}

	public String getReportName() {
		return reportName;
	}

	public String getAvailableFormats() {
		return availableFormats;
	}

	public Attributes<String, Object> getAttributes() { return attributes; }
}

