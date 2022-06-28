/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service;

import com.sailpoint.atlas.ApplicationInfo;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.OrgData;
import com.sailpoint.atlas.OrgDataProvider;
import com.sailpoint.atlas.messaging.client.Job;
import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.atlas.service.MessageClientService;
import com.sailpoint.audit.message.BulkUploadAuditEvents;
import com.sailpoint.audit.message.BulkUploadPayload;
import com.sailpoint.audit.service.model.JobTypes;
import com.sailpoint.audit.service.model.ScheduleProperties;
import com.sailpoint.audit.util.RedisDistributedLock;
import com.sailpoint.audit.util.RedisExpiringSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SyncJobSchedulerTest {
	@Mock
	ApplicationInfo _applicationInfo;

	@Mock
	FeatureFlagService _featureFlagService;

	@Mock
	MessageClientService _messageClientService;

	@Mock
	OrgDataProvider _orgDataProvider;

	@Mock
	RedisExpiringSet _completedSet;

	@Mock
	RedisExpiringSet _runningJobsSet;

	@Mock
	RedisDistributedLock _redisDistributedLock;

	@Mock
	AtlasConfig _atlasConfig;

	@Mock
	ScheduleProperties _scheduleProperties;

	@Mock
	RedisPool _redisPool;

	@Mock
	Job _job;

	@Mock
	SyncJobManager _syncJobManager;

	SyncJobScheduler _syncJobScheduler;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);

		when(_redisDistributedLock.acquireLock()).thenReturn(true);
		when(_atlasConfig.getPods()).thenReturn(new HashSet<>(Arrays.asList("testpod1", "testpod2")));
		when(_atlasConfig.getInt(anyString(), anyInt())).thenReturn(5);
		when(_orgDataProvider.findAll("testpod1")).thenReturn(Arrays.asList(new OrgData("testpod1","org1"),
				new OrgData("testpod1","org2")));
		when(_orgDataProvider.findAll("testpod2")).thenReturn(Arrays.asList(new OrgData("testpod2","org1"), new OrgData("testpod2","org2")));
		when(_featureFlagService.getBoolean(Matchers.any(FeatureFlags.class), any(), anyBoolean())).thenReturn(true);
		when(_applicationInfo.getStack()).thenReturn("aer");
		when(_completedSet.contains(anyString())).thenReturn(false);
		when(_runningJobsSet.contains(anyString())).thenReturn(true);

		_scheduleProperties = new ScheduleProperties(1);

		BulkUploadPayload bulkUploadPayload = new BulkUploadPayload();
		Map<String, Object> arguments = new HashMap<>();
		arguments.put("batchSize", 10000);
		arguments.put("recordLimit", 1000000);
		bulkUploadPayload.setArguments(arguments);
		_scheduleProperties.addJobProperty(FeatureFlags.BULK_UPLOAD_AUDIT_EVENTS,
				JobTypes.upload,
				"AUDIT",
				BulkUploadAuditEvents.PAYLOAD_TYPE.BULK_UPLOAD_AUDIT_EVENTS.name(),
				bulkUploadPayload,
				60*24,
				false,
				5);

		_syncJobScheduler = new SyncJobScheduler();
		_syncJobScheduler._applicationInfo = _applicationInfo;
		_syncJobScheduler._atlasConfig = _atlasConfig;
		_syncJobScheduler._featureFlagService = _featureFlagService;
		_syncJobScheduler._messageClientService = _messageClientService;
		_syncJobScheduler._orgDataProvider = _orgDataProvider;
		_syncJobScheduler._redisPool = _redisPool;
		_syncJobScheduler._syncJobManager = _syncJobManager;
		_syncJobScheduler.init(_scheduleProperties);
		_syncJobScheduler._redisDistributedLock = _redisDistributedLock;

		when(_messageClientService.submitJob(any(), any(), any(), anyString(), anyString()))
				.thenReturn(_job);
	}

	@Test
	public void testJobScheduler() {
		_syncJobScheduler.runJobs();

		verify(_messageClientService, times(8))
				.submitJob(any(), any(), any(), anyString(), anyString());
	}

	@Test
	public void testJobSchedulerMaxReached() {
		when(_syncJobManager.runningJobCount(anyObject())).thenReturn(6);

		_syncJobScheduler.runJobs();

		verify(_messageClientService, times(0))
				.submitJob(any(), any(), any(), anyString(), anyString());
	}
}
