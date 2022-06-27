/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.event.interest.matching;

import com.sailpoint.iris.client.Event;
import com.sailpoint.notification.sender.common.event.interest.matching.dto.NotificationInterestMatched;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class NotificationInterestMatchedTest {

	public final static String APPROVAL_REQUESTED = "APPROVAL_REQUESTED";
	private final static String INTEREST_APPROVAL = "INTEREST_APPROVAL";
	private final static String EMAIL = "email";
	private final static String NOTIFICATION_ID = "1234";
	private final static String RECIPIENT_ID = "314cf125-f892-4b16-bcbb-bfe4afb01f85";
	private final static String RECIPIENT_EMAIL = "jon@test.com";
	private final static String NOTIFICATION_KEY = "password_change";

	@Test
	public void notificationInterestMatchingBuilderTest() {

		Event originalEvent = getDomainEvent(APPROVAL_REQUESTED);
		NotificationInterestMatchedBuilder builder = new NotificationInterestMatchedBuilder(NOTIFICATION_ID,
				originalEvent)
				.withRecipientId(RECIPIENT_ID)
				.withRecipientEmail(RECIPIENT_EMAIL)
				.withNotificationKey(NOTIFICATION_KEY)
				.withInterestName(INTEREST_APPROVAL)
				.withCategoryName(EMAIL)
				.withEnabled(false);

		Assert.assertEquals(NOTIFICATION_ID, builder.getNotificationId());
		Assert.assertEquals(APPROVAL_REQUESTED, builder.getDomainEvent().getType());
		Assert.assertEquals(RECIPIENT_ID, builder.getRecipientId());
		Assert.assertEquals(INTEREST_APPROVAL, builder.getInterestName());
		Assert.assertEquals(EMAIL, builder.getCategoryName());
		Assert.assertEquals(RECIPIENT_EMAIL, builder.getRecipientEmail());
		Assert.assertEquals(NOTIFICATION_KEY, builder.getNotificationKey());
		Assert.assertFalse(builder.isEnabled());
	}

	@Test
	public void notificationInterestMatchingTest() {

		Event originalEvent = getDomainEvent(APPROVAL_REQUESTED);
		NotificationInterestMatched notificationEvent = new NotificationInterestMatchedBuilder(NOTIFICATION_ID,
				originalEvent)
				.withRecipientId(RECIPIENT_ID)
				.withRecipientEmail(RECIPIENT_EMAIL)
				.withNotificationKey(NOTIFICATION_KEY)
				.withInterestName(INTEREST_APPROVAL)
				.withCategoryName(EMAIL)
				.build();

		Assert.assertEquals(NOTIFICATION_ID, notificationEvent.getNotificationId());
		Assert.assertEquals(APPROVAL_REQUESTED, notificationEvent.getDomainEvent().getType());
		Assert.assertEquals(RECIPIENT_ID, notificationEvent.getRecipientId());
		Assert.assertEquals(INTEREST_APPROVAL, notificationEvent.getInterestName());
		Assert.assertEquals(EMAIL, notificationEvent.getCategoryName());
		Assert.assertEquals(RECIPIENT_EMAIL, notificationEvent.getRecipientEmail());
		Assert.assertEquals(NOTIFICATION_KEY, notificationEvent.getNotificationKey());
		Assert.assertTrue(notificationEvent.isEnabled());

		NotificationInterestMatchedBuilder builder = notificationEvent.derive();

		Assert.assertEquals(NOTIFICATION_ID, builder.getNotificationId());
		Assert.assertEquals(APPROVAL_REQUESTED, builder.getDomainEvent().getType());
		Assert.assertEquals(RECIPIENT_ID, builder.getRecipientId());
		Assert.assertEquals(INTEREST_APPROVAL, builder.getInterestName());
		Assert.assertEquals(EMAIL, builder.getCategoryName());
		Assert.assertEquals(RECIPIENT_EMAIL, builder.getRecipientEmail());
		Assert.assertEquals(NOTIFICATION_KEY, builder.getNotificationKey());
		Assert.assertTrue(builder.isEnabled());
	}

	@Test(expected = IllegalStateException.class)
	public void userPreferencesInterestMatchingExceptionTest() {

		new NotificationInterestMatchedBuilder(NOTIFICATION_ID,
				getDomainEvent(APPROVAL_REQUESTED))
				.withRecipientId(RECIPIENT_ID)
				.withRecipientEmail(RECIPIENT_EMAIL)
				.withInterestName(INTEREST_APPROVAL)
				.withCategoryName(EMAIL)
				.build();
	}

	@Test
	public void notificationInterestMatchingToStringSecurityTest() {

		// Given NotificationInterestMatched
		Event originalEvent = getDomainEvent(APPROVAL_REQUESTED);
		NotificationInterestMatched notificationEvent = new NotificationInterestMatchedBuilder(NOTIFICATION_ID,
				originalEvent)
				.withRecipientId(RECIPIENT_ID)
				.withRecipientEmail(RECIPIENT_EMAIL)
				.withNotificationKey(NOTIFICATION_KEY)
				.withInterestName(INTEREST_APPROVAL)
				.withCategoryName(EMAIL)
				.build();

		//insure security: no customer specific data in toString
		Assert.assertFalse(notificationEvent.toString().contains("Engineering Administrator"));
	}

	public static Event getDomainEvent(String type) {
		Map<String, String> headers= new HashMap<>();
		headers.put("pod", "timor");
		headers.put("org","sailpoint");
		return new Event(type, headers, "{  \n" +
				"    \"approvers\": [{  \n" +
				"        \"id\": \"314cf125-f892-4b16-bcbb-bfe4afb01f85\",  \n" +
				"        \"name\": \"james.smith\"  \n" +
				"    }, {  \n" +
				"        \"id\": \"70e7cde5-3473-46ea-94ea-90bc8c605a6c\",  \n" +
				"        \"name\": \"jane.doe\"  \n" +
				"    }],  \n" +
				"    \"requester_id\": \"46ec3058-eb0a-41b2-8df8-1c3641e4d771\",  \n" +
				"    \"requester_name\": \"boss.man\",  \n" +
				"    \"accessItems\": [{  \n" +
				"        \"type\": \"ROLE\",  \n" +
				"        \"name\": \"Engineering Administrator\"  \n" +
				"    }]  \n" +
				"}  ");
	}
}
