/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.template.common.manager.impl;

import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.dynamodb.DynamoDBService;
import com.sailpoint.atlas.test.EnvironmentUtil;
import com.sailpoint.atlas.test.integration.dynamodb.DynamoDBServerRule;
import com.sailpoint.atlas.test.integration.dynamodb.EnableInMemoryDynamoDB;
import com.sailpoint.notification.template.common.model.NotificationTemplate;
import com.sailpoint.notification.template.common.model.version.TemplateVersion;
import com.sailpoint.notification.template.common.model.version.TemplateVersionInfo;
import com.sailpoint.notification.template.common.model.version.TemplateVersionUserInfo;
import com.sailpoint.notification.template.common.repository.TemplateRepositoryDefault;
import com.sailpoint.notification.template.common.repository.impl.dynamodb.DynamoDBTemplateRepository;
import com.sailpoint.notification.template.common.repository.impl.dynamodb.entity.TemplatePersistentEntity;
import com.sailpoint.notification.template.common.repository.impl.json.JsonTemplateRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.mockito.Mockito.when;

/**
 * Test template repository manager implementations.
 */
@EnableInMemoryDynamoDB
@RunWith(MockitoJUnitRunner.class)
public class TemplateRepositoryManagerImplTest {

	private TemplateRepositoryManagerImpl _manager;

	@Mock
	AtlasConfig atlasConfig;

	@Rule
	public DynamoDBServerRule _dynamoDBServerRule = new DynamoDBServerRule(EnvironmentUtil.findFreePort());

	@Before
	public void setup() {
		_dynamoDBServerRule.getDynamoDBService().createTable(TemplatePersistentEntity.class,
				DynamoDBService.PROJECTION_ALL);

		when(atlasConfig.getString(JsonTemplateRepository.ATLAS_NOTIFICATION_TEMPLATE_REPOSITORY_LOCATION))
				.thenReturn(null);
	}

	@Test
	public void defaultRepoTest() {

		whenVersionSupport(true);

		//verify default template
		NotificationTemplate template = _manager.getDefault("someTenant");
		Assert.assertNotNull(template);
		Assert.assertEquals("Default template for rendered emails", template.getDescription());

		TemplateRepositoryDefault defaultRepo =  _manager.getDefaultRepository();
		Assert.assertNotNull(defaultRepo);
		Assert.assertNotNull(defaultRepo.findOneByKey("testNotificationKey"));
	}

	@Test
	public void configRepoVersionOnTest() {

		whenVersionSupport(true);

		//first get template from default
		NotificationTemplate template = _manager.findOneByTenantAndKeyAndMediumAndLocale("acme-ocean", "testNotificationKey",
				"phone", Locale.FRENCH);

		Assert.assertNotNull(template);
		Assert.assertEquals("testTemplateDescription4", template.getDescription());

		TemplateVersionUserInfo admin = new TemplateVersionUserInfo("1234", "admin");
		TemplateVersionUserInfo user = new TemplateVersionUserInfo("4321", "user");
		TemplateVersionUserInfo internal = new TemplateVersionUserInfo("2341", "internal");

		//save as is
		TemplateVersionInfo version = new TemplateVersionInfo(user, OffsetDateTime.now(ZoneOffset.UTC), "Save Template");
		_manager.save(template, version);

		//update and save new version
		template = NotificationTemplate.newBuilder()
				.tenant(template.getTenant())
				.key(template.getKey())
				.medium(template.getMedium())
				.locale(template.getLocale())
				.name("Updated Template Name")
				.description("Updated Description")
				.header("Updated Header")
				.body("Updated body")
				.subject("Updated subject")
				.build();

		version = new TemplateVersionInfo(admin, OffsetDateTime.now(ZoneOffset.UTC), "Update Template");

		_manager.save(template, version);

		List<NotificationTemplate> templates = _manager.findAllByTenant("acme-ocean");

		Assert.assertEquals(1, templates.size());
		Assert.assertEquals("Updated Description", templates.get(0).getDescription());

		List<TemplateVersion> templateVersions = _manager.getAllVersions("acme-ocean", "testNotificationKey",
				"phone", Locale.FRENCH);

		templateVersions.sort(Comparator.comparing(TemplateVersion::getVersionId));

		Assert.assertEquals(2, templateVersions.size());
		Assert.assertEquals("Updated Description", templateVersions.get(0).
				getNotificationTemplate().getDescription());
		Assert.assertEquals("testTemplateDescription4", templateVersions.get(1).
				getNotificationTemplate().getDescription());

		//restore initial template
		_manager.restoreVersionByIdAndTenant(templateVersions.get(1).getNotificationTemplate().getId(),
				"acme-ocean", internal);

		//check it was restored
		templateVersions = _manager.getAllVersions("acme-ocean", "testNotificationKey",
				"phone", Locale.FRENCH);

		templateVersions.sort(Comparator.comparing(TemplateVersion::getVersionId));
		Assert.assertEquals(3, templateVersions.size());
		Assert.assertEquals("testTemplateDescription4", templateVersions.get(0).
				getNotificationTemplate().getDescription());
		Assert.assertEquals("internal", templateVersions.get(0)
				.getTemplateVersionInfo().getUpdatedBy().getName());

		//check different queries
		List<TemplateVersion> templatesAll = _manager.getAllLatestByTenant("acme-ocean");
		Assert.assertEquals(1, templatesAll.size());

		templatesAll = _manager.getAllLatestByTenantAndKey("acme-ocean", "testNotificationKey");
		Assert.assertEquals(1, templatesAll.size());

		templatesAll = _manager.getAllLatestByTenantAndMediumAndLocale("acme-ocean", "phone", Locale.FRENCH);
		Assert.assertEquals(1, templatesAll.size());

		//delete version
		boolean ret = _manager.deleteOneByIdAndTenant("acme-ocean", templateVersions.get(1).getNotificationTemplate().getId());
		Assert.assertTrue(ret);

		//check it was deleted
		templateVersions = _manager.getAllVersions("acme-ocean", "testNotificationKey",
				"phone", Locale.FRENCH);
		Assert.assertEquals(2, templateVersions.size());

		//delete template and all versions
		_manager.deleteAllByTenantAndKey("acme-ocean", "testNotificationKey");

		//check it was deleted
		templateVersions = _manager.getAllVersions("acme-ocean", "testNotificationKey",
				"phone", Locale.FRENCH);
		Assert.assertEquals(0, templateVersions.size());

		//save one more time
		_manager.save(template, version);
		templates = _manager.findAllByTenant("acme-ocean");

		Assert.assertEquals(1, templates.size());

		//verify bulk delete condition
		List<Map<String, String>> batchList = new ArrayList<>();
		Map<String, String> batch = new HashMap<>();
		batch.put("medium", "SMS");
		batchList.add(batch);
		ret =_manager.bulkDeleteAllByTenantAndKeyAndMediumAndLocale(template.getTenant(), batchList);
		Assert.assertFalse(ret);

		//delete
		_manager.deleteAllByTenantAndKeyAndMediumAndLocale(template.getTenant(), template.getKey(),
				template.getMedium(), template.getLocale());

		//verify
		templates = _manager.findAllByTenant("acme-ocean");

		Assert.assertEquals(0, templates.size());
	}

	@Test
	public void configRepoVersionOffTest() {

		whenVersionSupport(false);

		//first get template from default
		NotificationTemplate template = _manager.findOneByTenantAndKeyAndMediumAndLocale("acme-ocean", "testNotificationKey",
				"phone", Locale.FRENCH);

		Assert.assertNotNull(template);
		Assert.assertEquals("testTemplateDescription4", template.getDescription());

		TemplateVersionUserInfo admin = new TemplateVersionUserInfo("1234", "admin");
		TemplateVersionUserInfo user = new TemplateVersionUserInfo("4321", "user");

		//save as is
		TemplateVersionInfo version = new TemplateVersionInfo(user, OffsetDateTime.now(ZoneOffset.UTC), "Save Template");
		_manager.save(template, version);

		//update and update
		template = NotificationTemplate.newBuilder()
				.tenant(template.getTenant())
				.key(template.getKey())
				.medium(template.getMedium())
				.locale(template.getLocale())
				.name("Updated Template Name")
				.description("Updated Description")
				.header("Updated Header")
				.body("Updated body")
				.subject("Updated subject")
				.build();

		version = new TemplateVersionInfo(admin, OffsetDateTime.now(ZoneOffset.UTC), "Update Template");

		_manager.save(template, version);

		List<NotificationTemplate> templates = _manager.findAllByTenant("acme-ocean");

		Assert.assertEquals(1, templates.size());
		Assert.assertEquals("Updated Description", templates.get(0).getDescription());

		List<TemplateVersion> templateVersions = _manager.getAllVersions("acme-ocean", "testNotificationKey",
				"phone", Locale.FRENCH);

		templateVersions.sort(Comparator.comparing(TemplateVersion::getVersionId));

		Assert.assertEquals(1, templateVersions.size());
		Assert.assertEquals("Updated Description", templateVersions.get(0).
				getNotificationTemplate().getDescription());

		//delete template and all versions
		_manager.deleteAllByTenantAndKey(templateVersions.get(0).getNotificationTemplate()
				.getTenant(), templateVersions.get(0).getNotificationTemplate().getKey());

		//verify
		templates = _manager.findAllByTenant("acme-ocean");

		Assert.assertEquals(0, templates.size());

	}

	private void whenVersionSupport(boolean value) {
		when(atlasConfig.getBoolean(TemplateVersion.HERMES_CONFIG_ENABLE_VERSION_SUPPORT, false))
				.thenReturn(value);
		_manager = new TemplateRepositoryManagerImpl(new DynamoDBTemplateRepository(_dynamoDBServerRule.getDynamoDBMapper(), atlasConfig),
				new JsonTemplateRepository(atlasConfig));
	}
}
