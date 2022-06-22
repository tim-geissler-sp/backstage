/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.api.event;

import com.sailpoint.notification.api.event.dto.ExtendedNotificationEvent;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit tests for ExtendedNotificationEvent
 */
@RunWith(MockitoJUnitRunner.class)
public class ExtendedNotificationEventTest {

	private static final String NOTIFICATION_BODY = "James smith requested the role 'Engineering Administrator', " +
			"Please click <a href=...>here</a> to review and approve";

	private static final String NOTIFICATION_SUBJECT = "Approve Access for james.smith";

	@Test
	public void notificationEventBuilderTest() {

		ExtendedNotificationEventBuilder builder = new ExtendedNotificationEventBuilder()
				.withRecipient(new RecipientBuilder()
						.withEmail("james.smith@sailpoint.com")
						.withId("1234")
						.build())
				.withFrom("boss.man@ailpoint.com")
				.withOrg("acme")
				.withOrgId(3L)
				.withRequestId("3456")
				.withNotificationKey("password_change")
				.withTemplateEvaluated(Boolean.TRUE)
				.withSubject(NOTIFICATION_SUBJECT)
				.withBody(NOTIFICATION_BODY)
				.withReplyTo("jon@test")
				.withMedium("html");

		Assert.assertEquals("james.smith@sailpoint.com", builder.getRecipient().getEmail());
		Assert.assertEquals("1234", builder.getRecipient().getId());
		Assert.assertEquals("boss.man@ailpoint.com", builder.getFrom());
		Assert.assertEquals(NOTIFICATION_SUBJECT, builder.getSubject());
		Assert.assertEquals(NOTIFICATION_BODY, builder.getBody());
		Assert.assertEquals("jon@test", builder.getReplyTo());
		Assert.assertEquals("html", builder.getMedium());
		Assert.assertEquals("acme", builder.getOrg());
		Assert.assertEquals(new Long(3), builder.getOrgId());
		Assert.assertEquals("3456", builder.getRequestId());
		Assert.assertEquals("password_change", builder.getNotificationKey());
		Assert.assertEquals(Boolean.TRUE, builder.isTemplateEvaluated());
	}

	@Test
	public void notificationEventTest() {

		ExtendedNotificationEvent extendedNotificationEvent = new ExtendedNotificationEventBuilder()
				.withRecipient(new RecipientBuilder()
						.withEmail("james.smith@sailpoint.com")
						.withId("1234")
						.build())
				.withFrom("boss.man@ailpoint.com")
				.withOrg("acme")
				.withOrgId(3L)
				.withRequestId("3456")
				.withNotificationKey("password_change")
				.withTemplateEvaluated(Boolean.FALSE)
				.withSubject(NOTIFICATION_SUBJECT)
				.withBody(NOTIFICATION_BODY)
				.withReplyTo("jon@test")
				.withMedium("html")
				.build();

		Assert.assertEquals("james.smith@sailpoint.com", extendedNotificationEvent.getRecipient().getEmail());
		Assert.assertEquals("1234", extendedNotificationEvent.getRecipient().getId());
		Assert.assertEquals("boss.man@ailpoint.com", extendedNotificationEvent.getFrom());
		Assert.assertEquals(NOTIFICATION_SUBJECT, extendedNotificationEvent.getSubject());
		Assert.assertEquals(NOTIFICATION_BODY, extendedNotificationEvent.getBody());
		Assert.assertEquals("jon@test", extendedNotificationEvent.getReplyTo());
		Assert.assertEquals("html", extendedNotificationEvent.getMedium());
		Assert.assertEquals("acme", extendedNotificationEvent.getOrg());
		Assert.assertEquals(new Long(3), extendedNotificationEvent.getOrgId());
		Assert.assertEquals("3456", extendedNotificationEvent.getRequestId());
		Assert.assertEquals("password_change", extendedNotificationEvent.getNotificationKey());
		Assert.assertEquals(Boolean.FALSE, extendedNotificationEvent.isTemplateEvaluated());
	}

	@Test
	public void notificationEventToStringSecurityTest() {

		//Given ExtendedNotificationEvent
		ExtendedNotificationEvent extendedNotificationEvent = new ExtendedNotificationEventBuilder()
				.withRecipient(new RecipientBuilder()
						.withEmail("james.smith@sailpoint.com")
						.withId("1234")
						.build())
				.withFrom("boss.man@ailpoint.com")
				.withOrg("acme")
				.withOrgId(3L)
				.withRequestId("3456")
				.withNotificationKey("password_change")
				.withTemplateEvaluated(Boolean.FALSE)
				.withSubject(NOTIFICATION_SUBJECT)
				.withBody(NOTIFICATION_BODY)
				.withReplyTo("jon@test")
				.withMedium("html")
				.build();

		//insure security: no customer specific data in toString
		Assert.assertFalse(extendedNotificationEvent.toString().contains(NOTIFICATION_SUBJECT));
		Assert.assertFalse(extendedNotificationEvent.toString().contains(NOTIFICATION_BODY));
	}
}
