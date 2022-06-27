/*
 *
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 *
 */

package com.sailpoint.audit.rest;

import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.OrgData;
import com.sailpoint.atlas.OrgDataProvider;
import com.sailpoint.atlas.service.MessageClientService;
import com.sailpoint.audit.message.BulkSyncPayload;
import com.sailpoint.audit.message.BulkUploadPayload;
import com.sailpoint.audit.message.CisToS3Payload;
import com.sailpoint.audit.service.SyncCisToS3Service;
import com.sailpoint.utilities.JsonUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class BulkSyncEventsResourceTest {

	@Mock
	MessageClientService _messageClientService;

	@Mock
	SyncCisToS3Service _syncCisToS3Service;

	@Mock
	OrgDataProvider _orgDataProvider;
	
	@Mock
	AtlasConfig _atlasConfig;

	BulkSyncEventsResource _bulkResource;

	@Before
	public void setUp() {

		MockitoAnnotations.initMocks(this);

		_bulkResource = new BulkSyncEventsResource();
		_bulkResource._messageClientService = _messageClientService;
		_bulkResource._syncCisToS3Service = _syncCisToS3Service;
		_bulkResource._orgDataProvider = _orgDataProvider;
		_bulkResource._atlasConfig = _atlasConfig;

		OrgData orgData = new OrgData();
		orgData.setOrg("acme-solar");
		orgData.setPod("dev");
		orgData.setAttribute("someKey", "someValue");
		orgData.setTenantId(UUID.randomUUID().toString());

		when(_orgDataProvider.ensureFind(anyString())).thenReturn(orgData);
		when(_orgDataProvider.findAll(anyString())).thenReturn(Collections.singletonList(orgData));
		when(_atlasConfig.getPods()).thenReturn(Collections.singletonMap("dev","dev").keySet());

	}

	@Test
	public void testRunBulkSync() throws Exception{
		Response resp = _bulkResource.runBulkSync(JsonUtil.parse(BulkSyncPayload.class, "{}"));

		Assert.assertNotNull(resp);
		Assert.assertEquals(resp.getStatus(), 200);
	}

	@Test
	public void testCount() throws Exception{
		Response resp = _bulkResource.count();

		Assert.assertNotNull(resp);
		Assert.assertEquals(resp.getStatus(), 200);
	}


	@Test
	public void testResetStatus() throws Exception{
		Response resp = _bulkResource.resetStatus(JsonUtil.parse(BulkUploadPayload.class, "{}"));

		Assert.assertNotNull(resp);
		Assert.assertEquals(resp.getStatus(), 200);
	}

	@Test
	public void testRunBulkS3Sync() throws Exception{
		Response resp = _bulkResource.runBulkS3Sync(JsonUtil.parse(BulkSyncPayload.class, "{}"));

		Assert.assertNotNull(resp);
		Assert.assertEquals(resp.getStatus(), 200);
	}

	@Test
	public void testUploadEventsIntoS3() throws Exception{
		Response resp = _bulkResource.uploadAuditEventsIntoS3(JsonUtil.parse(BulkUploadPayload.class, "{}"));

		Assert.assertNotNull(resp);
		Assert.assertEquals(resp.getStatus(), 200);
	}

	@Test
	public void testRunSyncReset() throws Exception{
		Response resp = _bulkResource.runSyncReset(JsonUtil.parse(BulkSyncPayload.class, "{}"));

		Assert.assertNotNull(resp);
		Assert.assertEquals(resp.getStatus(), 200);
	}

	@Test
	public void testRunBulkS3SyncReset() throws Exception{
		Response resp = _bulkResource.runBulkS3SyncReset(JsonUtil.parse(BulkSyncPayload.class, "{}"));

		Assert.assertNotNull(resp);
		Assert.assertEquals(resp.getStatus(), 200);
	}

	@Test
	public void testRunBulkS3SyncCount() throws Exception{
		Response resp = _bulkResource.runBulkS3SyncCount();

		Assert.assertNotNull(resp);
		Assert.assertEquals(resp.getStatus(), 200);
	}

	@Test
	public void testCis2S3BulkSync() throws Exception {

		// Case: Null arguments to the REST service.
		Response respNull = _bulkResource.postCis2S3BulkSync(null);
		Assert.assertNotNull(respNull);
		Assert.assertEquals(respNull.getStatus(), HttpServletResponse.SC_BAD_REQUEST);

		CisToS3Payload cisToS3Payload = new CisToS3Payload(); // No org name set intentionally.

		// Case: Missing org name in the payload.
		Response respNoOrg = _bulkResource.postCis2S3BulkSync(cisToS3Payload);
		Assert.assertNotNull(respNoOrg);
		Assert.assertEquals(respNoOrg.getStatus(), HttpServletResponse.SC_BAD_REQUEST);

		// Case single org:
		cisToS3Payload.setOrgName("acme-solar");
		Response respOneOrg = _bulkResource.postCis2S3BulkSync(cisToS3Payload);
		Assert.assertNotNull(respOneOrg);
		Assert.assertEquals(respOneOrg.getStatus(), HttpServletResponse.SC_OK);

		// Case all orgs:
		cisToS3Payload.setOrgName("*");
		Response respAllOrgs = _bulkResource.postCis2S3BulkSync(cisToS3Payload);
		Assert.assertNotNull(respAllOrgs);
		Assert.assertEquals(respAllOrgs.getStatus(), HttpServletResponse.SC_OK);

	}
}
