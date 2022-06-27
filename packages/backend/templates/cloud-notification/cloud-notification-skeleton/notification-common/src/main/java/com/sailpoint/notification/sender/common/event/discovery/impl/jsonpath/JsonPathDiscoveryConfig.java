/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.event.discovery.impl.jsonpath;

/**
 * Config class for FieldsDiscoveryJsonPath.
 */
public class JsonPathDiscoveryConfig {

	private final String _jsonPath;

	private final String _valueName;

	public JsonPathDiscoveryConfig(String jsonPath, String valueName) {
		_jsonPath = jsonPath;
		_valueName = valueName;
	}

	public String getJsonPath() {
		return _jsonPath;
	}

	public String getValueName() {
		return _valueName;
	}
}
