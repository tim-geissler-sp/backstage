/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service;

import com.google.inject.Provider;
import com.sailpoint.atlas.ApplicationInfo;
import com.sailpoint.atlas.message.MessageScopeDescriptor;
import com.sailpoint.atlas.message.MessageScopeFactory;
import com.sailpoint.atlas.messaging.client.Job;
import com.sailpoint.atlas.messaging.client.JobStore;
import com.sailpoint.atlas.messaging.client.MessageScope;
import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import com.sailpoint.audit.message.BulkUploadAuditEvents;
import com.sailpoint.audit.message.BulkUploadPayload;
import com.sailpoint.audit.service.model.JobTypes;
import com.sailpoint.audit.service.model.ScheduleProperties;
import com.sailpoint.audit.util.RedisExpiringSet;
import com.sailpoint.audit.utils.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SyncJobManagerTest {

	@Mock
	ApplicationInfo _applicationInfo;

	@Mock
	Job _job;

	@Mock
	JobStore _jobStore;

	@Mock
	Provider<JobStore> _jobStoreProvider;

	@Mock
	MessageScope _messageScope;

	@Mock
	MessageScopeDescriptor _messageScopeDescriptor;

	@Mock
	MessageScopeFactory _messageScopeFactory;

	@Mock
	RedisPool _redisPool;

	@Mock
	RedisExpiringSet _runningJobsSet;

	@Mock
	RedisExpiringSet _completedOrgsSet;

	SyncJobManager _syncJobManager;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		TestUtils.setDummyRequestContext();

		BulkUploadPayload bulkUploadResetPayload = new BulkUploadPayload();
		bulkUploadResetPayload.setReset(true);

		ScheduleProperties _scheduleProperties = new ScheduleProperties(1);
		_scheduleProperties.addJobProperty(FeatureFlags.BULK_UPLOAD_AUDIT_EVENTS_RESET,
				JobTypes.upload_reset,
				"AUDIT",
				BulkUploadAuditEvents.PAYLOAD_TYPE.BULK_UPLOAD_AUDIT_EVENTS.name(),
				bulkUploadResetPayload,
				24*60,
				false,
				100);

		Set<String> jobs = new HashSet<>(Arrays.asList("dev/acme-solar/upload_reset/123"));
		when(_runningJobsSet.getAll()).thenReturn(jobs);
		when(_messageScopeFactory.getMessageScope(anyObject(), anyString(), anyString())).thenReturn(_messageScope);
		when(_jobStoreProvider.get()).thenReturn(_jobStore);
		when(_jobStore.find(_messageScope, "123")).thenReturn(_job);
		_syncJobManager = new SyncJobManager();
		_syncJobManager._applicationInfo = _applicationInfo;
		_syncJobManager._redisPool = _redisPool;
		_syncJobManager.init(_scheduleProperties);
		_syncJobManager._completedOrgsSet = _completedOrgsSet;
		_syncJobManager._runningJobsSet = _runningJobsSet;
		_syncJobManager._messageScopeFactory = _messageScopeFactory;
		_syncJobManager._jobStore = _jobStoreProvider;
		_syncJobManager._scheduleProperties = _scheduleProperties;
	}

	@Test
	public void testIsJobRunning() {
		when(_job.isActive()).thenReturn(true);

		assertTrue(_syncJobManager.isJobRunning(_messageScopeDescriptor, JobTypes.upload_reset, "dev", "acme-solar"));
	}

	@Test
	public void testClearCompletedJobs() {
		when(_job.isComplete()).thenReturn(true);

		_syncJobManager.clearCompletedJobs(_messageScopeDescriptor, JobTypes.upload_reset, false);

		verify(_runningJobsSet, times(1))
				.remove(anyString());
	}

	@Test
	public void addJob() {
		_syncJobManager.addJob("dev", "acme-solar", JobTypes.upload_reset, "123");

		verify(_runningJobsSet, times(1))
				.add(eq("dev/acme-solar/upload_reset/123"), eq(2L), eq(TimeUnit.DAYS));
	}

	@Test
	public void testIsOrgComplete() {
		_syncJobManager.isOrgComplete("dev", "acme-solar", JobTypes.upload_reset);

		verify(_completedOrgsSet, times(1))
				.contains(eq("dev/acme-solar/upload_reset"));
	}

	@Test
	public void testRemoveJob() {
		_syncJobManager.removeJob("dev", "acme-solar", JobTypes.upload_reset, "123");

		verify(_runningJobsSet, times(1))
				.remove(eq("dev/acme-solar/upload_reset/123"));
	}

	@Test
	public void testResetStatusComplete() {
		_syncJobManager.resetStatusComplete("dev", "acme-solar", JobTypes.upload_reset);

		verify(_completedOrgsSet, times(1))
				.remove(eq("dev/acme-solar/upload_reset"));
	}

	@Test
	public void testResetStatusCompleteNullCheck() {
		_syncJobManager._completedOrgsSet = null;

		_syncJobManager.resetStatusComplete("dev", "acme-solar", JobTypes.upload_reset);

		verify(_completedOrgsSet, times(0))
				.remove(eq("dev/acme-solar/upload_reset"));
	}

	@Test
	public void testSetStatusComplete() {
		_syncJobManager.setStatusComplete("dev", "acme-solar", JobTypes.upload_reset);

		verify(_completedOrgsSet, times(1))
				.add(eq("dev/acme-solar/upload_reset"), anyLong(), anyObject());
	}

	@Test
	public void testRunningJobCount() {
		int count = _syncJobManager.runningJobCount(JobTypes.upload_reset);

		assertEquals(1, count);
	}
}
