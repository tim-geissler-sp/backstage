/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.repository.impl.json;

import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.utilities.JsonUtil;
import com.sailpoint.notification.template.common.model.NotificationTemplate;
import com.sailpoint.notification.template.common.repository.TemplateRepositoryDefault;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.mockito.Mockito.when;

/**
 * General tests to validate the JSON implementation of TemplateRepository.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonTemplateRepositoryTest {

	private static int NUMBER_TEMPLATES = 9;
	@Mock
	AtlasConfig atlasConfig;

	@Test
	public void testJsonRepositoryFromResource() {

		// Given no ATLAS_NOTIFICATION_TEMPLATE_REPOSITORY_LOCATION
		when(atlasConfig.getString(JsonTemplateRepository.ATLAS_NOTIFICATION_TEMPLATE_REPOSITORY_LOCATION))
				.thenReturn(null);

		// And JsonTemplateRepository
		TemplateRepositoryDefault templateRepository = new JsonTemplateRepository(atlasConfig);

		// Then
		Assert.assertEquals(templateRepository.findAll().size(), NUMBER_TEMPLATES);

		Assert.assertEquals(templateRepository.findAllByTenant("acme-solar").size(), 2);
		Assert.assertEquals(templateRepository.findAllByTenant("invalid-tenant").size(), 0);
		Assert.assertEquals(templateRepository.findAllByTenant("acme-lunar").size(), 1);
		Assert.assertEquals(templateRepository.findAllByMedium("email").size(), 4);

		Assert.assertNotNull(templateRepository.findOneByName("testTemplateName2"));
		Assert.assertNull(templateRepository.findOneByName("invalidName"));

		Assert.assertNotNull(templateRepository.findOneByTenantAndKey("acme-solar", "testTemplateNameKey"));
		Assert.assertNull(templateRepository.findOneByTenantAndKey("acme-solar", "invalidKey"));

		Assert.assertNotNull(templateRepository.findOneByTenantAndKey("invalid", "testNoTenantKey"));
		Assert.assertNull(templateRepository.findOneByTenantAndKey("invalid", "invalidKey"));

		Assert.assertNotNull(templateRepository.findOneByNameAndMedium("testTemplateName2", "email"));
		Assert.assertNotNull(templateRepository.findOneByNameAndLocale("testTemplateName2", Locale.ENGLISH));
		Assert.assertNull(templateRepository.findOneByNameAndLocale("testTemplateName2", Locale.CHINESE));

		Assert.assertNotNull(templateRepository.findOneByTenantAndMedium("acme-lunar", "sms"));
		Assert.assertNotNull(templateRepository.findOneByTenantAndLocale("acme-lunar", Locale.ITALIAN));
		Assert.assertNull(templateRepository.findOneByTenantAndMedium("invalid-tenant", "sms"));

		Assert.assertNotNull(templateRepository.findOneByNameAndMediumAndLocale("testTemplateName2", "email", Locale.ENGLISH));
		Assert.assertNotNull(templateRepository.findOneByTenantAndMediumAndLocale("acme-lunar", "sms", Locale.ITALIAN));

		Assert.assertNotNull(templateRepository.findOneByKey("testNotificationKey"));
		Assert.assertNull(templateRepository.findOneByKey("invalidKey"));

		Assert.assertNotNull(templateRepository.findOneByKeyAndMedium("testNotificationKey", "phone"));
		Assert.assertNotNull(templateRepository.findOneByKeyAndLocale("testNotificationKey", Locale.FRENCH));
		Assert.assertNull(templateRepository.findOneByKeyAndLocale("testNotificationKey", Locale.CHINESE));
		Assert.assertNotNull(templateRepository.findOneByKeyAndMediumAndLocale("testNotificationKey", "phone", Locale.FRENCH));

		Assert.assertNotNull(templateRepository.getDefault("tenant"));
		Assert.assertNotNull(templateRepository.findOneByTenantAndNameAndMedium("acme-solar",
				"testTemplateName2", "email"));
		Assert.assertNotNull(templateRepository.findOneByTenantAndKeyAndMedium("acme-ocean",
				"testNotificationKey", "phone"));
		Assert.assertNotNull(templateRepository.findOneByTenantAndKeyAndMediumAndLocale("acme-ocean",
				"testNotificationKey", "phone", Locale.FRENCH));
		Assert.assertNotNull(templateRepository.findOneByTenantAndKeyAndMediumAndLocale("acme-solar",
				"testNotificationKey", "phone", Locale.FRENCH));

		// Common Role Mining Completed Email Tests
		NotificationTemplate commonRoleMiningCompleted = templateRepository.findOneByKey("testCommonRoleMiningCompletedEmailKey");
		Assert.assertNotNull(commonRoleMiningCompleted);
		Assert.assertEquals(commonRoleMiningCompleted.getMedium(), "EMAIL");
		Assert.assertNotNull(commonRoleMiningCompleted.getHeader());
		Assert.assertNotNull(commonRoleMiningCompleted.getBody());
		Assert.assertNotNull(commonRoleMiningCompleted.getFooter());
		Assert.assertNotNull(commonRoleMiningCompleted.getLocale());

		//slack template
		Assert.assertNotNull(templateRepository.findOneByKey("slack_template"));
		Assert.assertNotNull(templateRepository.findOneByKeyAndMedium("slack_template", "slack")
				.getSlackTemplate());
		Assert.assertNotNull(templateRepository.findOneByKeyAndMedium("slack_template", "slack")
				.getSlackTemplate().getBlocks());
		Assert.assertNotNull(templateRepository.findOneByKeyAndMedium("slack_template", "slack")
				.getSlackTemplate().getText());
		Assert.assertNotNull(templateRepository.findOneByKeyAndMedium("slack_template", "slack")
				.getSlackTemplate().getAttachments());

		//teams template
		Assert.assertNotNull(templateRepository.findOneByKey("teams_template"));
		Assert.assertNotNull(templateRepository.findOneByKeyAndMedium("teams_template", "teams")
				.getTeamsTemplate());
		Assert.assertNotNull(templateRepository.findOneByKeyAndMedium("teams_template", "teams")
				.getTeamsTemplate().getTitle());
		Assert.assertNotNull(templateRepository.findOneByKeyAndMedium("teams_template", "teams")
				.getTeamsTemplate().getText());
		Assert.assertNotNull(templateRepository.findOneByKeyAndMedium("teams_template", "teams")
				.getTeamsTemplate().getMessageJson());

	}

	@Test
	public void testExternalJsonRepository() {
		// Given external JSON from ATLAS_NOTIFICATION_TEMPLATE_REPOSITORY_LOCATION
		when(atlasConfig.getString(JsonTemplateRepository.ATLAS_NOTIFICATION_TEMPLATE_REPOSITORY_LOCATION))
				.thenReturn(createTempJsonFile());

		// And JsonTemplateRepository
		TemplateRepositoryDefault templateRepository = new JsonTemplateRepository(atlasConfig);

		// Then
		Assert.assertEquals(templateRepository.findAll().size(), 2);
		Assert.assertEquals(templateRepository.findAllByTenant("acme-foo").size(), 1);
		Assert.assertEquals(templateRepository.findAllByTenant("invalid-tenant").size(), 0);
		Assert.assertEquals(templateRepository.findAllByTenant("acme-bar").size(), 1);
		Assert.assertNotNull(templateRepository.findOneByName("template-foo"));
	}


	private static String createTempJsonFile() {
		File file = null;
		Path path;

		try {
			path = Files.createTempFile("templates_" + UUID.randomUUID().toString(), ".json");
			file = path.toFile();
			Files.write(path, JsonUtil.toJsonPretty(createTempTemplates()).getBytes(StandardCharsets.UTF_8));
			file.deleteOnExit();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return file != null ? file.getAbsolutePath() : null;
	}

	private static List<NotificationTemplate> createTempTemplates() {
		final List<NotificationTemplate> notificationTemplates = new ArrayList<>(2);

		final NotificationTemplate templateAcmeFoo = NotificationTemplate.newBuilder()
				.body("body")
				.name("template-foo")
				.subject("subject")
				.tenant("acme-foo")
				.locale(Locale.ITALIAN)
				.build();
		final NotificationTemplate templateAcmeBar = NotificationTemplate.newBuilder()
				.body("body")
				.name("template-bar")
				.subject("subject")
				.tenant("acme-bar")
				.locale(Locale.ENGLISH)
				.build();

		notificationTemplates.add(templateAcmeFoo);
		notificationTemplates.add(templateAcmeBar);

		return notificationTemplates;
	}
}
