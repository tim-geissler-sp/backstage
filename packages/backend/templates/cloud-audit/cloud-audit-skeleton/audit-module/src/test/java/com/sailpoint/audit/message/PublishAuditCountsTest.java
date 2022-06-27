/*
 * Copyright (C) 2021 SailPoint Technologies, Inc.  All rights reserved.
 */

package com.sailpoint.audit.message;

import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.messaging.client.Message;
import com.sailpoint.atlas.messaging.server.MessageHandlerContext;
import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.audit.service.FeatureFlags;
import com.sailpoint.audit.service.MetricsPublisherService;
import com.sailpoint.audit.service.model.PublishAuditCountsDTO;
import com.sailpoint.audit.utils.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.anyString;

@RunWith(MockitoJUnitRunner.class)
public class PublishAuditCountsTest {

	@Mock
	FeatureFlagService _featureFlagService;

	@Mock
	MessageHandlerContext _context;

	@Mock
	AtlasConfig _atlasConfig;

	@Mock
	MetricsPublisherService _metricsPublisherService;

	PublishAuditCounts _publishAuditAccounts;

	private static final String ORG_NAME = "org_mock";
	private static final String DB_NAME = "DB_MOCK";
	private static final String S3_BUCKET_NAME = "S3_MOCK";

	@Before
	public void setUp(){
		RequestContext.set(TestUtils.setDummyRequestContext(ORG_NAME));

		_publishAuditAccounts = new PublishAuditCounts();
		_publishAuditAccounts._featureFlagService = _featureFlagService;
		_publishAuditAccounts._metricsPublisherService = _metricsPublisherService;
		_publishAuditAccounts._atlasConfig = _atlasConfig;
	}

	@Test
	public void testHandleMessage() throws Exception{
		_publishAuditAccounts.handleMessage(_context);
	}

	@Test
	public void testPayLoadType() {
		Assert.assertTrue(PublishAuditCounts.PAYLOAD_TYPE.values().length > 0);
		Assert.assertNotNull(PublishAuditCounts.PAYLOAD_TYPE.valueOf("PUBLISH_AUDIT_COUNTS"));
	}

	@Test
	public void testHandleMessagePublishAuditCounts() throws Exception{
		when(_featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, eq(anyBoolean()))).thenReturn(true);
		when(_atlasConfig.getString("AER_AUDIT_ATHENA_DATABASE")).thenReturn(DB_NAME);
		when(_atlasConfig.getString("AER_AUDIT_PARQUET_DATA_S3_BUCKET")).thenReturn(S3_BUCKET_NAME);
		String date = "2020-12-10";
		PublishAuditCountsDTO publishAuditCountsDTO = new PublishAuditCountsDTO(date);
		Message message = new Message(JsonUtils.toJson(publishAuditCountsDTO));
		when(_context.getMessage()).thenReturn(message);
		_publishAuditAccounts.handleMessage(_context);
		verify(_metricsPublisherService, times(1)).publishAuditEventCounts(eq(DB_NAME),
				eq(ORG_NAME), eq(date), eq(S3_BUCKET_NAME), anyString() );
	}

	@Test
	public void testHandleMessagePublishAuditCountsWithFFOff() throws Exception{
		when(_featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, eq(anyBoolean()))).thenReturn(false);
		_publishAuditAccounts.handleMessage(_context);
		verifyZeroInteractions(_metricsPublisherService);
	}
}