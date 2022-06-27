/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.event.discovery;


import com.sailpoint.notification.sender.common.event.discovery.impl.jsonpath.FieldsDiscoveryArrayJsonPath;
import com.sailpoint.notification.sender.common.event.discovery.impl.jsonpath.FieldsDiscoveryJsonPath;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class FieldsDiscoveryFactoryTest {

	@Test
	public void fieldsDiscoveryFactoryTest() {
		FieldsDiscovery discovery = FieldsDiscoveryFactory
				.getFieldsDiscovery(FieldsDiscoveryArrayJsonPath.JSON_ARRAY_PATH_DISCOVERY, FieldsDiscoveryArrayJsonPathTest.CONFIG);
		Assert.assertNotNull(discovery);

		List<Map<String, String>> fieldsValues = discovery.discover(FieldsDiscoveryArrayJsonPathTest.JSON_EVENT);
		Assert.assertEquals(2, fieldsValues.size());
		Assert.assertEquals("james.smith", fieldsValues.get(0).get("recipientName"));
		Assert.assertEquals("jane.doe", fieldsValues.get(1).get("recipientName"));

		discovery = FieldsDiscoveryFactory
				.getFieldsDiscovery(FieldsDiscoveryJsonPath.JSON_PATH_DISCOVERY, FieldsDiscoveryJsonPathTest.CONFIG);
		Assert.assertNotNull(discovery);

		fieldsValues = discovery.discover(FieldsDiscoveryJsonPathTest.JSON_EVENT);
		Assert.assertEquals(1, fieldsValues.size());
		Assert.assertEquals(2, fieldsValues.get(0).size());
		Assert.assertEquals("314cf125-f892-4b16-bcbb-bfe4afb01f85", fieldsValues.get(0).get("recipientId"));
		Assert.assertEquals("james.smith", fieldsValues.get(0).get("recipientName"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void fieldsDiscoveryFactoryUnknownTypeTest() {
		FieldsDiscoveryFactory
				.getFieldsDiscovery("unknownType", FieldsDiscoveryJsonPathTest.CONFIG);
	}

	@Test(expected = IllegalArgumentException.class)
	public void fieldsDiscoveryFactoryBrokenConfigTest() {
		FieldsDiscoveryFactory
				.getFieldsDiscovery(FieldsDiscoveryJsonPath.JSON_PATH_DISCOVERY, "brokenJson");
	}
}
