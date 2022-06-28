/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.infrastructure.sync;

import com.sailpoint.sp.identity.event.domain.App;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SyncUtilTest {

	Map<String, Object> identity;

	@Before
	public void setup() {
		identity = new HashMap<>();
		Map<String, Object> processingDetails = new HashMap<>();

		processingDetails.put("date", new Date(0));
		processingDetails.put("message", "message");
		processingDetails.put("retryCount", 0);
		processingDetails.put("stackTrace", "stackTrace");
		processingDetails.put("stage", "stage");
		identity.put("processingDetails", processingDetails);

		Map<String, Object> account = new HashMap<>();
		account.put("id", "account1234");
		account.put("name", "Test Account");
		account.put("nativeIdentity", "native1234");
		identity.put("accounts", Collections.singletonList(account));

		Map<String, Object> app = new HashMap<>();
		app.put("serviceAppId", "app1234");
		app.put("serviceAppName", "My App");
		app.put("serviceId", "source1234");
		app.put("serviceName", "Test Source");

		Map<String, Object> appAccount = new HashMap<>();
		appAccount.put("id", "account1234");
		appAccount.put("nativeIdentity", "native1234");
		app.put("account", appAccount);
		identity.put("apps", Collections.singletonList(app));
	}

	@Test
	public void testProcessProcessingDetails() {
		Map<String, Object> res = SyncUtil.fetchProcessingDetails(identity);
		assertEquals(res.size(), 4);
		assertNull(res.get("stackTrace"));

		identity.remove("processingDetails");
		res = SyncUtil.fetchProcessingDetails(identity);
		assertNull(res);
	}

	@Test
	public void testGetApps() {
		List<App> apps = SyncUtil.getAppsFromIdentityMap(identity);
		assertNotNull(apps);
		assertEquals(1, apps.size());
		App app = apps.get(0);
		assertEquals("app1234", app.getAppId().getId());
		assertEquals("My App", app.getAppId().getName());
		assertEquals("source1234", app.getSourceId().getId());
		assertEquals("Test Source", app.getSourceId().getName());
		assertEquals("account1234", app.getAccountId().getId());
		assertEquals("Test Account", app.getAccountId().getName());
	}

}
