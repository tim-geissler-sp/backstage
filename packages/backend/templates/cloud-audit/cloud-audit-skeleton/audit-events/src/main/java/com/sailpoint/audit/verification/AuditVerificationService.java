/*
 * Copyright (C) 2022 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.verification;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.atlas.search.util.ThreadUtils;
import com.sailpoint.metrics.PrometheusMetricsUtil;
import com.sailpoint.utilities.JsonUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
/**
 * Service for submitting audit events to be verified
 */
public class AuditVerificationService {
	private static final String AUDIT_VERIFICATION_QUEUE_SUBMISSION_FAILURE_TOTAL = "audit.verification.queue.submission.failure.total";
	private static final String AUDIT_VERIFICATION_QUEUE_SUBMISSION_FAILURE_TOTAL_HELP = "Total number of times an audit verification message was not able to be submitted to SQS.";
	private static Log _log = LogFactory.getLog(AuditVerificationService.class);

	private static final int[] BACKOFF_DELAY = new int[] {5, 15, 30};

	public static final String ATTEMPT_DATA_TYPE = "Number";
	public static final String ATTEMPT_DEFAULT_VALUE = "0";
	public static final String ATTEMPT_ATTRIBUTE = "attempt";
	public static final String AUDIT_VERIFICATION_QUEUE_KEY = "SP_AUDIT_VERIFICATION_QUEUE_NAME";
	public static final String AUDIT_VERIFICATION_QUEUE_DEFAULT = "spt-audit-verification-megapod-useast1";

	private final AmazonSQS _sqsClient;
	private final String _auditVerificationQueueName;
	private final FeatureFlagService _featureFlagService;

	public enum Flags {
		PLAT_SUBMIT_AUDIT_VERIFICATION
	}

	@Inject
	public AuditVerificationService(@Named(AuditVerificationModule.AUDIT_VERIFICATION_SQS_CLIENT) AmazonSQS sqsClient, AtlasConfig atlasConfig, FeatureFlagService featureFlagService) {
		_sqsClient = sqsClient;
		_featureFlagService = featureFlagService;

		_auditVerificationQueueName = atlasConfig.getString(AUDIT_VERIFICATION_QUEUE_KEY, AUDIT_VERIFICATION_QUEUE_DEFAULT);
	}

	/**
	 * Submits verificationRequest to SQS queue
	 * @param verificationRequest The request to be submitted
	 */
	public void submitForVerification(final AuditVerificationRequest verificationRequest) {
		// if the feature flag is not enabled do not send the verification request
		if(!_featureFlagService.getBoolean(Flags.PLAT_SUBMIT_AUDIT_VERIFICATION, false)) {
			return;
		}
		final String body = JsonUtil.toJson(verificationRequest);
		// Verification has not been attempted so we are adding this to the message attributes.
		// The verifier will pick the message up. If it has to go back on the queue attempts is
		// incremented. If some attempt threshold is exceeded the message is sent to the dead
		// letter queue.
		final MessageAttributeValue attempts = new MessageAttributeValue();
		attempts.setDataType(ATTEMPT_DATA_TYPE);
		attempts.setStringValue(ATTEMPT_DEFAULT_VALUE);
		final Map<String, MessageAttributeValue> DEFAULT_MESSAGE_ATTRIBUTES = Collections.singletonMap(ATTEMPT_ATTRIBUTE, attempts);
		// Try to submit the message with retry
		boolean success = false;
		Exception error = null;
		int retry = 0;
		while(!success && retry < BACKOFF_DELAY.length) {
			try {
				final String queueUrl = _sqsClient.getQueueUrl(_auditVerificationQueueName).getQueueUrl();
				final SendMessageRequest request = new SendMessageRequest(queueUrl, body);
				request.setMessageAttributes(DEFAULT_MESSAGE_ATTRIBUTES);
				_sqsClient.sendMessage(request);
				success = true;
			} catch (Exception e) {
				error = e;
				ThreadUtils.sleep(BACKOFF_DELAY[retry], TimeUnit.SECONDS);
				retry++;
			}
		}
		if(!success) {
			PrometheusMetricsUtil.counter(AUDIT_VERIFICATION_QUEUE_SUBMISSION_FAILURE_TOTAL, AUDIT_VERIFICATION_QUEUE_SUBMISSION_FAILURE_TOTAL_HELP).inc();
			_log.error("Unable to submit audit verification message for domain event: " + verificationRequest.getId(), error);
		}
	}
}
