/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.utils;

import com.sailpoint.audit.service.util.StackExtractor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class StackExtractorTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testStackSourceNameId() {
		Map<String, String> parsedFields = StackExtractor.getStack("[wps] Test-Source [source-123456]");
		Assert.assertEquals("wps", parsedFields.get("stack"));
		Assert.assertEquals("Test-Source", parsedFields.get("application"));
		Assert.assertEquals("source-123456", parsedFields.get("sourceId"));
	}

	@Test
	public void testStackSourceName() {
		Map<String, String> parsedFields = StackExtractor.getStack("[wps] TestSource [source]");
		Assert.assertEquals("wps", parsedFields.get("stack"));
		Assert.assertEquals("TestSource", parsedFields.get("application"));
		Assert.assertEquals("source", parsedFields.get("sourceId"));
	}

	@Test
	public void testSourceName() {
		Map<String, String> parsedFields = StackExtractor.getStack("TestSource [source]");
		Assert.assertEquals("TestSource", parsedFields.get("application"));
		Assert.assertEquals("source", parsedFields.get("sourceId"));
	}

	@Test
	public void testSourceNameId() {
		Map<String, String> parsedFields = StackExtractor.getStack("Test Source [source-123456]");
		Assert.assertEquals("Test Source", parsedFields.get("application"));
		Assert.assertEquals("source-123456", parsedFields.get("sourceId"));
	}
}
