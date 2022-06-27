/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.context.impl.velocity;

import com.google.common.collect.ImmutableSet;
import org.apache.velocity.VelocityContext;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit tests for TemplateContextVelocity.
 */
@RunWith(MockitoJUnitRunner.class)
public class TemplateContextVelocityTest {

	@Test
	public void basicPropertiesTest() {

		// Given Velocity Template
		TemplateContextVelocity templateContextVelocity = new TemplateContextVelocity();

		// Then no entries
		Assert.assertEquals(templateContextVelocity.keySet().size(), 0);

		// Given first entry
		templateContextVelocity.put("key", "value");

		// Then validate basic properties
		Assert.assertEquals(1, templateContextVelocity.keySet().size());
		Assert.assertEquals(ImmutableSet.of("key"), templateContextVelocity.keySet());
		Assert.assertEquals("value", templateContextVelocity.get("key"));
		Assert.assertTrue(templateContextVelocity.containsKey("key"));
		Assert.assertFalse(templateContextVelocity.containsKey("randomKey"));
	}

	@Test
	public void innerContextTest() {
		// Given Velocity Template
		TemplateContextVelocity innerContext = new TemplateContextVelocity();

		// And first entry
		innerContext.put("key", "value");

		// Then retrieve VelocityContext
		VelocityContext velocityContext = (VelocityContext) innerContext.getContext();
		Assert.assertNotNull(velocityContext);

		TemplateContextVelocity templateContextVelocity = new TemplateContextVelocity(innerContext);
		Assert.assertEquals(0, templateContextVelocity.keySet().size());
	}
}
