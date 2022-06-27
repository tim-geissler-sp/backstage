/*
 * Copyright (c) 2017. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sailpoint.audit.service.model.AuditEventDTO;

import java.util.Map;

public class AuditEventPayload {
	private String _auditEventXml;

	private AuditEventDTO _auditEventJson;

	private boolean _useAerStorage;

	public boolean hasJson(){
		return (_auditEventJson != null);
	}

	public boolean hasXml(){
		return (_auditEventXml != null && ! _auditEventXml.isEmpty() );
	}

	public String getAuditEventXml() {
		return _auditEventXml;
	}

	public void setAuditEventXml(String auditEventXml) {
		this._auditEventXml = auditEventXml;
	}

	public AuditEventDTO getAuditEventJson() {
		return _auditEventJson;
	}

	public void setAuditEventJson(Map<String,Object> auditEventJson) {
		ObjectMapper mapper = new ObjectMapper();
		this._auditEventJson = mapper.convertValue(auditEventJson, AuditEventDTO.class);
	}
	public void setAuditEventJson(AuditEventDTO auditEventDTO){
		this._auditEventJson = auditEventDTO;
	}

	public boolean isUseAerStorage() {
		return _useAerStorage;
	}

	public void setUseAerStorage(boolean useAerStorage) {
		this._useAerStorage = useAerStorage;
	}
}
