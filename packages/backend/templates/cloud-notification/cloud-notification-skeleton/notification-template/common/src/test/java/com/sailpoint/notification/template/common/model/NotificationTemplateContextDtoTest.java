/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.model;


import com.google.common.collect.ImmutableMap;
import com.sailpoint.notification.context.common.model.NotificationTemplateContextDto;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.OffsetDateTime;

@RunWith(MockitoJUnitRunner.class)
public class NotificationTemplateContextDtoTest {

	@Before
	public void setUp() {}

	@Test
	public void NotificationTemplateContextDtoTest() {
		NotificationTemplateContextDto notificationTemplateContextDto = new NotificationTemplateContextDto();
		notificationTemplateContextDto.setCreated(OffsetDateTime.now());
		notificationTemplateContextDto.setModified(OffsetDateTime.now());
		notificationTemplateContextDto.setAttributes(ImmutableMap.of("key", "val"));

		Assert.assertNotNull(notificationTemplateContextDto.getAttributes());
		Assert.assertNotNull(notificationTemplateContextDto.getAttributes().get("key"));
		Assert.assertEquals("val", notificationTemplateContextDto.getAttributes().get("key"));
		Assert.assertTrue(notificationTemplateContextDto.getCreated().isBefore(OffsetDateTime.now().plusNanos(100)));
		Assert.assertTrue(notificationTemplateContextDto.getModified().isBefore(OffsetDateTime.now().plusNanos(100)));
	}
}
