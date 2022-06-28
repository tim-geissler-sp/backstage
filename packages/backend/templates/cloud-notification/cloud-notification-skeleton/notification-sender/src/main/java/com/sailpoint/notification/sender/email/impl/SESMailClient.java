/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.sender.email.impl;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.AmazonSimpleEmailServiceException;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.DeleteIdentityRequest;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.GetIdentityVerificationAttributesRequest;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.MessageRejectedException;
import com.amazonaws.services.simpleemail.model.MessageTag;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import com.amazonaws.services.simpleemail.model.VerifyEmailIdentityRequest;
import com.google.common.annotations.VisibleForTesting;
import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.atlas.messaging.util.Backoff;
import com.sailpoint.atlas.messaging.util.ExponentialBackoff;
import com.sailpoint.atlas.util.StringUtil;
import com.sailpoint.notification.sender.common.exception.InvalidNotificationException;
import com.sailpoint.notification.sender.email.MailClient;
import com.sailpoint.notification.sender.email.dto.VerificationStatus;
import com.sailpoint.notification.sender.email.service.MailService;
import com.sailpoint.notification.sender.email.service.model.Mail;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AWS SES implementation of the MailClient.
 */
public class SESMailClient implements MailClient {

	public static final String DEFAULT_EMAIL_FROM_ADDRESS = "no-reply@sailpoint.com";
	private static Log _log = LogFactory.getLog(SESMailClient.class);
	private static final int MAX_ATTEMPTS = 3;

	private final AmazonSimpleEmailService _amazonSimpleEmailService;
	private final FeatureFlagService _featureFlagService;
	private final Optional<String> _sourceArn;
	private final BackoffConfig _backoffConfig;


	/**
	 * Feature flag for using Source ARN
	 */
	public static final String HERMES_SOURCE_ARN_ENABLED = "HERMES_SOURCE_ARN_ENABLED";

	public SESMailClient(AmazonSimpleEmailService amazonSimpleEmailService, FeatureFlagService featureFlagService, Optional<String> sourceArn) {
		this(amazonSimpleEmailService, featureFlagService, sourceArn, BackoffConfig.DEFAULT_BACKOFF_MIN, BackoffConfig.DEFAULT_BACKOFF_MAX, BackoffConfig.DEFAULT_BACKOFF_INTERVAL, BackoffConfig.DEFAULT_BACKOFF_FACTOR);
	}

	@VisibleForTesting
	public SESMailClient(AmazonSimpleEmailService amazonSimpleEmailService,
				  FeatureFlagService featureFlagService,
				  Optional<String> sourceArn,
				  int backoffMin,
				  int backoffMax,
				  int backoffInterval,
				  double backoffFactor) {
		_amazonSimpleEmailService = amazonSimpleEmailService;
		_featureFlagService = featureFlagService;
		_sourceArn = sourceArn;
		_backoffConfig = new BackoffConfig(backoffMin, backoffMax, backoffInterval, backoffFactor);
	}

	/**
	 * Sends an email to SES via the SDK. Retries MAX_ATTEMPTS times with exponential backoff if SES throws a
	 * rate exceeded exception.
	 *
	 * @param sendEmailRequest A SendEmailRequest object.
	 */
	public void sendMail(SendEmailRequest sendEmailRequest) throws InvalidNotificationException {
		int attempts = 0;
		final String destinationAddresses = sendEmailRequest.getDestination() != null ?
				String.join(",", sendEmailRequest.getDestination().getToAddresses()) :
				"";

		final String notificationKey = sendEmailRequest.getTags().stream()
				.filter(messageTag -> messageTag.getName().equals(MailService.NOTIFICATION_KEY))
				.findAny()
				.map(messageTag -> messageTag.getValue())
				.orElse("");

		if(destinationAddresses.equals(""))
		{
			throw new InvalidNotificationException(String.format("Destination address is empty --" +
					" Did not attempt to send email of type %s ", notificationKey));
		}

		Backoff backoff = new ExponentialBackoff(
				_backoffConfig.getBackoffMin(),
				_backoffConfig.getBackoffMax(),
				_backoffConfig.getBackoffInterval(),
				_backoffConfig.getBackoffFactor());

		while (attempts++ < MAX_ATTEMPTS) {
			try {
				SendEmailResult sendEmailResult = _amazonSimpleEmailService.sendEmail(sendEmailRequest);
				//sendEmail may throw an unchecked exception.
				//We will let the service/test handle it.
				_log.info(String.format("Email of type %s successfully sent to %s via SES %s",
						notificationKey,
						destinationAddresses,
						sendEmailResult.getMessageId()));
				break;
			} catch (AmazonServiceException e) {
				long backoffInterval = backoff.nextInterval();

				if ("Throttling".equals(e.getErrorCode())
						&& "Maximum sending rate exceeded.".equals(e.getErrorMessage())) {
					_log.error("Throttled. Backing off for " + backoffInterval, e);
					this.sleep(backoffInterval);
				} else {
					// AmazonSimpleEmailServiceException comes from https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/simpleemail/model/AmazonSimpleEmailServiceException.html
					if (e instanceof AmazonSimpleEmailServiceException) {
						if (e.isRetryable()) {
							this.sleep(backoffInterval);
						}
						throw new InvalidNotificationException(e);
					} else {
						throw e;
					}
				}
			}
		}
	}
	private void sleep(long backOff) {
		try {
			Thread.sleep(backOff);
		} catch (InterruptedException ie) {
			//silently ignoring this because we aren't interrupting threads
		}
	}

	/**
	 * Builds a SendEmailRequest from the Mail and sends it to SES.
	 *
	 * @param mail A MailBuilder object.
	 */
	public void sendMail(Mail mail) throws InvalidNotificationException {
		SendEmailRequest request = new SendEmailRequest()
				.withDestination(
						new Destination().withToAddresses(mail.getToAddress()))
				.withMessage(new Message()
						.withBody(new Body()
								.withHtml(new Content()
										.withCharset("UTF-8").withData(mail.getHtml())))
						.withSubject(new Content()
								.withCharset("UTF-8").withData(mail.getSubject())))
				.withSource(mail.getFromAddress())
				.withReplyToAddresses(mail.getNormalizedReplyToAddress());

		if (_sourceArn.isPresent()
				&& _featureFlagService.getBoolean(HERMES_SOURCE_ARN_ENABLED, false)
				&& DEFAULT_EMAIL_FROM_ADDRESS.equals(mail.getFromAddress())) {
			request.setSourceArn(_sourceArn.get());
		}

		if (mail.hasConfigurationSet()) {
			request.setConfigurationSetName(mail.getConfigurationSet());
		}

		if (mail.getTags() != null && !mail.getTags().isEmpty()) {
			request.setTags(mail.getTags().entrySet().stream()
					.filter(tag -> tag.getValue() != null && tag.getValue() != "")
					.map(tag -> new MessageTag().withName(tag.getKey()).withValue(tag.getValue()))
					.collect(Collectors.toList()));
		}

		sendMail(request);
	}

	@Override
	public Map<String, VerificationStatus> getVerificationStatus(List<String> mailAddresses) {
		GetIdentityVerificationAttributesRequest request = new GetIdentityVerificationAttributesRequest()
				.withIdentities(mailAddresses);

		return _amazonSimpleEmailService.getIdentityVerificationAttributes(request).getVerificationAttributes().entrySet()
				.stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> SESMailClient.fromValue(e.getValue().getVerificationStatus())));
	}

	@Override
	public void verifyAddress(String mailAddress) {
		VerifyEmailIdentityRequest request = new VerifyEmailIdentityRequest().withEmailAddress(mailAddress);
		_amazonSimpleEmailService.verifyEmailIdentity(request);
	}

	@Override
	public void deleteAddress(String mailAddress) {
		DeleteIdentityRequest request = new DeleteIdentityRequest().withIdentity(mailAddress);
		_amazonSimpleEmailService.deleteIdentity(request);
	}

	/**
	 * Retruns VerificationStatus enum from SES' VerificationStatus model
	 *
	 * {@link com.amazonaws.services.simpleemail.model.VerificationStatus}'s NotStarted and TemporaryFailure
	 * are remapped to PENDING.
	 *
	 * @param value String value of enum
	 * @return VerificationStatus corresponding to the value
	 */
	private static VerificationStatus fromValue(String value) {
		if (StringUtil.isNullOrEmpty(value)) {
			throw new IllegalArgumentException("Value cannot be null or empty!");
		}

		if (value.equals(com.amazonaws.services.simpleemail.model.VerificationStatus.NotStarted.toString()) ||
				value.equals(com.amazonaws.services.simpleemail.model.VerificationStatus.TemporaryFailure.toString())) {
			return VerificationStatus.PENDING;
		}

		return VerificationStatus.valueOf(value.toUpperCase());
	}
}
