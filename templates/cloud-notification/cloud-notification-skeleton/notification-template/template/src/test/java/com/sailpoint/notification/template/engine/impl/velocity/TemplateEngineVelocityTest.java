/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.engine.impl.velocity;

import com.sailpoint.notification.api.event.RecipientBuilder;
import com.sailpoint.notification.template.context.TemplateContext;
import com.sailpoint.notification.template.context.impl.velocity.TemplateContextVelocity;
import com.sailpoint.notification.template.util.TemplateUtil;
import com.sailpoint.notification.userpreferences.dto.UserPreferences;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;
import org.apache.velocity.tools.generic.DateTool;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;

/**
 * Unit tests for TemplateEngineVelocity.
 */
@RunWith(MockitoJUnitRunner.class)
public class TemplateEngineVelocityTest {

	@Test
	public void renderTemplateTest() {
		// Given TemplateEngineVelocity
		TemplateEngineVelocity templateEngineVelocity = new TemplateEngineVelocity();

		// And TemplateContext
		TemplateContext templateContext = new TemplateContextVelocity();
		templateContext.put("service", "hermes");

		// And template text
		String templateText = "Unit test for $service.";

		// When
		String result = templateEngineVelocity.evaluate(templateText, templateContext);

		// Then
		assertEquals("Unit test for hermes.", result);
	}

	@Test
	public void innerContextRenderTest() {

		// Given TemplateEngineVelocity
		TemplateEngineVelocity templateEngineVelocity = new TemplateEngineVelocity();

		// And template text
		String templateText = "*$event.get('test.type').toLowerCase().replaceAll('_',' ')* test for $event.get('service.name').";

		// And TemplateContext
		TemplateContext innerContext = new TemplateContextVelocity();
		innerContext.put("service.name", "hermes");
		innerContext.put("test.type", "Unit_Unit");

		TemplateContext templateContext = new TemplateContextVelocity();
		templateContext.put("event", innerContext);

		// When
		String result = templateEngineVelocity.evaluate(templateText, templateContext);

		// Then
		assertEquals("*unit unit* test for hermes.", result);

	}

	@Test
	public void nonVelocityContextTest() {
		// Given TemplateEngineVelocity
		TemplateEngineVelocity templateEngineVelocity = new TemplateEngineVelocity();

		// And template text
		String templateText = "$testType test for $service.";

		// When
		String result = templateEngineVelocity.evaluate(templateText, new TemplateContext<String, Object>() {
			@Override
			public void put(String key, Object val) {
			}

			@Override
			public Object get(String key) {
				return null;
			}

			@Override
			public Set<String> keySet() {
				return null;
			}

			@Override
			public boolean containsKey(String key) {
				return false;
			}
		});

		// Then
		Assert.assertNull(result);
	}

	@Test(expected = NullPointerException.class)
	public void nullContextTest() {
		// Given TemplateEngineVelocity
		TemplateEngineVelocity templateEngineVelocity = new TemplateEngineVelocity();

		// And template text
		String templateText = "$testType test for $service.";

		// When
		templateEngineVelocity.evaluate(templateText, null);
	}

	@Test(expected = IllegalStateException.class)
	public void unableToEvaluateTemplateContextTest() {
		// Given TemplateEngineVelocity
		TemplateEngineVelocity templateEngineVelocity = new TemplateEngineVelocity();

		// And broken template text
		String templateText = "$test.get('test'";

		// And TemplateContext
		TemplateContext innerContext = new TemplateContextVelocity();
		innerContext.put("service.name", "hermes");
		innerContext.put("test.type", "Unit");

		// When
		templateEngineVelocity.evaluate(templateText, innerContext);
	}

	@Test
	public void velocityDateToolTest() {
		// Given TemplateEngineVelocity
		TemplateEngineVelocity templateEngineVelocity = new TemplateEngineVelocity();

		//And TemplateContext
		TemplateContext innerContext = new TemplateContextVelocity();

		ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(MILLIS);
		innerContext.put("isoDateString", zonedDateTime.format(DateTimeFormatter.ISO_DATE_TIME));

		TemplateContext templateContext = new TemplateContextVelocity();
		templateContext.put("event", innerContext);

		// And DateTool in context
		templateContext.put("date", new DateTool());

		String templateText =
				"#set($isoDate = $event.get('isoDateString'))" +
				"#set($dateObj = $date.toDate(\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\", $isoDate))" +
				"$date.format('short', $dateObj)";

		String evaluatedText = templateEngineVelocity.evaluate(templateText, templateContext);

		assertEquals(zonedDateTime.format(DateTimeFormatter.ofPattern("M/d/yy, h:mm a")), evaluatedText);
	}

	@Test
	public void velocityUtilTest() {
		// Given TemplateEngineVelocity
		TemplateEngineVelocity templateEngineVelocity = new TemplateEngineVelocity();

		//And TemplateContext
		TemplateContext templateContext = new TemplateContextVelocity();

		UserPreferencesRepository repository = Mockito.mock(UserPreferencesRepository.class);
		Mockito.doReturn(
				new UserPreferences.UserPreferencesBuilder()
						.withRecipient(
								new RecipientBuilder()
										.withName("Aaron Nichols")
										.withEmail("aaron@sailpoint.com")
										.build())
						.build())
				.when(repository).findByRecipientId("70e7cde5-3473-46ea-94ea-90bc8c605a6c");

		//And util in context
		templateContext.put("util", new TemplateUtil(repository));

		String templateText = "#set($user = \"70e7cde5-3473-46ea-94ea-90bc8c605a6c\")$util.getUser($user).getName() " +
				"$util.getUser($user).getEmail()";

		String evaluatedText = templateEngineVelocity.evaluate(templateText, templateContext);

		assertEquals("Aaron Nichols aaron@sailpoint.com", evaluatedText);
	}

	@Test
	public void velocityUtilNullTest() {
		// Given TemplateEngineVelocity
		TemplateEngineVelocity templateEngineVelocity = new TemplateEngineVelocity();

		//And TemplateContext
		TemplateContext templateContext = new TemplateContextVelocity();

		UserPreferencesRepository repository = Mockito.mock(UserPreferencesRepository.class);
		Mockito.doReturn(null)
				.when(repository).findByRecipientId(any());

		//And util in context
		templateContext.put("util", new TemplateUtil(repository));

		String templateText = "$util.getUser('70e7cde5-3473-46ea-94ea-90bc8c605a6c').getName() " +
				"$util.getUser('70e7cde5-3473-46ea-94ea-90bc8c605a6c').getEmail()";

		String evaluatedText = templateEngineVelocity.evaluate(templateText, templateContext);

		assertEquals("Unknown Unknown", evaluatedText);
	}
}
