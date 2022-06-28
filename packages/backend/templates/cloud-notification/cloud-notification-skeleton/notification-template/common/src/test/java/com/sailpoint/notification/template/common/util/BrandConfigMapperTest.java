/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.template.common.util;

import com.sailpoint.notification.context.common.model.BrandConfig;
import com.sailpoint.notification.context.common.util.BrandConfigMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * Test BrandConfigMapper
 */
public class BrandConfigMapperTest {

	@Test
	public void brandConfigMapperTest() {
		BrandConfig config = new BrandConfig();
		config.setName("testName");
		config.setProductName("testProductName");
		Map<String, Object> result = BrandConfigMapper.brandingConfigToMap(config);
		Assert.assertEquals(8, ((Map)result.get("testName")).size());
		Assert.assertEquals("testProductName", ((Map)result.get("testName")).get("productName"));
	}

}
