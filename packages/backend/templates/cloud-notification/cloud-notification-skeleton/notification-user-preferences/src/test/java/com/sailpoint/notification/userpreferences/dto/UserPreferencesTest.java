/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.dto;

import com.sailpoint.notification.api.event.RecipientBuilder;
import com.sailpoint.notification.api.event.dto.Recipient;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

/**
 * Test UserPreferences class.
 */
public class UserPreferencesTest {

	Recipient _recipient;

	@Test
	public void userPreferencesTest() {
		givenRecipient();
		UserPreferences userPreferences = new UserPreferences.UserPreferencesBuilder()
				.withRecipient(_recipient)
				.withBrand(null)
				.build();

		thenVerifyRecipient(userPreferences);
		Assert.assertNotNull(userPreferences.getBrand());
		Assert.assertFalse(userPreferences.getBrand().isPresent());
		Assert.assertFalse(userPreferences.equals("test"));
	}

	@Test
	public void userPreferencesWithBrandTest() {
		givenRecipient();
		UserPreferences userPreferences = new UserPreferences.UserPreferencesBuilder()
				.withRecipient(_recipient)
				.withBrand(Optional.of("Ranger"))
				.build();

		thenVerifyRecipient(userPreferences);
		Assert.assertEquals("Ranger", userPreferences.getBrand().get());
		Assert.assertFalse(userPreferences.equals("test"));
	}

	private void givenRecipient() {
		_recipient = new RecipientBuilder()
				.withId("314cf125-f892-4b16-bcbb-bfe4afb01f85")
				.withEmail("james.smith@sailpoint.com")
				.withName("james.smith")
				.withPhone("512-888-8888")
		.build();
	}

	private void thenVerifyRecipient(UserPreferences userPreferences) {
		Assert.assertEquals("314cf125-f892-4b16-bcbb-bfe4afb01f85", userPreferences.getRecipient().getId());
		Assert.assertEquals("james.smith@sailpoint.com", userPreferences.getRecipient().getEmail());
		Assert.assertEquals("james.smith", userPreferences.getRecipient().getName());
		Assert.assertEquals("512-888-8888", userPreferences.getRecipient().getPhone());
	}
}