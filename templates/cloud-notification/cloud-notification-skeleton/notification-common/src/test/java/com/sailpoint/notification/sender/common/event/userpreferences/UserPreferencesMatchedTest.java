/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.sender.common.event.userpreferences;

import com.sailpoint.iris.client.Event;
import com.sailpoint.notification.api.event.dto.Recipient;
import com.sailpoint.notification.sender.common.event.interest.matching.NotificationInterestMatchedTest;
import com.sailpoint.notification.sender.common.event.userpreferences.dto.UserPreferencesMatched;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

public class UserPreferencesMatchedTest {

	@Test
	public void userPreferencesMatchedBuilderTest() {
		Event eventMock = Mockito.mock(Event.class);
		Recipient recipientMock = Mockito.mock(Recipient.class);
		UserPreferencesMatchedBuilder builder = new UserPreferencesMatchedBuilder().withDomainEvent(eventMock)
				.withMedium("medium")
				.withRecipient(recipientMock)
				.withTemplate("template")
				.withNotificationKey("notification_key");

		Assert.assertEquals(eventMock, builder.getDomainEvent());
		Assert.assertEquals(recipientMock, builder.getRecipient());
		Assert.assertEquals("medium", builder.getMedium());
		Assert.assertEquals("template", builder.getTemplate());
		Assert.assertEquals("notification_key", builder.getNotificationKey());
	}

	@Test
	public void userPreferencesMatchedTest() {
		Event eventMock = Mockito.mock(Event.class);
		Recipient recipientMock = Mockito.mock(Recipient.class);

		UserPreferencesMatchedBuilder userPreferencesMatchedBuilder = new UserPreferencesMatchedBuilder().withDomainEvent(eventMock)
				.withMedium("medium")
				.withRecipient(recipientMock)
				.withTemplate("template")
				.withNotificationKey("notification_key")
				.withBrand(Optional.of("acme"));

		UserPreferencesMatched userPreferencesMatched = userPreferencesMatchedBuilder.build();

		Assert.assertEquals(eventMock, userPreferencesMatched.getDomainEvent());
		Assert.assertEquals(recipientMock, userPreferencesMatched.getRecipient());
		Assert.assertEquals("medium", userPreferencesMatched.getMedium());
		Assert.assertEquals("template", userPreferencesMatched.getTemplate());
		Assert.assertEquals("notification_key", userPreferencesMatched.getNotificationKey());
		Assert.assertEquals("acme", userPreferencesMatched.getBrand().get());
		UserPreferencesMatchedBuilder derivedBuilder = userPreferencesMatched.derive();

		Assert.assertEquals(userPreferencesMatchedBuilder.getDomainEvent(), derivedBuilder.getDomainEvent());
		Assert.assertEquals(userPreferencesMatchedBuilder.getRecipient(), derivedBuilder.getRecipient());
		Assert.assertEquals(userPreferencesMatchedBuilder.getMedium(), derivedBuilder.getMedium());
		Assert.assertEquals(userPreferencesMatchedBuilder.getTemplate(), derivedBuilder.getTemplate());
		Assert.assertEquals(userPreferencesMatchedBuilder.getNotificationKey(), derivedBuilder.getNotificationKey());
		Assert.assertEquals(userPreferencesMatchedBuilder.getBrand(), derivedBuilder.getBrand());
	}

	@Test
	public void userPreferencesMatchedToStringSecurityTest() {
		Recipient recipientMock = Mockito.mock(Recipient.class);
		//Given userPreferencesMatched
		UserPreferencesMatched userPreferencesMatched = new UserPreferencesMatchedBuilder()
				.withDomainEvent(NotificationInterestMatchedTest.getDomainEvent(NotificationInterestMatchedTest.APPROVAL_REQUESTED))
				.withMedium("medium")
				.withRecipient(recipientMock)
				.withTemplate("template")
				.withNotificationKey("notification_key")
				.build();

		//insure security: no customer specific data in toString
		Assert.assertFalse(userPreferencesMatched.toString().contains("Engineering Administrator"));

	}
	@Test(expected = IllegalStateException.class)
	public void userPreferencesMatchedBuilderExceptionTest() {

		Event eventMock = Mockito.mock(Event.class);
		Recipient recipientMock = Mockito.mock(Recipient.class);
		new UserPreferencesMatchedBuilder().withDomainEvent(eventMock)
				.withMedium("medium")
				.withRecipient(recipientMock)
				.build();
	}
}
