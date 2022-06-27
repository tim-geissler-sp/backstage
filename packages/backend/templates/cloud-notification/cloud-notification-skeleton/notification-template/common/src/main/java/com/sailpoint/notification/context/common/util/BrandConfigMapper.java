/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.context.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;
import com.sailpoint.notification.context.common.model.BrandConfig;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class to map BrandConfig <=> GlobalContext.
 */
@Singleton
public class BrandConfigMapper {

	private static final ObjectMapper _objectMapper = new ObjectMapper();

	/**
	 * Converts BrandConfig to GlobalContext.
	 *
	 * @param brandConfig The BrandConfig dto
	 * @return a global context dto with brand data in attributes
	 */
	public static Map<String, Object> brandingConfigToMap(BrandConfig brandConfig) {
		Map mapConfig = _objectMapper.convertValue(brandConfig, Map.class);
		//name is going to be used as a key in this inline map so it can be removed
		String brand = Objects.requireNonNull(mapConfig.remove("name").toString());
		return Collections.singletonMap(brand, mapConfig);
	}
}