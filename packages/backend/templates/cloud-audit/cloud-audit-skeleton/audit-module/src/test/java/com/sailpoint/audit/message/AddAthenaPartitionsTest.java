/*
 * Copyright (C) 2021 SailPoint Technologies, Inc.  All rights reserved.
 */

package com.sailpoint.audit.message;

import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.messaging.server.MessageHandlerContext;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.audit.service.FeatureFlags;
import com.sailpoint.audit.service.model.AddAthenaPartitionsDTO;
import com.sailpoint.audit.util.AthenaPartitionManager;
import com.sailpoint.atlas.messaging.client.Message;
import com.sailpoint.audit.utils.TestUtils;
import com.sailpoint.utilities.JsonUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AddAthenaPartitionsTest {

	@Mock
	FeatureFlagService _featureFlagService;

	@Mock
	MessageHandlerContext _context;

	@Mock
	Message _message;

	AddAthenaPartitions _addAthenaPartitions;

	private static final String ORG_NAME = "org_mock";

	@Before
	public void setUp(){
		RequestContext.set(TestUtils.setDummyRequestContext(ORG_NAME));

		_addAthenaPartitions = new AddAthenaPartitions();
		_addAthenaPartitions._featureFlagService = _featureFlagService;

		when(_featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, eq(anyBoolean()))).thenReturn(true);
		when(_context.getMessage()).thenReturn(_message);
		when(_message.getContentJson()).thenReturn(JsonUtil.toJson(new AddAthenaPartitionsDTO()));
	}

	@Test
	public void testHandleMessage() throws Exception{
		_addAthenaPartitions.handleMessage(_context);
	}

	@Test
	public void testPayLoadType() {
		Assert.assertTrue(OrgCreated.PAYLOAD_TYPE.values().length > 0);
		Assert.assertNotNull(AddAthenaPartitions.PAYLOAD_TYPE.valueOf("ADD_ATHENA_PARTITIONS"));
	}

	@Test
	public void testHandleMessageProvisionAthenaTableWithFFOff() throws Exception{
		when(_featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, eq(anyBoolean()))).thenReturn(false);
		long previousCount = AthenaPartitionManager.submissionCounter.get();
		_addAthenaPartitions.handleMessage(_context);
		assertTrue("Should get zero submissions", previousCount == AthenaPartitionManager.submissionCounter.get());
	}
}
