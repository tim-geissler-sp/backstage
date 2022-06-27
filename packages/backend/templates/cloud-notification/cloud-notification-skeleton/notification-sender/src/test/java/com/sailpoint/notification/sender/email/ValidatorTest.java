/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.sender.email;

import com.sailpoint.notification.api.event.RecipientBuilder;
import com.sailpoint.notification.api.event.dto.NotificationRendered;
import com.sailpoint.notification.sender.common.exception.InvalidNotificationException;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.matchers.ThrowableCauseMatcher;
import org.junit.internal.matchers.ThrowableMessageMatcher;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for Validator
 */
public class ValidatorTest {

	NotificationRendered base;

	Validator validator;

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		base = NotificationRendered.builder()
				.body("body")
				.domainEvent(new Object())
				.from("from@from")
				.medium("medium@medium")
				.recipient(new RecipientBuilder()
						.withEmail("to@to")
						.build())
				.replyTo("reply@to")
				.subject("subject")
				.build();
		validator = new Validator();
	}

	@Test
	public void valid() throws InvalidNotificationException {
		validator.notificationRenderedValidator(base);
	}

	@Test
	public void nullReplyTo() throws InvalidNotificationException {
		NotificationRendered notificationRendered = base.derive()
				.replyTo(null)
				.build();

		validator.notificationRenderedValidator(notificationRendered);
	}

	@Test
	public void emptyReplyTo() throws InvalidNotificationException {
		NotificationRendered notificationRendered = base.derive()
				.replyTo("")
				.build();

		validator.notificationRenderedValidator(notificationRendered);
	}

	@Test
	public void invalidReplyTo() throws InvalidNotificationException {
		NotificationRendered notificationRendered = base.derive()
				.replyTo("none")
				.build();

		expectSailPointNoRetryException(IllegalArgumentException.class,
				"Reply-To address " + notificationRendered.getReplyTo() + " is invalid");
		validator.notificationRenderedValidator(notificationRendered);
	}

	@Test
	public void nullFrom() throws InvalidNotificationException {
		NotificationRendered notificationRendered = base.derive()
				.from(null)
				.build();

		expectSailPointNoRetryException(NullPointerException.class,
				"From address cannot be null");
		validator.notificationRenderedValidator(notificationRendered);
	}

	@Test
	public void emptyFrom() throws InvalidNotificationException {
		NotificationRendered notificationRendered = base.derive()
				.from("")
				.build();

		expectSailPointNoRetryException(IllegalArgumentException.class,
				"From address  is invalid");
		validator.notificationRenderedValidator(notificationRendered);
	}

	@Test
	public void invalidFrom() throws InvalidNotificationException {
		NotificationRendered notificationRendered = base.derive()
				.from("none")
				.build();

		expectSailPointNoRetryException(IllegalArgumentException.class,
				"From address " + notificationRendered.getFrom() + " is invalid");
		validator.notificationRenderedValidator(notificationRendered);
	}

	@Test
	public void nullTo() throws InvalidNotificationException {
		NotificationRendered notificationRendered = base.derive()
				.recipient(new RecipientBuilder()
				.withEmail(null)
				.build())
				.build();

		expectSailPointNoRetryException(NullPointerException.class,
				"Recipient address cannot be null");
		validator.notificationRenderedValidator(notificationRendered);
	}

	@Test
	public void emptyTo() throws InvalidNotificationException {
		NotificationRendered notificationRendered = base.derive()
				.recipient(new RecipientBuilder()
						.withEmail("")
						.build())
				.build();

		expectSailPointNoRetryException(IllegalArgumentException.class,
				"Recipient address  is invalid");
		validator.notificationRenderedValidator(notificationRendered);
	}

	@Test
	public void invalidTo() throws InvalidNotificationException {
		NotificationRendered notificationRendered = base.derive()
				.recipient(new RecipientBuilder()
						.withEmail("none")
						.build())
				.build();

		expectSailPointNoRetryException(IllegalArgumentException.class,
				"Recipient address " + notificationRendered.getRecipient().getEmail() + " is invalid");
		validator.notificationRenderedValidator(notificationRendered);
	}

	@Test
	public void unicodeInToAddress() throws InvalidNotificationException {
		NotificationRendered notificationRendered = base.derive()
				.recipient(new RecipientBuilder()
						.withEmail("die.vögel@fakebirdhouse.com")
						.build())
				.build();

		expectSailPointNoRetryException(IllegalArgumentException.class,
				"Recipient address " + notificationRendered.getRecipient().getEmail() + " has unicode in it");
		validator.notificationRenderedValidator(notificationRendered);
	}

	@Test
	public void unicodeInFromAddress() throws InvalidNotificationException {
		NotificationRendered notificationRendered = base.derive()
				.from("die.vögel@fakebirdhouse.com")
				.build();

		expectSailPointNoRetryException(IllegalArgumentException.class,
				"From address die.vögel@fakebirdhouse.com has unicode in it");
		validator.notificationRenderedValidator(notificationRendered);
	}

	private void expectSailPointNoRetryException(Class cause, String message) {
		expectExpection(InvalidNotificationException.class, cause, message);
	}

	private void expectExpection(Class exceptionClass, Class cause, String message) {
		expectedEx.expect(exceptionClass);
		expectedEx.expect(new ThrowableCauseMatcher(new IsInstanceOf(cause)));
		expectedEx.expect(new ThrowableMessageMatcher(new StringContains(message)));
	}
}
