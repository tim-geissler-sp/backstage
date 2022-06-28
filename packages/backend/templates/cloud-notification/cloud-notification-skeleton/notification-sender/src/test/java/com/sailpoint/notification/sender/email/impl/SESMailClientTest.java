/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.sender.email.impl;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.DeleteIdentityResult;
import com.amazonaws.services.simpleemail.model.GetIdentityVerificationAttributesResult;
import com.amazonaws.services.simpleemail.model.IdentityVerificationAttributes;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import com.amazonaws.services.simpleemail.model.VerifyEmailIdentityResult;
import com.google.common.collect.ImmutableMap;
import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.notification.sender.common.exception.InvalidNotificationException;
import com.sailpoint.notification.sender.email.dto.VerificationStatus;
import com.sailpoint.notification.sender.email.service.model.Mail;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.sailpoint.notification.sender.email.impl.SESMailClient.HERMES_SOURCE_ARN_ENABLED;
import static com.sailpoint.notification.sender.email.service.MailService.NOTIFICATION_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SESMailClientTest {

	/**
	 * SESMailClient with actual AWS client used for mailbox simulator tests
	 */
	SESMailClient _sesMailClientWithRealAWS;

	/**
	 * SESMailClient with mock AWS client for mocked tests
	 */
	SESMailClient _sesMailClientWithMockedAWS;

	private Mail _validMail;

	@Mock
	public AmazonSimpleEmailService _amazonSimpleEmailService;

	@Mock
	public FeatureFlagService _featureFlagService;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);

		_validMail = new Mail.MailBuilder()
				.withFromAddress("scrum-echo@sailpoint.com")
				.withToAddress("success@simulator.amazonses.com")
				.withSubject("testValidSendEmailRequest")
				.withHtml("<body>hello<br />html</body>")
				.withReplyToAddress("scrum-surf@sailpoint.com")
				.build();

		_sesMailClientWithRealAWS = new SESMailClient(AmazonSimpleEmailServiceClientBuilder.defaultClient(), _featureFlagService, Optional.empty());
		_sesMailClientWithMockedAWS = new SESMailClient(_amazonSimpleEmailService, _featureFlagService, Optional.of("testArn"), 200, 2000, 200, 2.0);
	}

	@After
	public void validate() {
		validateMockitoUsage();
	}

	@Test
	public void testValidSendEmailRequest() throws InvalidNotificationException {
		_sesMailClientWithRealAWS.sendMail(_validMail);
	}

	@Test
	public void testEmptySubjectAndBody() throws InvalidNotificationException {
		Mail invalidMail = _validMail.derive()
				.withHtml("")
				.withSubject("")
				.withTags(Collections.singletonMap(NOTIFICATION_KEY, "notificationKey"))
				.build();
		_sesMailClientWithRealAWS.sendMail(invalidMail);
	}

	@Test
	public void tesNullTags() throws InvalidNotificationException {
		Mail invalidMail = _validMail.derive()
				.withHtml("")
				.withSubject("")
				.withTags(Collections.singletonMap(NOTIFICATION_KEY, null))
				.build();
		_sesMailClientWithRealAWS.sendMail(invalidMail);
	}
	@Test
	public void tesEmptyStringTags() throws InvalidNotificationException {
		Mail invalidMail = _validMail.derive()
				.withHtml("")
				.withSubject("")
				.withTags(Collections.singletonMap(NOTIFICATION_KEY, ""))
				.build();
		_sesMailClientWithRealAWS.sendMail(invalidMail);
	}

	@Test
	public void testEmptyReplyTo() throws InvalidNotificationException {
		Mail invalidMail = _validMail.derive()
				.withReplyToAddress("")
				.build();
		_sesMailClientWithRealAWS.sendMail(invalidMail);
	}

	@Test(expected = InvalidNotificationException.class)
	public void testInvalidSendEmailRequest() throws InvalidNotificationException {
		Mail invalidMail = new Mail.MailBuilder().build();
		_sesMailClientWithRealAWS.sendMail(invalidMail);
	}

	@Test(expected = InvalidNotificationException.class)
	public void testSendEmailRequestWithUnverifiedEmail() throws InvalidNotificationException {
		Mail mail = _validMail.derive()
				.withFromAddress("unverified@email")
				.build();

		_sesMailClientWithRealAWS.sendMail(mail);
	}

	@Test(expected = SdkClientException.class)
	public void testExceptionSentBack() throws InvalidNotificationException {
		when(_amazonSimpleEmailService.sendEmail(any(SendEmailRequest.class))).thenThrow(SdkClientException.class);
		_sesMailClientWithMockedAWS.sendMail(_validMail);
	}

	@Test
	public void testBackoff() throws InvalidNotificationException {
		AmazonServiceException e = new AmazonServiceException("Maximum sending rate exceeded.");
		e.setErrorCode("Throttling");
		when(_amazonSimpleEmailService.sendEmail(any())).thenThrow(e);

		_sesMailClientWithMockedAWS.sendMail(_validMail);
		verify(_amazonSimpleEmailService, times(3)).sendEmail(any());
	}

	@Test
	public void testHappyPath() throws InvalidNotificationException {
		when(_amazonSimpleEmailService.sendEmail(any())).thenReturn(new SendEmailResult().withMessageId(""));
		_sesMailClientWithMockedAWS.sendMail(_validMail);
		verify(_amazonSimpleEmailService, atMost(1)).sendEmail(any());
	}

	@Test
	public void testGetMailVerificationStatus() {
		when(_amazonSimpleEmailService.getIdentityVerificationAttributes(any()))
				.thenReturn(new GetIdentityVerificationAttributesResult()
						.withVerificationAttributes(ImmutableMap.of(
								"test@mail.com", new IdentityVerificationAttributes()
								.withVerificationStatus(com.amazonaws.services.simpleemail.model.VerificationStatus.Success),
								"test2@mail.com", new IdentityVerificationAttributes()
										.withVerificationStatus(com.amazonaws.services.simpleemail.model.VerificationStatus.NotStarted)))
				);
		Map<String, VerificationStatus> result = _sesMailClientWithMockedAWS.getVerificationStatus(Collections.singletonList("test@mail.com"));
		verify(_amazonSimpleEmailService, atMost(1)).getIdentityVerificationAttributes(any());
		assertEquals(VerificationStatus.SUCCESS, result.get("test@mail.com"));
		assertEquals(VerificationStatus.PENDING, result.get("test2@mail.com"));
	}

	@Test
	public void testInitiateMailVerification() {
		when(_amazonSimpleEmailService.verifyEmailIdentity(any())).thenReturn(new VerifyEmailIdentityResult());
		_sesMailClientWithMockedAWS.verifyAddress("test@mail.com");
		verify(_amazonSimpleEmailService, atMost(1)).verifyEmailIdentity(any());
	}

	@Test
	public void testDeleteVerifiedMail() {
		when(_amazonSimpleEmailService.deleteIdentity(any())).thenReturn(new DeleteIdentityResult());
		_sesMailClientWithMockedAWS.deleteAddress("test@mail.com");
		verify(_amazonSimpleEmailService, atMost(1)).deleteIdentity(any());
	}

	@Test
	public void testSourceARNenabled_noreply() throws InvalidNotificationException {
		when(_featureFlagService.getBoolean(HERMES_SOURCE_ARN_ENABLED, false)).thenReturn(true);
		when(_amazonSimpleEmailService.sendEmail(any())).thenReturn(Mockito.mock(SendEmailResult.class));

		_sesMailClientWithMockedAWS.sendMail(_validMail.derive().withFromAddress("no-reply@sailpoint.com").build());

		ArgumentCaptor<SendEmailRequest> sendEmailRequestArgumentCaptor = ArgumentCaptor.forClass(SendEmailRequest.class);
		verify(_amazonSimpleEmailService, times(1)).sendEmail(sendEmailRequestArgumentCaptor.capture());

		SendEmailRequest request = sendEmailRequestArgumentCaptor.getValue();
		assertEquals("testArn", request.getSourceArn());
	}

	@Test
	public void testSourceARNenabled_differentAddress() throws InvalidNotificationException {
		when(_featureFlagService.getBoolean(HERMES_SOURCE_ARN_ENABLED, false)).thenReturn(true);
		when(_amazonSimpleEmailService.sendEmail(any())).thenReturn(Mockito.mock(SendEmailResult.class));

		_sesMailClientWithMockedAWS.sendMail(_validMail);

		ArgumentCaptor<SendEmailRequest> sendEmailRequestArgumentCaptor = ArgumentCaptor.forClass(SendEmailRequest.class);
		verify(_amazonSimpleEmailService, times(1)).sendEmail(sendEmailRequestArgumentCaptor.capture());

		SendEmailRequest request = sendEmailRequestArgumentCaptor.getValue();
		assertNull(request.getSourceArn());
	}
}
