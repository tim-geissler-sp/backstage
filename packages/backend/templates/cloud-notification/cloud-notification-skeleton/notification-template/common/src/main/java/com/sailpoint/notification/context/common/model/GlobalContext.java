/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.context.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * GlobalContext model.
 */
public class GlobalContext {

	private String _id;

	private String _tenant;

	private Map<String, Object> _attributes;

	private DateTime _created;

	private DateTime _modified;

	private Long _version;

	public GlobalContext(@JsonProperty("tenant") String tenant) {
		requireNonNull(tenant);
		_tenant = tenant;
	}

	public String getId() {
		return _id;
	}

	public void setId(String id) {
		_id = id;
	}

	public String getTenant() {
		return _tenant;
	}

	public Map<String, Object> getAttributes() {
		return _attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		_attributes = attributes;
	}

	public DateTime getCreated() {
		return _created;
	}

	public void setCreated(DateTime created) {
		_created = created;
	}

	public DateTime getModified() {
		return _modified;
	}

	public void setModified(DateTime modified) {
		_modified = modified;
	}

	public Long getVersion() { return _version; }

	public void setVersion(Long version) {
		_version = version;
	}
}
