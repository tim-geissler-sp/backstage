/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.api.event.dto;

import com.sailpoint.notification.api.event.RecipientBuilder;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test RecipientBuilder and Recipient.
 */
public class RecipientTest {

	@Test
	public void recipientTest() {
		RecipientBuilder recipientBuilder = new RecipientBuilder()
				.withId("1234")
				.withEmail("james.smith@sailpoint.com")
				.withName("james.smith")
				.withPhone("512-888-8888");

		Assert.assertEquals("1234", recipientBuilder.getId());
		Assert.assertEquals("james.smith@sailpoint.com", recipientBuilder.getEmail());
		Assert.assertEquals("james.smith", recipientBuilder.getName());
		Assert.assertEquals("512-888-8888", recipientBuilder.getPhone());

		Recipient recipient = recipientBuilder.build();

		Assert.assertEquals("1234", recipient.getId());
		Assert.assertEquals("james.smith@sailpoint.com", recipient.getEmail());
		Assert.assertEquals("james.smith", recipient.getName());
		Assert.assertEquals("512-888-8888", recipient.getPhone());

		recipientBuilder = recipient.derive();

		Assert.assertEquals("1234", recipientBuilder.getId());
		Assert.assertEquals("james.smith@sailpoint.com", recipientBuilder.getEmail());
		Assert.assertEquals("james.smith", recipientBuilder.getName());
		Assert.assertEquals("512-888-8888", recipientBuilder.getPhone());
	}
}