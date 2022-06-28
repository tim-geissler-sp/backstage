/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.sailpoint.atlas.ApplicationInfo;
import com.sailpoint.atlas.message.MessageScopeDescriptor;
import com.sailpoint.atlas.message.MessageScopeFactory;
import com.sailpoint.atlas.messaging.client.Job;
import com.sailpoint.atlas.messaging.client.JobStore;
import com.sailpoint.atlas.messaging.client.MessageScope;
import com.sailpoint.atlas.messaging.client.Status;
import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import com.sailpoint.audit.service.model.JobTypes;
import com.sailpoint.audit.service.model.ScheduleProperties;
import com.sailpoint.audit.util.RedisExpiringSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class SyncJobManager {
	@Inject
	Provider<JobStore> _jobStore;

	@Inject
	MessageScopeFactory _messageScopeFactory;

	@Inject
	RedisPool _redisPool;

	@Inject
	ApplicationInfo _applicationInfo;

	protected RedisExpiringSet _completedOrgsSet;

	protected RedisExpiringSet _runningJobsSet;

	protected ScheduleProperties _scheduleProperties;

	public void init(ScheduleProperties scheduleProperties) {
		_scheduleProperties = scheduleProperties;
		_completedOrgsSet = new RedisExpiringSet(_redisPool, _applicationInfo.getStack() + "/org-audit-bulk-upload-complete");
		_runningJobsSet = new RedisExpiringSet(_redisPool, _applicationInfo.getStack() + "/org-audit-bulk-upload-jobs");
	}

	public boolean isJobRunning(MessageScopeDescriptor messageScopeDescriptor, JobTypes jobType, String pod, String org) {
		AtomicBoolean isJobRunning = new AtomicBoolean(false);
		MessageScope messageScope = _messageScopeFactory.getMessageScope(messageScopeDescriptor, pod, org);
		_runningJobsSet.getAll().forEach(podOrgJob -> {
			if (podOrgJob.startsWith(getKey(pod, org, jobType) + "/")) {
				String jobId = podOrgJob.substring(podOrgJob.lastIndexOf("/") + 1);
				Job job = _jobStore.get().find(messageScope, jobId);
				if (job != null && job.isActive()) {
					isJobRunning.set(true);
				};
			}
		});
		return isJobRunning.get();
	}

	public int runningJobCount(JobTypes jobType) {
		AtomicInteger count = new AtomicInteger(0);
		_runningJobsSet.getAll().forEach(podOrgJobTypeKey -> {
			String[] podOrgJob = podOrgJobTypeKey.split("/");
			if (podOrgJob.length==4) {
				String jobTypeStr = podOrgJob[2];
				if (jobTypeStr.equals(jobType.name())) {
					count.getAndIncrement();
				}
			}
		});
		return count.get();
	}

	public void clearCompletedJobs(MessageScopeDescriptor messageScopeDescriptor, JobTypes jobType, boolean externalCompletionStatus) {
		_runningJobsSet.getAll().forEach(podOrgJobTypeKey -> {
			String[] podOrgJob = podOrgJobTypeKey.split("/");
			if (podOrgJob.length==4) {
				String pod = podOrgJob[0];
				String org = podOrgJob[1];
				String jobTypeStr = podOrgJob[2];
				String jobId = podOrgJob[3];
				if (jobTypeStr.equals(jobType.name())) {
					MessageScope messageScope = _messageScopeFactory.getMessageScope(messageScopeDescriptor, pod, org);
					Job job = _jobStore.get().find(messageScope, jobId);
					if (job != null && job.isComplete()) {
						if (!externalCompletionStatus || job.getStatus()==Status.ERROR) {
							setStatusComplete(pod, org, jobType);
						}
						_runningJobsSet.remove(getKey(pod, org, jobType) + "/" + jobId);
					}
				}
			}
		});
	}

	public void addJob(String pod, String org, JobTypes jobType, String jobId) {
		_runningJobsSet.add(getKey(pod, org, jobType) + "/"  + jobId,2, TimeUnit.DAYS);
	}

	public void removeJob(String pod, String org, JobTypes jobType, String jobId) {
		_runningJobsSet.remove(getKey(pod, org, jobType) + "/" + jobId);
	}

	public void setStatusComplete(String pod, String org, JobTypes jobType) {
		_completedOrgsSet.add( getKey(pod, org, jobType),
				_scheduleProperties.getExpirationHours(jobType), TimeUnit.HOURS);
	}

	public void resetStatusComplete(String pod, String org, JobTypes jobType) {
		if ( _completedOrgsSet == null) {
			_completedOrgsSet = new RedisExpiringSet(_redisPool, _applicationInfo.getStack() + "/org-audit-bulk-upload-complete");
			return;
		}

		_completedOrgsSet.remove(getKey(pod, org, jobType));
	}

	public boolean isOrgComplete(String pod, String org, JobTypes jobType) {
		return  _completedOrgsSet.contains(pod + '/' + org + '/' + jobType);
	}

	private String getKey(String pod, String org, JobTypes jobType) {
		return pod + '/' + org + '/' + jobType;
	}
}
