/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.event.discovery;

import com.sailpoint.notification.sender.common.event.discovery.impl.jsonpath.FieldsDiscoveryJsonPath;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

/**
 * Test for class FieldsDiscoveryJsonPath
 */
public class FieldsDiscoveryJsonPathTest {

	@Test
	public void fieldsDiscoveryJSONPathTest() {
		FieldsDiscoveryJsonPath discoveryJsonPath = new FieldsDiscoveryJsonPath(CONFIG);
		List<Map<String, String>> fieldsValues = discoveryJsonPath.discover(JSON_EVENT);
		Assert.assertEquals(1, fieldsValues.size());
		Assert.assertEquals(2, fieldsValues.get(0).size());
		Assert.assertEquals("314cf125-f892-4b16-bcbb-bfe4afb01f85", fieldsValues.get(0).get("recipientId"));
		Assert.assertEquals("james.smith", fieldsValues.get(0).get("recipientName"));
	}

	final static String JSON_EVENT = "{\"content\": {  \n"+
			"        \"approver\": {  \n"+
			"            \"id\": \"314cf125-f892-4b16-bcbb-bfe4afb01f85\",  \n"+
			"            \"name\": \"james.smith\"  \n"+
			"        },  \n"+
			"        \"requester_id\": \"46ec3058-eb0a-41b2-8df8-1c3641e4d771\",  \n"+
			"        \"requester_name\": \"boss.man\",  \n"+
			"        \"accessItems\": [{  \n"+
			"            \"type\": \"ROLE\",  \n"+
			"            \"name\": \"Engineering Administrator\"  \n"+
			"        }]  \n"+
			"    }  }";

	final static String CONFIG = "[\n" +
			"  {\n" +
			"    \"jsonPath\": \"$.content.approver.id\",\n" +
			"    \"valueName\": \"recipientId\"\n" +
			"  },\n" +
			"  {\n" +
			"    \"jsonPath\": \"$.content.approver.name\",\n" +
			"    \"valueName\": \"recipientName\"\n" +
			"  }\n" +
			"]";
}
