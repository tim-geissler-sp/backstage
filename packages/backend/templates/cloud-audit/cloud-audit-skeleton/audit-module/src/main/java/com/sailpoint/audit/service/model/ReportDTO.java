/*
 * Copyright (c) 2017. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.service.model;

import java.util.Map;

/**
 * Created by mark.boyle on 4/24/17.
 */
public class ReportDTO {
	private String name;
	private String reportName;
	private String taskDefName;
	private String type;
	private String id;
	private Map<String, Object> arguments;
	private long date;
	private long duration;
	private String status;
	private long rows;
	private boolean completed;
	private String availableFormats;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getReportName() {
		return reportName;
	}

	public void setReportName(String reportName) {
		this.reportName = reportName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Map<String, Object> getArguments() {
		return arguments;
	}

	public void setArguments(Map<String, Object> arguments) {
		this.arguments = arguments;
	}

	public long getDate() {
		return date;
	}

	public void setDate(long date) {
		this.date = date;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public long getRows() {
		return rows;
	}

	public void setRows(long rows) {
		this.rows = rows;
	}

	public String getTaskDefName() {
		return taskDefName;
	}

	public void setTaskDefName(String taskDefName) {
		this.taskDefName = taskDefName;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	public String getAvailableFormats() {
		return availableFormats;
	}

	public void setAvailableFormats(String availableFormats) {
		this.availableFormats = availableFormats;
	}
}
