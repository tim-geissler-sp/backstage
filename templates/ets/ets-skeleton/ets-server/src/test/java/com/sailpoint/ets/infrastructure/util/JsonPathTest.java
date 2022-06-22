/*
 * Copyright (C) 2021 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.util;

import com.sailpoint.atlas.util.JsonPathUtil;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests to ensure that JsonPath implementation is able to support common ETS subscription filtering use-cases
 * Filtering occurs in {@link com.sailpoint.ets.infrastructure.web.SubscriptionController#validateFilter}
 */
public class JsonPathTest {

	@Test
	public void jsonPathTestCases() {
		String json = "{\n" +
			"  \"_metadata\": {\n" +
			"    \"invocationId\": \"57f1f7bd-e60a-4d19-b6de-e65b413fc578\",\n" +
			"    \"triggerId\": \"idn:post-provisioning\",\n" +
			"    \"triggerType\": \"fireAndForget\"\n" +
			"  },\n" +
			"  \"accountRequests\": [\n" +
			"    {\n" +
			"      \"accountId\": \"CN=Hefty Smurf,OU=eef-smurfs,OU=arsenal,DC=TestAutomationAD,DC=local\",\n" +
			"      \"accountOperation\": \"Disable\",\n" +
			"      \"attributeRequests\": [],\n" +
			"      \"provisioningResult\": \"committed\",\n" +
			"      \"provisioningTarget\": \"AD - Smurfs\",\n" +
			"      \"source\": {\n" +
			"        \"id\": \"2c9180897175034f017178ed377447b7\",\n" +
			"        \"name\": \"AD - Smurfs\",\n" +
			"        \"type\": \"SOURCE\"\n" +
			"      },\n" +
			"      \"ticketId\": null\n" +
			"    }\n" +
			"  ],\n" +
			"  \"action\": \"Lifecycle State Change\",\n" +
			"  \"errors\": [],\n" +
			"  \"recipient\": {\n" +
			"    \"id\": \"2c91808c715b70af01717966a74e11c4\",\n" +
			"    \"name\": \"Hefty Smurf\",\n" +
			"    \"type\": \"IDENTITY\"\n" +
			"  },\n" +
			"  \"requester\": {\n" +
			"    \"id\": \"2c9180845000e01201500688a4f3061b\",\n" +
			"    \"name\": \"erin.frank\",\n" +
			"    \"type\": \"IDENTITY\"\n" +
			"  },\n" +
			"  \"sources\": \"AD - Smurfs\",\n" +
			"  \"trackingNumber\": \"274f29fef03f4cc28d487eea802ba0c7\",\n" +
			"  \"warnings\": []\n" +
			"}";

		assertFalse(JsonPathUtil.isPathExist(json, "$[?(@.action == 'Blah blah')] "));
		assertTrue(JsonPathUtil.isPathExist(json, "$[?(@.action == 'Lifecycle State Change')]"));

		assertFalse(JsonPathUtil.isPathExist(json, "$.accountRequests[?(@.accountOperation == 'Enable')]"));
		assertTrue(JsonPathUtil.isPathExist(json, "$.accountRequests[?(@.accountOperation == 'Disable')]"));

		assertFalse(JsonPathUtil.isPathExist(json, "$.accountRequests[?(@.accountId =~ /^.*day quil.*$/i)]"));
		assertTrue(JsonPathUtil.isPathExist(json, "$.accountRequests[?(@.accountId =~ /^.*hefty smurf.*$/i)]"));
	}
}
