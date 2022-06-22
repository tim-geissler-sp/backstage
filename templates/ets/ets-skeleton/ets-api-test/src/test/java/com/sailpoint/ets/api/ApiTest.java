/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.api;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static com.sailpoint.util.cucumber.CucumberReportUtil.generateReport;
import static org.junit.Assert.assertEquals;

public class ApiTest {

	private static final String TEST_PATH = "classpath:com/sailpoint/ets";
	private static final String BERMUDA_API_URL = "https://api-e2e-ber.api.cloud.sailpoint.com";
	private static final String LIGHTHOUSE_API_URL = "https://api-e2e-light.api.cloud.sailpoint.com";

	@Test
	public void test() {
		// Remove isProd if-block after EventBridge has full support in Prod
		List<String> tags = new LinkedList<>();
		tags.add("~@ignore");
		if( this.isNonDevEnv() )
		{
			tags.add("~@devOnly");
		}
		Results results = Runner.path(TEST_PATH).tags(tags).parallel(1);
		try {
			generateReport(results.getReportDir(), "build", "ets");
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertEquals(results.getErrorMessages(), 0, results.getFailCount());
	}

	/**
	 * If this is a NonDev Env return True, otherwise false.  That is, return true if prod, staging, or perf.
	 */
	private boolean isNonDevEnv()
	{
		String isProdProperty = System.getProperty("isProd", "false");
		if( Boolean.parseBoolean(isProdProperty) )
		{
			return true;
		}

		String apiUrl = System.getProperty("apiUrl", "");
		if( apiUrl.equals(BERMUDA_API_URL)) {
			return true;
		}
		if( apiUrl.equals(LIGHTHOUSE_API_URL) ) {
			return true;
		}
		return false;
	}
}
