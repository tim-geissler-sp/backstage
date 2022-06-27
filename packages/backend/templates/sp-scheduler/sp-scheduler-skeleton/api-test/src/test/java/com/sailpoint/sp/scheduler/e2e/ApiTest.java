/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.sp.scheduler.e2e;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.junit.Test;

import static com.sailpoint.util.cucumber.CucumberReportUtil.generateReport;
import static org.junit.Assert.assertEquals;

public class ApiTest {

	private static final String TEST_PATH = "classpath:com/sailpoint/sp/scheduler/e2e";

	@Test
	public void test() {
		Results results = Runner.path(TEST_PATH).tags("~@ignore").parallel(1);
		generateReport(results.getReportDir(), "build", "sp-scheduler");
		assertEquals(results.getErrorMessages(), 0, results.getFailCount());
	}
}
