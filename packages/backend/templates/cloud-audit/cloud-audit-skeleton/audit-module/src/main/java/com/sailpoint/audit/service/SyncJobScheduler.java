/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.sailpoint.atlas.ApplicationInfo;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.OrgData;
import com.sailpoint.atlas.OrgDataProvider;
import com.sailpoint.atlas.messaging.client.Job;
import com.sailpoint.atlas.messaging.client.JobStore;
import com.sailpoint.atlas.messaging.client.JobSubmission;
import com.sailpoint.atlas.messaging.client.Payload;
import com.sailpoint.atlas.messaging.client.SendMessageOptions;
import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.atlas.service.MessageClientService;
import com.sailpoint.audit.service.model.JobTypes;
import com.sailpoint.audit.service.model.ScheduleProperties;
import com.sailpoint.audit.util.RedisDistributedLock;
import com.sailpoint.featureflag.FeatureFlagClient;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
public class SyncJobScheduler {

	@Inject
	ApplicationInfo _applicationInfo;

	@Inject
	AtlasConfig _atlasConfig;

	@Inject
	FeatureFlagService _featureFlagService;

	@Inject
	MessageClientService _messageClientService;

	@Inject
	OrgDataProvider _orgDataProvider;

	@Inject
	RedisPool _redisPool;

	@Inject
	SyncJobManager _syncJobManager;

	private final static String MAX_RUNNING_JOB_COUNT = "MAX_RUNNING_JOB_COUNT";

	protected RedisDistributedLock _redisDistributedLock;
	private ScheduledExecutorService _executorService;
	private ScheduleProperties _scheduleProperties;

	private static Log _log = LogFactory.getLog(SyncJobScheduler.class);

	public void init(String resourceFileName) {
		ScheduleProperties scheduleProperties = getScheduleProperties(resourceFileName);
		init(scheduleProperties);
	}

	public void init(ScheduleProperties scheduleProperties) {
		_syncJobManager.init(scheduleProperties);
		_scheduleProperties = scheduleProperties;
		_redisDistributedLock = new RedisDistributedLock(_redisPool,
				_applicationInfo.getStack() + "/dist-lock/" + StringUtils.join(_atlasConfig.getPods(), '-'),
				Duration.ofMinutes(2L).toMillis());

		_executorService = Executors.newSingleThreadScheduledExecutor();
		_executorService.scheduleAtFixedRate(this::runJobs, 0, scheduleProperties.getDelayMinutes(), TimeUnit.MINUTES);
	}

	void runJobs() {
		if (_redisDistributedLock.acquireLock()) {
			try {
				_scheduleProperties.getJobProperties().forEach(jobProperties -> {
					_syncJobManager.clearCompletedJobs(jobProperties.getScope(), jobProperties.getJobType(), jobProperties.isExternalCompletionStatus());

					final Map<Boolean, List<OrgData>> orgsByEnabledFeatureFlag = _atlasConfig.getPods()
							.stream()
							.map(_orgDataProvider::findAll)
							.flatMap(Collection::stream)
							.collect(Collectors.partitioningBy(orgData -> isFeatureFlagEnabled(orgData, jobProperties.getFeatureFlag())));

					// Submit job for feature flag enabled orgs, return map of submitted jobs by org.
					orgsByEnabledFeatureFlag.get(Boolean.TRUE)
							.stream()
							.filter(orgData -> !isOrgCompleted(orgData, jobProperties.getJobType()))
							.forEach(orgData -> submitJob(orgData, jobProperties));
				});
			} catch (Exception e) {
				_log.error("Failed to submit job", e);
			} finally {
				// Wait for 30 seconds to make sure the other schedulers have been attempted.
				try { Thread.sleep(30000); } catch (InterruptedException ie) {}
				_redisDistributedLock.releaseLock();
			}
		}
	}

	private void submitJob(OrgData orgData, ScheduleProperties.JobProperties jobProperties) {

		final JobSubmission jobSubmission = new JobSubmission(new Payload(jobProperties.getPayloadType(), jobProperties.getPayload()));

		if (_syncJobManager.isJobRunning(jobProperties.getScope(), jobProperties.getJobType(), orgData.getPod(), orgData.getOrg())) {
			_log.debug(jobProperties.getJobType()+" job already running");
			return;
		}
		int maxRunningJobCount = _atlasConfig.getInt(jobProperties.getJobType().name().toUpperCase()+"_"+MAX_RUNNING_JOB_COUNT, jobProperties.getMaxRunningJobCount());
		if (_syncJobManager.runningJobCount(jobProperties.getJobType()) >= maxRunningJobCount) {
			_log.debug(jobProperties.getJobType()+" max job count reached");
			return;
		}
		Job job = _messageClientService.submitJob(jobProperties.getScope(), jobSubmission, new SendMessageOptions(), orgData.getPod(), orgData.getOrg());
		_syncJobManager.addJob(orgData.getPod(), orgData.getOrg(), jobProperties.getJobType(), job.getId());

		_log.info("job bulk "+jobProperties.getJobType()+" queued for pod: " + orgData.getPod() + ", org: " + orgData.getOrg() + " -- jobId: " + job.getId());
	}

	private boolean isFeatureFlagEnabled(OrgData orgData, FeatureFlags featureFlag) {
		return _featureFlagService.getBoolean(
				featureFlag,
				FeatureFlagClient.buildFeatureUser(orgData.getPod(), orgData.getOrg(), _applicationInfo.getStack()),
				false
		);
	}

	private boolean isOrgCompleted(OrgData orgData, JobTypes jobType) {
		return _syncJobManager.isOrgComplete(orgData.getPod(), orgData.getOrg(), jobType);
	}

	public ScheduleProperties getScheduleProperties(String resourceFileName) {
		ScheduleProperties scheduleProperties = null;
		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceFileName)) {
			String json = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
			scheduleProperties = JsonUtils.parse(ScheduleProperties.class, json);
		} catch (Exception e) {
			_log.error("error loading schedule properties from schedule_properties.json file", e);
		}
		return scheduleProperties;
	}
}
