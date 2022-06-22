/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.email.service;

import com.google.inject.Inject;
import com.sailpoint.metrics.MetricsUtil;
import com.sailpoint.notification.api.event.dto.NotificationRendered;
import com.sailpoint.notification.sender.common.exception.InvalidNotificationException;
import com.sailpoint.notification.sender.common.lifecycle.NotificationMetricsUtil;
import com.sailpoint.notification.sender.email.MailClient;
import com.sailpoint.notification.sender.email.Validator;
import com.sailpoint.notification.sender.email.service.model.Mail;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.CheckForNull;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Singleton
public class MailService {

	private static final Log _log = LogFactory.getLog(MailService.class);

	@Inject
	MailClient _mailClient;

	@Inject
	Validator _validator;

	@Inject
	NotificationMetricsUtil _metricsUtil;

	public static final String MAIL_METRIC_PREFIX = MailService.class.getName();

	public static final String NOTIFICATION_KEY = "notificationKey";

	/**
	 * Enum of possible mail metric results
	 */
	public enum MailResultMetricName {
		SUCCESS,
		FAILURE
	}

	/**
	 * Sends email using the mail client implementation that is bound to the interface.
	 * @param notificationRendered notification event
	 */
	public void sendMail(NotificationRendered notificationRendered) throws InvalidNotificationException {

		try {
			_validator.notificationRenderedValidator(notificationRendered);
			String originalSubject = extractOriginalSubject(notificationRendered);
			if (originalSubject == null || originalSubject.isEmpty() ||
					originalSubject.equalsIgnoreCase("no_send") ||
					originalSubject.equalsIgnoreCase("stop")) {
				_log.info(String.format("Sending email to %s was disabled, the subject is '%s' ",
						notificationRendered.getRecipient(), originalSubject));
				return;
			}

			final Optional<String> notificationKey = Optional.ofNullable(notificationRendered.getNotificationKey());
			Mail.MailBuilder mailBuilder = new Mail.MailBuilder()
					.withToAddress(notificationRendered.getRecipient().getEmail())
					.withFromAddress(notificationRendered.getFrom())
					.withSubject(notificationRendered.getSubject())
					.withHtml(notificationRendered.getBody())
					.withReplyToAddress(notificationRendered.getReplyTo());

			if (notificationKey.isPresent()) {
				mailBuilder = mailBuilder.withTags(Collections.singletonMap(NOTIFICATION_KEY, notificationKey.get()));
			}

			_mailClient.sendMail(mailBuilder.build());

			MetricsUtil.getCounter(MAIL_METRIC_PREFIX + ".sendMail." + MailResultMetricName.SUCCESS,
					_metricsUtil.getTags(Optional.empty())).inc();
		} catch (Exception ex) {
			handleFailure(ex, notificationRendered);
		}
	}

	@CheckForNull
	private String extractOriginalSubject(NotificationRendered notificationRendered) {
		if (notificationRendered == null || notificationRendered.getSubject() == null) {
			return null;
		}
		return notificationRendered.getSubject().replaceAll("\\[Original recipient: .*] ", "");
	}

	/**
	 * Log failure, handle metrics and throw exception to consumer.
	 *
	 * @param ex - original error.
	 * @param notificationRendered notification event.
	 */
	private void handleFailure(Exception ex, NotificationRendered notificationRendered) throws InvalidNotificationException {

		Throwable rootCause = ExceptionUtils.getRootCause(ex);
		if(rootCause != null) {
			final Map<String, String> tags = _metricsUtil.getTags(Optional.of(rootCause.getClass().getSimpleName()));
				MetricsUtil.getCounter(MAIL_METRIC_PREFIX + ".sendMail." + MailResultMetricName.FAILURE, tags).inc();
		}

		if(ex instanceof InvalidNotificationException) {
			throw (InvalidNotificationException)ex;
		} else {
			_log.error(ex);
			throw new RuntimeException(ex);
		}
	}
}
