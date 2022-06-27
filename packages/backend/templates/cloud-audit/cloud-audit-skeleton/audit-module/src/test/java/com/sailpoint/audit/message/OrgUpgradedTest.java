/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */

package com.sailpoint.audit.message;

import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.messaging.server.MessageHandlerContext;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.atlas.task.schedule.service.TaskScheduleImportService;
import com.sailpoint.audit.service.AthenaDataCatalogService;
import com.sailpoint.atlas.service.MessageClientService;
import com.sailpoint.audit.service.FeatureFlags;
import com.sailpoint.audit.utils.TestUtils;
import com.sailpoint.mantis.core.service.CrudService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import sailpoint.object.Configuration;

import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OrgUpgradedTest {

	@Mock
	FeatureFlagService _featureFlagService;

	@Mock
	MessageHandlerContext _context;

	@Mock
	CrudService _crudService;

	@Mock
	Configuration _config;

	@Mock
	AtlasConfig _atlasConfig;

	@Mock
	AthenaDataCatalogService _athenaService;

	@Mock
	MessageClientService _messageClientService;

	@Mock
	TaskScheduleImportService _taskScheduleImportService;

	OrgUpgraded _orgUpgraded;

	private static final String ORG_NAME = "org_mock";
	private static final String DB_NAME = "DB_MOCK";
	private static final String S3_BUCKET_NAME = "S3_MOCK";


	@Before
	public void setUp(){
		when(_featureFlagService.getBoolean(any(FeatureFlags.class), anyBoolean())).thenReturn(true);
		when(_featureFlagService.getBoolean(any(FeatureFlags.class))).thenReturn(true);

		RequestContext.set(TestUtils.setDummyRequestContext(ORG_NAME));

		_orgUpgraded = new OrgUpgraded();
		_orgUpgraded._featureFlagService = _featureFlagService;
		_orgUpgraded._crudService = _crudService;
		_orgUpgraded._athenaService = _athenaService;
		_orgUpgraded._atlasConfig = _atlasConfig;
		_orgUpgraded._messageClientService = _messageClientService;
		_orgUpgraded._taskScheduleImportService = _taskScheduleImportService;

		when(_crudService.findByName(any(), anyString())).thenReturn(Optional.of(_config));
		when(_config.getId()).thenReturn("id");
	}

	@Test
	public void testHandleMessage() throws Exception{
		_orgUpgraded.handleMessage(_context);
	}

	@Test
	public void testPayLoadType() {
		Assert.assertTrue(OrgUpgraded.PAYLOAD_TYPE.values().length > 0);
		Assert.assertTrue(OrgUpgraded.PAYLOAD_TYPE.valueOf("ORG_UPGRADED") != null);
	}

	@Test
	public void testHandleMessageProvisionAthenaTable() throws Exception{
		when(_featureFlagService.getBoolean(FeatureFlags.SP_AUDIT_PROVISION_ATHENA_TABLE, eq(anyBoolean()))).thenReturn(true);
		when(_atlasConfig.getString("AER_AUDIT_ATHENA_DATABASE")).thenReturn(DB_NAME);
		when(_atlasConfig.getString("AER_AUDIT_PARQUET_DATA_S3_BUCKET")).thenReturn(S3_BUCKET_NAME);

		_orgUpgraded.handleMessage(_context);
		verify(_athenaService, times(1)).createTable(DB_NAME, ORG_NAME+"_audit_data",
				S3_BUCKET_NAME, "parquet" );
		verify(_taskScheduleImportService, times(1))
				.importSchedulesFromResource(eq("add_athena_partitions_schedule.json"));
	}

	@Test
	public void testHandleMessageProvisionAthenaTableWithFFOff() throws Exception{
		when(_featureFlagService.getBoolean(FeatureFlags.SP_AUDIT_PROVISION_ATHENA_TABLE, eq(anyBoolean()))).thenReturn(false);
		_orgUpgraded.handleMessage(_context);
		verifyZeroInteractions(_athenaService);
		verifyZeroInteractions(_taskScheduleImportService);
	}

	@Test
	public void testHandleMessageAddAthenaPartitionsFFEnabled() throws Exception{
		when(_featureFlagService.getBoolean(FeatureFlags.CLEANUP_BULKUPLOAD_CONFIG)).thenReturn(false);
		when(_featureFlagService.getBoolean(FeatureFlags.SP_AUDIT_PROVISION_ATHENA_TABLE, false)).thenReturn(false);
		when(_featureFlagService.getBoolean(FeatureFlags.PLTDP_ONETIME_BULKSYNC_CUSTOM)).thenReturn(false);
		when(_featureFlagService.getBoolean(FeatureFlags.PLTDP_ADD_ATHENA_PARTIONS_CUSTOM, eq(anyBoolean()))).thenReturn(true);
		_orgUpgraded.handleMessage(_context);
		verify(_messageClientService, times(140)).submitJob(any(), any());
	}
}
