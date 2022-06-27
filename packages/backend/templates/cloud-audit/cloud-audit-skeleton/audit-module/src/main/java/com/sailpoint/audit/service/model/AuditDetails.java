/*
 * Copyright (c) 2017. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.service.model;

import com.sailpoint.audit.service.util.StackExtractor;
import sailpoint.object.AuditEvent;
import sailpoint.thunderbolt.util.NameUtil;

import java.util.Date;
import java.util.Map;

/**
 * Created by mark.boyle on 4/4/17.
 */
public class AuditDetails {

	private String id;
	private Date created;
	private String action;
	private String source;
	private String target;
	private String application;
	private String accountName;
	private String type;
	private String hostname;
	private String ipaddr;
	private String contextid;
	private String info;
	private String trackingId;
	private String errors;

	private Map<String,Object> attributes;

	public AuditDetails() {}

	public AuditDetails(AuditEvent event) {
		id = event.getId();
		created = event.getCreated();
		action = event.getAction();
		source = event.getSource();
		target = event.getTarget();
		application = NameUtil.getApplicationDisplayName(event.getApplication());
		Map<String,String> parsedFields = StackExtractor.getStack(application);
		application = parsedFields.get("application");
		accountName = event.getAccountName();
		type = event.getInstance();
		hostname = event.getString1();
		ipaddr = event.getString2();
		contextid = event.getString3();
		info = event.getString4();
		trackingId = event.getTrackingId();
		attributes = event.getAttributes();
	}

	public String getId() {
		return id;
	}

	public Date getCreated() {
		return created;
	}

	public String getAction() {
		return action;
	}

	public String getSource() {
		return source;
	}

	public String getTarget() {
		return target;
	}

	public String getApplication() {
		return application;
	}

	public String getType() {
		return type;
	}

	public String getHostname() {
		return hostname;
	}

	public String getIpaddr() {
		return ipaddr;
	}

	public String getContextid() {
		return contextid;
	}

	public String getInfo() {
		return info;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public void setIpaddr(String ipaddr) {
		this.ipaddr = ipaddr;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public String getTrackingId() {
		return trackingId;
	}

	public void setTrackingId(String trackingId) {
		this.trackingId = trackingId;
	}

	public String getErrors() {
		return errors;
	}

	public void setErrors(String errors) {
		this.errors = errors;
	}
}
