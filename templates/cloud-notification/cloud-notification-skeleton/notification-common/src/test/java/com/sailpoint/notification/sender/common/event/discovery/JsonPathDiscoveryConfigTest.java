/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.event.discovery;

import com.sailpoint.utilities.JsonUtil;
import com.sailpoint.notification.sender.common.event.discovery.impl.jsonpath.JsonPathDiscoveryConfig;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class JsonPathDiscoveryConfigTest {

	@Test
	public void jsonPathDiscoveryConfigTest() {
		List<JsonPathDiscoveryConfig> discoveryConfigs = new ArrayList<>();
		discoveryConfigs.add(new JsonPathDiscoveryConfig("$.content.approvers[*].id", "recipientId"));
		discoveryConfigs.add(new JsonPathDiscoveryConfig("$.content.approvers[*].name", "recipientName"));
		String json = JsonUtil.toJson(discoveryConfigs);

		discoveryConfigs = JsonUtil.parseList(JsonPathDiscoveryConfig.class, json);
		Assert.assertEquals(2, discoveryConfigs.size());
		Assert.assertEquals("$.content.approvers[*].id", discoveryConfigs.get(0).getJsonPath());
		Assert.assertEquals("recipientId", discoveryConfigs.get(0).getValueName());
	}
}
