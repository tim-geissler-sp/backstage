/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service.model;

import com.sailpoint.atlas.idn.IdnMessageScope;
import com.sailpoint.atlas.message.MessageScopeDescriptor;
import com.sailpoint.audit.service.FeatureFlags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleProperties {

	public class JobProperties {

		private final FeatureFlags _featureFlag;
		private final JobTypes _jobType;
		private final Object _payload;
		private final String _payloadType;
		private final String _scope;
		private final Integer _expirationHours;
		private final Boolean _externalCompletionStatus;
		private final Integer _maxRunningJobCount;

		public JobProperties(FeatureFlags featureFlag, JobTypes jobType,
							 String scope, String payloadType, Object payload,
							 int expirationHours, boolean externalCompletionStatus,
							 int maxRunningJobCount) {
			_featureFlag = featureFlag;
			_jobType = jobType;
			_scope = scope;
			_payloadType = payloadType;
			_payload = payload;
			_expirationHours = expirationHours;
			_externalCompletionStatus = externalCompletionStatus;
			_maxRunningJobCount = maxRunningJobCount;
		}

		public FeatureFlags getFeatureFlag() {
			return _featureFlag;
		}

		public JobTypes getJobType() {
			return _jobType;
		}

		public MessageScopeDescriptor getScope() {
			return IdnMessageScope.find(_scope);
		}

		public String getPayloadType() {
			return _payloadType;
		}

		public Object getPayload() {
			return _payload;
		}

		public Integer getExpirationHours() {
			return _expirationHours;
		}

		public Boolean isExternalCompletionStatus() {
			return _externalCompletionStatus;
		}

		public Integer getMaxRunningJobCount() {
			return _maxRunningJobCount;
		}
	}

	private List<JobProperties> _jobProperties;
	private Map<JobTypes,Integer> _jobTypeExpirationHoursMap;
	private final int _scheduleDelayMinutes;

	public ScheduleProperties(int delayMinutes) {
		_scheduleDelayMinutes = delayMinutes;
		_jobProperties = new ArrayList<>();
		_jobTypeExpirationHoursMap = null;
	}

	public void addJobProperty(FeatureFlags featureFlag, JobTypes jobType,
							   String scope, String payloadType, Object payload,
							   int expirationHours, boolean externalCompletionStatus,
							   int maxRunningJobCount) {
		_jobProperties.add(new JobProperties(featureFlag, jobType, scope, payloadType, payload, expirationHours, externalCompletionStatus, maxRunningJobCount));
	}

	public int getDelayMinutes() {
		return _scheduleDelayMinutes;
	}

	public int getExpirationHours(JobTypes jobType) {
		if (_jobTypeExpirationHoursMap==null) {
			_jobTypeExpirationHoursMap = new HashMap<>();
			_jobProperties.forEach(jobProperties ->
				_jobTypeExpirationHoursMap.put(jobProperties.getJobType(), jobProperties.getExpirationHours())
			);
		}
		return _jobTypeExpirationHoursMap.get(jobType);
	}

	public List<JobProperties> getJobProperties() {
		return _jobProperties;
	}
}
