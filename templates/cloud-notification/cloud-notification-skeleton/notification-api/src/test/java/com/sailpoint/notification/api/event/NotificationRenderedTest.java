/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.api.event;

import com.sailpoint.notification.api.event.dto.NotificationRendered;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit tests for NotificationRendered
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationRenderedTest {

	private static final String NOTIFICATION_BODY = "James smith requested the role 'Engineering Administrator', " +
			"Please click <a href=...>here</a> to review and approve";

	private static final String NOTIFICATION_SUBJECT = "Approve Access for james.smith";

	private static final String DOMAIN_EVENT = "Domain Event";

	@Test
	public void notificationRenderedTest() {

		NotificationRendered notificationEvent = NotificationRendered.builder()
				.recipient(new RecipientBuilder()
						.withEmail("james.smith@sailpoint.com")
						.build())
				.from("boss.man@ailpoint.com")
				.subject(NOTIFICATION_SUBJECT)
				.body(NOTIFICATION_BODY)
				.replyTo("jon@test")
				.domainEvent("{\"type\", \"jsonContent\"}")
				.medium("html")
				.build();

		Assert.assertEquals("james.smith@sailpoint.com", notificationEvent.getRecipient().getEmail());
		Assert.assertEquals("boss.man@ailpoint.com", notificationEvent.getFrom());
		Assert.assertEquals(NOTIFICATION_SUBJECT, notificationEvent.getSubject());
		Assert.assertEquals(NOTIFICATION_BODY, notificationEvent.getBody());
		Assert.assertEquals("jon@test", notificationEvent.getReplyTo());
		Assert.assertNotNull(notificationEvent.getDomainEvent());
		Assert.assertEquals("{\"type\", \"jsonContent\"}", notificationEvent.getDomainEvent());
		Assert.assertEquals("html", notificationEvent.getMedium());
	}

	@Test
	public void notificationRenderedToStringSecurityTest() {

		//Given NotificationRendered
		NotificationRendered notificationEvent = NotificationRendered.builder()
				.recipient(new RecipientBuilder()
						.withEmail("james.smith@sailpoint.com")
						.build())
				.from("boss.man@ailpoint.com")
				.subject(NOTIFICATION_SUBJECT)
				.body(NOTIFICATION_BODY)
				.replyTo("jon@test")
				.domainEvent("{\"type\", \"jsonContent\"}")
				.medium("html")
				.domainEvent(DOMAIN_EVENT)
				.build();

		//insure security: no customer specific data in toString
		Assert.assertFalse(notificationEvent.toString().contains(NOTIFICATION_SUBJECT));
		Assert.assertFalse(notificationEvent.toString().contains(NOTIFICATION_BODY));
		Assert.assertFalse(notificationEvent.toString().contains(DOMAIN_EVENT));
	}
}
