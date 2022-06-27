/*
 * Copyright (C) 2022 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.verification;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.audit.verification.AuditVerificationRequest.VerificationTarget;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuditVerificationServiceTest {

	public static final String TENANT_ID = "tenant0001";
	public static final String POD_NAME = "my pod";
	public static final String ORG_NAME = "my org";
	public static final String EVENT_ID = "messageid1234";
	private static final Date CREATED_DATE = new Date();
	@Mock
	FeatureFlagService _featureFlagService;

	@Mock
	AmazonSQS _sqsClient;

	@Mock
	AtlasConfig _atlasConfig;

	@InjectMocks
	AuditVerificationService _auditVerificationService;

	AuditVerificationRequest _verificationRequest;

	@Before
	public void setup() {
		_verificationRequest = AuditVerificationRequest.builder().
			tenantId(TENANT_ID).
			pod(POD_NAME).
			org(ORG_NAME).
			id(EVENT_ID).
			created(CREATED_DATE).
			verifyIn(Arrays.asList(VerificationTarget.S3, VerificationTarget.SEARCH)).
			build();
	}

	@Test
	public void whenFeatureFlagIsDisabledShouldReturnImmediately() {
		when(_featureFlagService.getBoolean(eq(AuditVerificationService.Flags.PLAT_SUBMIT_AUDIT_VERIFICATION), anyBoolean())).thenReturn(false);
		_auditVerificationService.submitForVerification(_verificationRequest);
		verify(_sqsClient, never()).sendMessage(any(SendMessageRequest.class));
	}

	@Test
	public void whenFeatureFlagIsEnabledShouldDoWork() {
		when(_featureFlagService.getBoolean(eq(AuditVerificationService.Flags.PLAT_SUBMIT_AUDIT_VERIFICATION), anyBoolean())).thenReturn(true);
		when(_sqsClient.getQueueUrl(any(String.class))).thenReturn(new GetQueueUrlResult());
		ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
		when(_sqsClient.sendMessage(requestCaptor.capture())).thenReturn(new SendMessageResult());
		_auditVerificationService.submitForVerification(_verificationRequest);
		verify(_sqsClient, times(1)).sendMessage(any(SendMessageRequest.class));
		// Verify the request was correctly composed
		SendMessageRequest request = requestCaptor.getValue();
		Assert.assertEquals(AuditVerificationService.ATTEMPT_DEFAULT_VALUE, request.getMessageAttributes().get(AuditVerificationService.ATTEMPT_ATTRIBUTE).getStringValue());
	}

	@Test
	public void whenSQSFailsShouldRetryThreeTimesAndThenReturn() {
		when(_featureFlagService.getBoolean(eq(AuditVerificationService.Flags.PLAT_SUBMIT_AUDIT_VERIFICATION), anyBoolean())).thenReturn(true);
		when(_sqsClient.getQueueUrl(any(String.class))).thenReturn(new GetQueueUrlResult());
		when(_sqsClient.sendMessage(any(SendMessageRequest.class))).thenThrow(new RuntimeException());
		_auditVerificationService.submitForVerification(_verificationRequest);
		verify(_sqsClient, times(3)).sendMessage(any(SendMessageRequest.class));
	}

}
