/*
 *
 *  Copyright (c) 2021.  SailPoint Technologies, Inc. All rights reserved.
 *
 */

package com.sailpoint.audit.event;

import com.amazonaws.regions.Regions;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.atlas.service.RemoteFileService;
import com.sailpoint.audit.service.AthenaDataCatalogService;
import com.sailpoint.audit.service.DeletedOrgsCacheService;
import com.sailpoint.audit.service.FeatureFlags;
import com.sailpoint.audit.service.util.AuditUtil;
import com.sailpoint.audit.utils.TestUtils;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.PodTopic;
import com.sailpoint.iris.server.EventHandlerContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;

@RunWith(MockitoJUnitRunner.class)
public class OrgDeleteHandlerTest {

	OrgDeleteHandler _sut;

	private static final String ORG_NAME = "ORG_MOCK";
	private static final String DB_NAME = "DB_MOCK";
	private static final String S3_BUCKET_NAME = "S3_MOCK";

	@Mock
	DeletedOrgsCacheService _deletedOrgsCache;

	@Mock
	private EventHandlerContext _eventHandlerContext;

	@Mock
	AtlasConfig _atlasConfig;

	@Mock
	AthenaDataCatalogService _athenaService;

	@Mock
	FeatureFlagService _featureFlagService;

	@Mock
	RemoteFileService _remoteFileService;

	@Before
	public void setUp() {
		RequestContext requestContext = TestUtils.setDummyRequestContext(ORG_NAME);

		when(_eventHandlerContext.getTopic()).thenReturn(new PodTopic(IdnTopic.ORG_LIFECYCLE.getName(), requestContext.getPod()));
		when(_eventHandlerContext.getEvent()).thenReturn(new Event("ORG_DELETED", Collections.emptyMap(), "{}"));

		_sut = new OrgDeleteHandler();
		_sut._deletedOrgsCache = _deletedOrgsCache;
		_sut._athenaService = _athenaService;
		_sut._atlasConfig = _atlasConfig;
		_sut._featureFlagService = _featureFlagService;
		_sut._s3FileService = _remoteFileService;

		when(_atlasConfig.getString("AER_AUDIT_ATHENA_DATABASE")).thenReturn(DB_NAME);
		when(_atlasConfig.getString("AER_AUDIT_PARQUET_DATA_S3_BUCKET")).thenReturn(S3_BUCKET_NAME);
	}

	@Test
	public void handleEvent() throws InterruptedException {
		when(_featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, eq(anyBoolean()))).thenReturn(true);
		when(_atlasConfig.getAwsRegion()).thenReturn(Regions.US_EAST_1);

		_sut.handleEvent(_eventHandlerContext);
		verify(_athenaService, times(1)).deleteTable(DB_NAME, AuditUtil.getOrgAuditAthenaTableName(ORG_NAME), S3_BUCKET_NAME);
		String[] stringPrefixes = {"parquet/org="+ORG_NAME};
		verify(_remoteFileService, times(1)).deleteMultipleObjects(eq(S3_BUCKET_NAME), eq(stringPrefixes), anyString());
	}

	@Test
	public void handleEventFlagOff() throws InterruptedException {
		_sut.handleEvent(_eventHandlerContext);
		verifyZeroInteractions(_athenaService);
		verifyZeroInteractions(_remoteFileService);
	}
}
