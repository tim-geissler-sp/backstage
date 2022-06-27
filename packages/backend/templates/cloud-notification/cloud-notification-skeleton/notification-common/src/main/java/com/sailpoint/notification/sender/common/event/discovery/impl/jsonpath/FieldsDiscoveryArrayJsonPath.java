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
 * Class implementing FieldsDiscovery for json payload based on json path from json array
 */
public class FieldsDiscoveryArrayJsonPath implements FieldsDiscovery {

	public final static String JSON_ARRAY_PATH_DISCOVERY = "jsonArrayPathDiscovery";

	private final List<JsonPathDiscoveryConfig> _config;

	public FieldsDiscoveryArrayJsonPath(String config) {
		_config = JsonUtil.parseList(JsonPathDiscoveryConfig.class, config);
	}

	/**
	 *  {@inheritDoc}
	 */
	@Override
	public List<Map<String, String>> discover(String payload) {
		List<Map<String, String>> result = new ArrayList<>();
		Object document = JsonPathUtil.parse(payload);

		for(JsonPathDiscoveryConfig c : _config) {
			List<String> values = JsonPathUtil.getByPath(document, c.getJsonPath());
			for(int i =0; i< values.size(); i++) {
				Map<String, String> map;
				if(result.size() <= i) {
					map = new HashMap<>();
					result.add(map);
				} else {
					map = result.get(i);
				}
				map.put(c.getValueName(), values.get(i));
			}
		}
		return result;
	}
}
