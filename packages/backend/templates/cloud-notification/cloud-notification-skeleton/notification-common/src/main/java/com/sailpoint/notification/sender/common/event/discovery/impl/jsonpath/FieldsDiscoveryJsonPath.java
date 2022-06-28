/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.event.discovery.impl.jsonpath;

import com.sailpoint.atlas.util.JsonPathUtil;
import com.sailpoint.utilities.JsonUtil;
import com.sailpoint.notification.sender.common.event.discovery.FieldsDiscovery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class implementing FieldsDiscovery for json payload based on json path
 */
public class FieldsDiscoveryJsonPath implements FieldsDiscovery {

	public final static String JSON_PATH_DISCOVERY = "jsonPathDiscovery";

	private final List<JsonPathDiscoveryConfig> _config;

	public FieldsDiscoveryJsonPath(String config) {
		_config = JsonUtil.parseList(JsonPathDiscoveryConfig.class, config);
	}

	/**
	 *  {@inheritDoc}
	 */
	@Override
	public List<Map<String, String>> discover(String payload) {
		List<Map<String, String>> result = new ArrayList<>();
		Map<String, String> map = new HashMap<>();
		result.add(map);

		Object document = JsonPathUtil.parse(payload);
		for(JsonPathDiscoveryConfig c : _config) {
			List<String> values = JsonPathUtil.getByPath(document, c.getJsonPath());
			if(values.size() > 0) {
				map.put(c.getValueName(), values.get(0));
			}
		}
		return result;
	}
}
