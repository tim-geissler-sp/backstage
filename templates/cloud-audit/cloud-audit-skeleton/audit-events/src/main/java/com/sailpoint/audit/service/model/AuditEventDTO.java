/*
 * Copyright (c) 2017. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service.model;

import sailpoint.object.AuditEvent;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 *  Base model of what an AuditEvent will be stored as.
 */
public class AuditEventDTO {
	private String action;
	private String target;
	private String targetType;
	private String source;
	private String sourceType;
	private String stack;
	private String uuid;
	private String created;
	private String type;
	private String instance;
	private String application;
	private String description;
	private String hostname;
	private String ipaddr;
	private String requestId;
	private String contextId;
	private String info;
	private String comment;
	private String accountName;
	private String _interface;
	private String attributeName;
	private String attributeValue;
	private Map<String,Object> attributes;

	public AuditEventDTO(){
		this.created = DateTimeFormatter.ISO_INSTANT.format(new Date().toInstant());
		this.uuid = UUID.randomUUID().toString();
	}

	public AuditEventDTO(AuditEvent auditEvent){
		Date d = auditEvent.getCreated();
		if(d == null){
			d = new Date();
		}
		this.created = DateTimeFormatter.ISO_INSTANT.format(d.toInstant());
		this.action = auditEvent.getAction();
		this.application = auditEvent.getApplication();
		this.uuid = auditEvent.getId();
		if( this.uuid == null ){
			this.uuid = UUID.randomUUID().toString();
		}
		this.target = auditEvent.getTarget();
		this.source = auditEvent.getSource();
		this.description = auditEvent.getDescription();
		this.hostname = auditEvent.getString1();
		this.ipaddr = auditEvent.getString2();
		this.contextId = auditEvent.getString3();
		this.info = auditEvent.getString4();
		this.type = auditEvent.getInstance();
		this.accountName = auditEvent.getAccountName();
		this._interface = auditEvent.getInterface();
		this.attributeName = auditEvent.getAttributeName();
		this.attributeValue = auditEvent.getAttributeValue();

		if(auditEvent.getAttributes() != null) {
			setAttributes(auditEvent.getAttributes().getMap());
		}
	}

	protected void setCreated(String created) {
		this.created = created;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getTargetType() {
		return targetType;
	}

	public void setTargetType(String targetType) {
		this.targetType = targetType;
	}

	public String getSourceType() {
		return sourceType;
	}

	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	public String getStack() {
		return stack;
	}

	public void setStack(String stack) {
		this.stack = stack;
	}

	public String getUuid() {
		return uuid;
	}

	public String getCreated() {
		return created;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getIpaddr() {
		return ipaddr;
	}

	public void setIpaddr(String ipaddr) {
		this.ipaddr = ipaddr;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public String getType() {
		return ( instance == null ) ? type : instance;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setInstance(String instance) {
		this.instance = instance;
	}

	public String getContextId() {
		return contextId;
	}

	public void setContextId(String contextId) {
		this.contextId = contextId;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
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

	public String getInterface() {
		return _interface;
	}

	public void setInterface(String _interface) {
		this._interface = _interface;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public String getAttributeValue() {
		return attributeValue;
	}

	public void setAttributeValue(String attributeValue) {
		this.attributeValue = attributeValue;
	}
}