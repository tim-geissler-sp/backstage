/*
 * Copyright (C) 2021 SailPoint Technologies, Inc.  All rights reserved.
 */

package com.sailpoint.audit.message;

import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.messaging.server.MessageHandlerContext;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.atlas.task.schedule.service.TaskScheduleImportService;
import com.sailpoint.audit.service.AthenaDataCatalogService;
import com.sailpoint.audit.service.FeatureFlags;
import com.sailpoint.audit.utils.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class OrgCreatedTest {

	@Mock
	FeatureFlagService _featureFlagService;

	@Mock
	MessageHandlerContext _context;

	@Mock
	AtlasConfig _atlasConfig;

	@Mock
	TaskScheduleImportService _taskScheduleImportService;

	@Mock
	AthenaDataCatalogService _athenaService;

	OrgCreated _orgcreated;

	private static final String ORG_NAME = "org_mock";
	private static final String DB_NAME = "DB_MOCK";
	private static final String S3_BUCKET_NAME = "S3_MOCK";

	@Before
	public void setUp(){
		RequestContext.set(TestUtils.setDummyRequestContext(ORG_NAME));

		_orgcreated = new OrgCreated();
		_orgcreated._featureFlagService = _featureFlagService;
		_orgcreated._athenaService = _athenaService;
		_orgcreated._atlasConfig = _atlasConfig;
		_orgcreated._taskScheduleImportService = _taskScheduleImportService;
	}

	@Test
	public void testHandleMessage() throws Exception{
		_orgcreated.handleMessage(_context);
	}

	@Test
	public void testPayLoadType() {
		Assert.assertTrue(OrgCreated.PAYLOAD_TYPE.values().length > 0);
		Assert.assertNotNull(OrgCreated.PAYLOAD_TYPE.valueOf("ORG_CREATED"));
	}

	@Test
	public void testHandleMessageProvisionAthenaTable() throws Exception{
		when(_atlasConfig.getString("AER_AUDIT_ATHENA_DATABASE")).thenReturn(DB_NAME);
		when(_atlasConfig.getString("AER_AUDIT_PARQUET_DATA_S3_BUCKET")).thenReturn(S3_BUCKET_NAME);

		_orgcreated.handleMessage(_context);
		verify(_athenaService, times(1)).createTable(DB_NAME, ORG_NAME+"_audit_data",
				S3_BUCKET_NAME, "parquet" );
		verify(_taskScheduleImportService, times(1))
				.importSchedulesFromResource(eq("add_athena_partitions_schedule.json"));
	}

}