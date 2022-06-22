/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.template.common.repository.impl.dynamodb;

import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.dynamodb.DynamoDBService;
import com.sailpoint.atlas.test.EnvironmentUtil;
import com.sailpoint.atlas.test.integration.dynamodb.DynamoDBServerRule;
import com.sailpoint.atlas.test.integration.dynamodb.EnableInMemoryDynamoDB;
import com.sailpoint.notification.template.common.model.NotificationTemplate;
import com.sailpoint.notification.template.common.model.version.TemplateVersion;
import com.sailpoint.notification.template.common.model.version.TemplateVersionInfo;
import com.sailpoint.notification.template.common.model.version.TemplateVersionUserInfo;
import com.sailpoint.notification.template.common.repository.impl.dynamodb.entity.TemplatePersistentEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.mockito.Mockito.when;


/**
 * Test DynamoDBTemplateRepository
 */
@EnableInMemoryDynamoDB
@RunWith(MockitoJUnitRunner.class)
public class DynamoDBTemplateRepositoryTest {

	private static final Log _log = LogFactory.getLog(DynamoDBTemplateRepository.class);

	private static final String TENANT_1 = "acme-solar";
	private static final String TENANT_2 = "acme-ocean";

	private static final String KEY_1 = "key_1";
	private static final String KEY_2 = "key_2";

	private static final String MEDIUM_EMAIL = "email";
	private static final String MEDIUM_PHONE = "mobile";

	private DynamoDBTemplateRepository _repository;

	@Mock
	AtlasConfig atlasConfig;

	@Rule
	public DynamoDBServerRule _dynamoDBServerRule = new DynamoDBServerRule(EnvironmentUtil.findFreePort());

	@Before
	public void setup() {
		_dynamoDBServerRule.getDynamoDBService().createTable(TemplatePersistentEntity.class,
				DynamoDBService.PROJECTION_ALL);
		when(atlasConfig.getBoolean(TemplateVersion.HERMES_CONFIG_ENABLE_VERSION_SUPPORT, false))
				.thenReturn(true);

		_repository = new DynamoDBTemplateRepository(_dynamoDBServerRule.getDynamoDBMapper(), atlasConfig);
	}

	@Test
	public void testSaveDeleteTemplate() {
		//given notification templates.
		List<NotificationTemplate> notificationTemplates = getTemplates();

		//save
		_repository.save(notificationTemplates.get(0), new TemplateVersionInfo());

		//verify saved
		List<NotificationTemplate> result = _repository.findAllByTenant(TENANT_1);

		Assert.assertEquals(1, result.size());
		Assert.assertNotNull(result.get(0).getId());
		Assert.assertEquals(notificationTemplates.get(0).getKey(), result.get(0).getKey());
		Assert.assertEquals(notificationTemplates.get(0).getName(), result.get(0).getName());
		Assert.assertEquals(notificationTemplates.get(0).getTenant(), result.get(0).getTenant());
		Assert.assertEquals(notificationTemplates.get(0).getMedium(), result.get(0).getMedium());
		Assert.assertEquals(notificationTemplates.get(0).getDescription(), result.get(0).getDescription());
		Assert.assertEquals(notificationTemplates.get(0).getSubject(), result.get(0).getSubject());
		Assert.assertEquals(notificationTemplates.get(0).getBody(), result.get(0).getBody());
		Assert.assertEquals(notificationTemplates.get(0).getHeader(), result.get(0).getHeader());
		Assert.assertEquals(notificationTemplates.get(0).getFooter(), result.get(0).getFooter());
		Assert.assertEquals(notificationTemplates.get(0).getReplyTo(), result.get(0).getReplyTo());
		Assert.assertEquals(notificationTemplates.get(0).getFrom(), result.get(0).getFrom());
		Assert.assertEquals(notificationTemplates.get(0).getLocale(), result.get(0).getLocale());

		//save/verify more templates
		_repository.save(notificationTemplates.get(1), new TemplateVersionInfo());

		result =  _repository.findAllByTenant(TENANT_1);
		Assert.assertEquals(2, result.size());

		_repository.save(notificationTemplates.get(1), new TemplateVersionInfo());
		_repository.save(notificationTemplates.get(2), new TemplateVersionInfo());
		_repository.save(notificationTemplates.get(3), new TemplateVersionInfo());

		//delete templates, verify deletions
		boolean ret = _repository.deleteAllByTenantAndKey(TENANT_1, KEY_1);
		Assert.assertTrue(ret);

		ret = _repository.deleteAllByTenantAndKeyAndMediumAndLocale(TENANT_1, KEY_1, "someValue", Locale.FRENCH);
		Assert.assertFalse(ret);

		result =  _repository.findAllByTenant(TENANT_1);
		Assert.assertEquals(1, result.size());

		TemplateVersion version =  _repository.getOneByIdAndTenant(TENANT_1, result.get(0).getId());
		Assert.assertNotNull(version);
		Assert.assertEquals(result.get(0).getId(), version.getNotificationTemplate().getId());

		version =  _repository.getOneByIdAndTenant(TENANT_1, "1234");
		Assert.assertNull(version);

		result =  _repository.findAllByTenant(TENANT_2);
		Assert.assertEquals(1, result.size());

		ret = _repository.deleteAllByTenantAndKey(TENANT_2, KEY_1);
		Assert.assertTrue(ret);

		result =  _repository.findAllByTenant(TENANT_2);
		Assert.assertEquals(0, result.size());

		ret = _repository.deleteAllByTenantAndKey(TENANT_1, KEY_2);
		Assert.assertTrue(ret);

		result =  _repository.findAllByTenant(TENANT_1);
		Assert.assertEquals(0, result.size());

		ret = _repository.deleteAllByTenantAndKey("empty", "key");
		Assert.assertFalse(ret);

		ret = _repository.deleteOneByIdAndTenant(TENANT_1, "1234");
		Assert.assertFalse(ret);
	}

	@Test
	public void testVersions() {
		List<NotificationTemplate> notificationTemplates = getTemplates();

		//add couple of templates with different key, for other tenant.
		_repository.save(notificationTemplates.get(2), new TemplateVersionInfo());
		_repository.save(notificationTemplates.get(3), new TemplateVersionInfo());

		TemplateVersionUserInfo admin = new TemplateVersionUserInfo("1234", "admin");
		TemplateVersionUserInfo user = new TemplateVersionUserInfo("4321", "user");
		TemplateVersionUserInfo internal = new TemplateVersionUserInfo("2341", "internal");

		//create few versions fot the same template
		TemplateVersionInfo versionInfo = new TemplateVersionInfo();
		OffsetDateTime now1 = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(MILLIS);
		versionInfo.setUpdatedBy(admin);
		versionInfo.setNote("create new template");
		versionInfo.setDate(now1);
		_repository.save(notificationTemplates.get(1), versionInfo);

		OffsetDateTime now2 = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(MILLIS);
		versionInfo.setUpdatedBy(user);
		versionInfo.setNote("add new version for template");
		versionInfo.setDate(now2);
		_repository.save(notificationTemplates.get(1), versionInfo);

		OffsetDateTime now3 = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(MILLIS);
		versionInfo.setUpdatedBy(internal);
		versionInfo.setNote("add one more version for template");
		versionInfo.setDate(now3);
		_repository.save(notificationTemplates.get(1), versionInfo);

		List<TemplateVersion> result = _repository.getAllLatestByTenant(TENANT_1);
		Assert.assertEquals(2, result.size());

		result = _repository.getAllLatestByTenantAndKey(TENANT_1, KEY_1);
		Assert.assertEquals(1, result.size());

		List<TemplateVersion> versions = _repository.getLatestByTenantAndKeyAndMediumAndLocale(TENANT_1, KEY_1, MEDIUM_PHONE, Locale.ENGLISH);
		Assert.assertEquals(1, versions.size());
		TemplateVersion version = versions.get(0);
		Assert.assertEquals("V0", version.getVersionId());
		Assert.assertEquals(notificationTemplates.get(1).getKey(), version.getNotificationTemplate().getKey());
		Assert.assertEquals(now3, version.getTemplateVersionInfo().getDate());
		Assert.assertEquals("internal", version.getTemplateVersionInfo().getUpdatedBy().getName());
		Assert.assertEquals("add one more version for template", version.getTemplateVersionInfo().getNote());

		result = _repository.getAllLatestByTenantAndMediumAndLocale(TENANT_1, MEDIUM_PHONE, Locale.ENGLISH);
		Assert.assertEquals(2, result.size());

		result = _repository.getAllVersions(TENANT_1, KEY_1, MEDIUM_PHONE, Locale.ENGLISH);
		result.sort(Comparator.comparing(TemplateVersion::getVersionId));

		Assert.assertEquals(3, result.size());

		Assert.assertEquals("V0", result.get(0).getVersionId());
		Assert.assertEquals(notificationTemplates.get(1).getKey(), result.get(0).getNotificationTemplate().getKey());
		Assert.assertEquals(now3, result.get(0).getTemplateVersionInfo().getDate());
		Assert.assertEquals("internal", result.get(0).getTemplateVersionInfo().getUpdatedBy().getName());
		Assert.assertEquals("add one more version for template", result.get(0).getTemplateVersionInfo().getNote());

		Assert.assertEquals("V1", result.get(1).getVersionId());
		Assert.assertEquals(notificationTemplates.get(1).getKey(), result.get(1).getNotificationTemplate().getKey());
		Assert.assertEquals(now1, result.get(1).getTemplateVersionInfo().getDate());
		Assert.assertEquals("admin", result.get(1).getTemplateVersionInfo().getUpdatedBy().getName());
		Assert.assertEquals("create new template", result.get(1).getTemplateVersionInfo().getNote());

		Assert.assertEquals("V2", result.get(2).getVersionId());
		Assert.assertEquals(notificationTemplates.get(1).getKey(), result.get(2).getNotificationTemplate().getKey());
		Assert.assertEquals(now2, result.get(2).getTemplateVersionInfo().getDate());
		Assert.assertEquals("user", result.get(2).getTemplateVersionInfo().getUpdatedBy().getName());
		Assert.assertEquals("add new version for template", result.get(2).getTemplateVersionInfo().getNote());

		//clean up.
		_repository.deleteAllByTenantAndKey(TENANT_1, KEY_1);
		_repository.deleteAllByTenantAndKey(TENANT_1, KEY_2);
		_repository.deleteAllByTenantAndKey(TENANT_2, KEY_1);

	}

	@Test
	public void testRestoreVersions() {
		List<NotificationTemplate> notificationTemplates = getTemplates();

		//add couple of templates with different key, for other tenant.
		_repository.save(notificationTemplates.get(2), new TemplateVersionInfo());
		_repository.save(notificationTemplates.get(3), new TemplateVersionInfo());

		TemplateVersionUserInfo admin = new TemplateVersionUserInfo("1234", "admin");
		TemplateVersionUserInfo user = new TemplateVersionUserInfo("4321", "user");

		//create few versions fot the same template
		TemplateVersionInfo versionInfo = new TemplateVersionInfo();
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		versionInfo.setUpdatedBy(admin);
		versionInfo.setNote("update template");
		versionInfo.setDate(now);

		_repository.save(notificationTemplates.get(1), versionInfo);
		_repository.save(notificationTemplates.get(1), versionInfo);
		_repository.save(notificationTemplates.get(1), versionInfo);

		List<TemplateVersion> result = _repository.getAllVersions(TENANT_1, KEY_1, MEDIUM_PHONE, Locale.ENGLISH);
		result.sort(Comparator.comparing(TemplateVersion::getVersionId));

		Assert.assertEquals(3, result.size());

		Assert.assertEquals("V0", result.get(0).getVersionId());
		Assert.assertEquals("V1", result.get(1).getVersionId());
		Assert.assertEquals("V2", result.get(2).getVersionId());

		_repository.restoreVersionByIdAndTenant(result.get(1).getNotificationTemplate().getId(), TENANT_1, user);

		result = _repository.getAllVersions(TENANT_1, KEY_1, MEDIUM_PHONE, Locale.ENGLISH);
		result.sort(Comparator.comparing(TemplateVersion::getVersionId));

		Assert.assertEquals(4, result.size());

		Assert.assertEquals("V0", result.get(0).getVersionId());
		Assert.assertEquals("user", result.get(0).getTemplateVersionInfo().getUpdatedBy().getName());
		Assert.assertEquals("Restored from version V1", result.get(0).getTemplateVersionInfo().getNote());

		//clean up.
		_repository.deleteAllByTenantAndKey(TENANT_1, KEY_1);
		_repository.deleteAllByTenantAndKey(TENANT_1, KEY_2);
		_repository.deleteAllByTenantAndKey(TENANT_2, KEY_1);
	}

	@Test
	public void testDynamoDBErrors() {
		List<NotificationTemplate> notificationTemplates = getTemplates();

		//add couple of templates with different key, for other tenant.
		_repository.save(notificationTemplates.get(2), new TemplateVersionInfo());
		_repository.save(notificationTemplates.get(3), new TemplateVersionInfo());

		_repository.save(notificationTemplates.get(1), new TemplateVersionInfo());

		TemplateVersionUserInfo user = new TemplateVersionUserInfo("4321", "user");

		List<NotificationTemplate> templates =  _repository.findAllByTenant(TENANT_1);
		try {
			_repository.deleteOneByIdAndTenant(TENANT_1, templates.get(0).getId());
		} catch (IllegalArgumentException e) {
			Assert.assertEquals(("You can't delete latest template version") , e.getMessage());
		}

		try {
			_repository.restoreVersionByIdAndTenant("12345", TENANT_1, user);
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("Failed to restore template for tenant " + TENANT_1 +
			". Template with id: 12345 not found", e.getMessage());
		}

		List<TemplateVersion> result = _repository.getAllVersions(TENANT_1, KEY_1, MEDIUM_PHONE, Locale.ENGLISH);
		Assert.assertEquals(1, result.size());

		try {
			_repository.restoreVersionByIdAndTenant(result.get(0).getNotificationTemplate().getId(), TENANT_1, user);
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("You can't restore latest template", e.getMessage());
		}

		try {
			_repository.findAllByTenant("");
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("Tenant cannot be null or empty.", e.getMessage());
		}

		try {
			_repository.deleteAllByTenantAndKey("", null);
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("Tenant or Key cannot be null or empty.", e.getMessage());
		}

		//verify empty list
		List<TemplateVersion> versions = _repository.getAllVersions("someTenant", "someKey",
				MEDIUM_PHONE, Locale.ENGLISH);
		Assert.assertEquals(0, versions.size());

		versions = _repository.getAllVersions(TENANT_1, "someKey",
				MEDIUM_PHONE, Locale.ENGLISH);
		Assert.assertEquals(0, versions.size());

		versions = _repository.getAllVersions("someTenant", KEY_1,
				MEDIUM_PHONE, Locale.ENGLISH);
		Assert.assertEquals(0, versions.size());

		//clean up.
		_repository.deleteAllByTenantAndKey(TENANT_1, KEY_1);
		_repository.deleteAllByTenantAndKey(TENANT_1, KEY_2);
		_repository.deleteAllByTenantAndKey(TENANT_2, KEY_1);
	}

	@Test
	public void testFindOneByTenantAndKeyAndMediumAndLocale() {
		List<NotificationTemplate> notificationTemplates = getTemplates();

		//add couple of templates with different key, for other tenant.
		_repository.save(notificationTemplates.get(2), new TemplateVersionInfo());
		_repository.save(notificationTemplates.get(3), new TemplateVersionInfo());

		//save test template
		_repository.save(notificationTemplates.get(0), new TemplateVersionInfo());

		//find test template.
		NotificationTemplate result = _repository.findOneByTenantAndKeyAndMediumAndLocale(TENANT_1, KEY_1,
				MEDIUM_EMAIL, Locale.ENGLISH);

		Assert.assertNotNull(result.getId());
		Assert.assertEquals(notificationTemplates.get(0).getKey(), result.getKey());
		Assert.assertEquals(notificationTemplates.get(0).getName(), result.getName());
		Assert.assertEquals(notificationTemplates.get(0).getTenant(), result.getTenant());
		Assert.assertEquals(notificationTemplates.get(0).getMedium(), result.getMedium());
		Assert.assertEquals(notificationTemplates.get(0).getDescription(), result.getDescription());
		Assert.assertEquals(notificationTemplates.get(0).getSubject(), result.getSubject());
		Assert.assertEquals(notificationTemplates.get(0).getBody(), result.getBody());
		Assert.assertEquals(notificationTemplates.get(0).getHeader(), result.getHeader());
		Assert.assertEquals(notificationTemplates.get(0).getFooter(), result.getFooter());
		Assert.assertEquals(notificationTemplates.get(0).getReplyTo(), result.getReplyTo());
		Assert.assertEquals(notificationTemplates.get(0).getFrom(), result.getFrom());
		Assert.assertEquals(notificationTemplates.get(0).getLocale(), result.getLocale());

		//not found template.
		result = _repository.findOneByTenantAndKeyAndMediumAndLocale("someTenant", "someKey",
				"someMedium", Locale.ENGLISH);

		Assert.assertNull(result);

		//clean up.
		_repository.deleteAllByTenantAndKey(TENANT_1, KEY_1);
		_repository.deleteAllByTenantAndKey(TENANT_1, KEY_2);
		_repository.deleteAllByTenantAndKey(TENANT_2, KEY_1);
	}

	@Test
	public void testBulkDelete() {

		getTemplates().forEach(t-> _repository.save(t, new TemplateVersionInfo()));

		List<TemplateVersion> result = _repository.getAllLatestByTenant(TENANT_1);
		Assert.assertEquals(3, result.size());

		List<Map<String, String>> bathWithError = new ArrayList<>();
		Map<String, String> correctMap = new HashMap<>();
		correctMap.put("key", KEY_1);
		correctMap.put("medium", MEDIUM_EMAIL);
		correctMap.put("locale", Locale.ENGLISH.toLanguageTag());
		bathWithError.add(correctMap);
		Map<String, String> incorrectMap = new HashMap<>();
		incorrectMap.put("key", KEY_1);
		incorrectMap.put("medium", "SMS");
		incorrectMap.put("locale", Locale.ENGLISH.toLanguageTag());
		bathWithError.add(incorrectMap);

		boolean deleteResult = _repository.bulkDeleteAllByTenantAndKeyAndMediumAndLocale(TENANT_1, bathWithError);
		Assert.assertFalse(deleteResult);

		result = _repository.getAllLatestByTenant(TENANT_1);
		Assert.assertEquals(3, result.size());

		List<Map<String, String>> bathNoErrors = new ArrayList<>();
		bathNoErrors.add(correctMap);
		Map<String, String> oneMoreCorrectMap = new HashMap<>();
		oneMoreCorrectMap.put("key", KEY_1);
		oneMoreCorrectMap.put("medium", MEDIUM_PHONE);
		oneMoreCorrectMap.put("locale", Locale.ENGLISH.toLanguageTag());
		bathNoErrors.add(oneMoreCorrectMap);

		deleteResult = _repository.bulkDeleteAllByTenantAndKeyAndMediumAndLocale(TENANT_1, bathNoErrors);
		Assert.assertTrue(deleteResult);

		result = _repository.getAllLatestByTenant(TENANT_1);
		Assert.assertEquals(1, result.size());

		//clean up.
		_repository.deleteAllByTenantAndKey(TENANT_1, KEY_1);
		_repository.deleteAllByTenantAndKey(TENANT_1, KEY_2);
		_repository.deleteAllByTenantAndKey(TENANT_2, KEY_1);
	}

	private List<NotificationTemplate> getTemplates() {
		List<NotificationTemplate> templates = new ArrayList<>();
		templates.add(NotificationTemplate.newBuilder()
				.name("name")
				.key(KEY_1)
				.tenant(TENANT_1)
				.medium(MEDIUM_EMAIL)
				.description("description")
				.subject("subject")
				.body("body")
				.header("header")
				.footer("footer")
				.replyTo("replyTo")
				.from("from")
				.locale(Locale.ENGLISH)
				.build());

		templates.add(NotificationTemplate.newBuilder()
				.name("name")
				.key(KEY_1)
				.tenant(TENANT_1)
				.medium(MEDIUM_PHONE)
				.description("description")
				.subject("subject")
				.body("body")
				.header("header")
				.footer("footer")
				.replyTo("replyTo")
				.from("from")
				.locale(Locale.ENGLISH)
				.build());

		templates.add(NotificationTemplate.newBuilder()
				.name("name")
				.key(KEY_2)
				.tenant(TENANT_1)
				.medium(MEDIUM_PHONE)
				.description("description")
				.subject("subject")
				.body("body")
				.header("header")
				.footer("footer")
				.replyTo("replyTo")
				.from("from")
				.locale(Locale.ENGLISH)
				.build());

		templates.add(NotificationTemplate.newBuilder()
				.name("name")
				.key(KEY_1)
				.tenant(TENANT_2)
				.medium(MEDIUM_PHONE)
				.description("description")
				.subject("subject")
				.body("body")
				.header("header")
				.footer("footer")
				.replyTo("replyTo")
				.from("from")
				.locale(Locale.ENGLISH)
				.build());

		return templates;
	}
}
