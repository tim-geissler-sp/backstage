/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.manager.integration;

import com.sailpoint.atlas.AtlasApplication;
import com.sailpoint.atlas.OrgData;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.dynamodb.DynamoDBServiceModule;
import com.sailpoint.atlas.health.AtlasHealthPlugin;
import com.sailpoint.atlas.plugin.AtlasPlugin;
import com.sailpoint.atlas.plugin.PluginConfigurationContext;
import com.sailpoint.atlas.security.SecurityContext;
import com.sailpoint.atlas.test.integration.IdnAtlasIntegrationTest;
import com.sailpoint.atlas.test.integration.IdnAtlasIntegrationTestApplication;
import com.sailpoint.atlas.test.integration.dynamodb.EnableInMemoryDynamoDB;
import com.sailpoint.featureflag.impl.MockFeatureFlagClient;
import com.sailpoint.notification.api.event.dto.SlackNotificationAutoApprovalData;
import com.sailpoint.utilities.JsonUtil;
import com.sailpoint.mantisclient.HttpResponseException;
import com.sailpoint.mantisclient.Params;
import com.sailpoint.notification.template.common.model.TemplateMediumDto;
import com.sailpoint.notification.template.common.model.version.TemplateVersion;
import com.sailpoint.notification.template.manager.NotificationTemplateManagerPlugin;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateBulkDeleteDto;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateDto;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateDtoDefault;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateDtoVersion;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateVersionUserInfoDto;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.sailpoint.notification.template.manager.rest.resouce.TemplateConfigResource.HERMES_TEMPLATES_SLACK_PERSISTENT_ENABLED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Integration test that loads Atlas and Notification Template Manager module.
 */
@EnableInMemoryDynamoDB
public class NotificationTemplateManagerIntegrationTest extends IdnAtlasIntegrationTest {

	private static final String DEFAULT_URI = "/v3/notification-template-defaults";
	private static final String CONFIG_URI = "/v3/notification-templates";
	private static final String CONFIG_DELETE_URI = "/v3/notification-templates/bulk-delete";
	private static final String VERSION_URI_FORMAT = "/v3/notification-template-versions/%s/%s/%s";
	private static final String VERSION_URI = "/v3/notification-template-versions";

	private static final String FILTER_KEY = "key eq \"notificationKeyTest\"";
	private static final String FILTER_KEY_IN = "key in (\"notificationKeyTest\",\"notificationKey\")";
	private static final String FILTER_KEY_MEDIUM_LOCALE = "key eq \"notificationKeyTest\" " +
			"and medium eq \"email\" and locale eq \"en\"";
	private static final String FILTER_KEY_MEDIUM_SLACK_LOCALE = "medium eq \"slack\" and locale eq \"en\"";

	private static final String FILTER_MEDIUM_LOCALE = "medium eq \"email\" and locale eq \"en\"";
	private static final String FILTER_SLACK_MEDIUM_LOCALE_JA = "medium eq \"slack\" and locale eq \"ja\"";

	private static final String FILTER_VERSION_V1 = "versionId eq \"V1\"";

	private static final String FILTER_VERSIONS_ALL = "versionId eq \"all\"";

	private static final String FILTER_NAME_CO = "name co \"e2e-template\"";
	private static final String FILTER_NAME_SW = "name sw \"Default\"";
	private static final String SORTER_NAME = "name";
	private static final String SORTER_MEDIUM = "medium";

	public static final String LOCALE_EQ_FR = "locale eq \"FR\"";
	public static final String MEDIUM_EQ_SMS = "medium eq \"SMS\"";

	@Override
	protected AtlasApplication createApplication() {
		System.setProperty(TemplateVersion.HERMES_CONFIG_ENABLE_VERSION_SUPPORT, Boolean.TRUE.toString());
		return new IdnAtlasIntegrationTestApplication() {{
			registerPlugin(new AtlasHealthPlugin());
			registerPlugin(new NotificationTemplateManagerPlugin());
			registerPlugin(new AtlasPlugin() {
				@Override
				public void configure(PluginConfigurationContext context) {
					context.addGuiceModule(new DynamoDBServiceModule());
				}
			});
		}};
	}

	@Override
	public void initializeApplication() throws Exception {
		super.initializeApplication();
	}

	@Before
	public void before() {
		System.setProperty(TemplateVersion.HERMES_CONFIG_ENABLE_VERSION_SUPPORT, Boolean.TRUE.toString());
		RequestContext.set(setDummyRequestContext());
	}

	@After
	public void cleanup() {
		RequestContext.set(null);
		System.clearProperty(TemplateVersion.HERMES_CONFIG_ENABLE_VERSION_SUPPORT);
	}

	@Test
	public void templateDefaultTest() {

		//test get defaults no params
		List<TemplateDtoDefault> response = JsonUtil.parseList(TemplateDtoDefault.class, _restClient
				.get(DEFAULT_URI));
		Assert.assertNotNull(response);
		Assert.assertEquals(4, response.size());

		//test get defaults limits params
		Params params = new Params();
		params.query("offset", 1);
		params.query("limit", 2);

		response = JsonUtil.parseList(TemplateDtoDefault.class, _restClient
				.get(DEFAULT_URI, params));
		Assert.assertNotNull(response);
		Assert.assertEquals(2, response.size());
		Assert.assertEquals("notificationKey", response.get(1).getKey());

		//test get defaults with filters
		params = new Params();
		params.query("filters", FILTER_KEY_MEDIUM_LOCALE);
		params.query("count", "true");
		response = JsonUtil.parseList(TemplateDtoDefault.class, _restClient
				.get(DEFAULT_URI, params));
		Assert.assertNotNull(response);
		Assert.assertEquals(1, response.size());
		Assert.assertEquals("notificationKeyTest", response.get(0).getKey());

		params = new Params();
		params.query("filters", FILTER_KEY);
		params.query("count", "true");
		response = JsonUtil.parseList(TemplateDtoDefault.class, _restClient
				.get(DEFAULT_URI, params));
		Assert.assertNotNull(response);
		Assert.assertEquals(1, response.size());
		Assert.assertEquals("notificationKeyTest", response.get(0).getKey());

		params = new Params();
		params.query("filters", FILTER_KEY_IN);
		response = JsonUtil.parseList(TemplateDtoDefault.class, _restClient
				.get(DEFAULT_URI, params));
		Assert.assertNotNull(response);
		Assert.assertEquals(2, response.size());

		params = new Params();
		params.query("filters", FILTER_NAME_CO);
		response = JsonUtil.parseList(TemplateDtoDefault.class, _restClient
				.get(DEFAULT_URI, params));
		Assert.assertNotNull(response);
		Assert.assertEquals(2, response.size());

		params = new Params();
		params.query("filters", FILTER_NAME_SW);
		response = JsonUtil.parseList(TemplateDtoDefault.class, _restClient
				.get(DEFAULT_URI, params));
		Assert.assertNotNull(response);
		Assert.assertEquals(1, response.size());

		params = new Params();
		params.query("sorters", SORTER_NAME);
		response = JsonUtil.parseList(TemplateDtoDefault.class, _restClient
				.get(DEFAULT_URI, params));
		Assert.assertNotNull(response);
		Assert.assertEquals("Access Request", response.get(0).getName());

		params = new Params();
		params.query("sorters", SORTER_MEDIUM);
		response = JsonUtil.parseList(TemplateDtoDefault.class, _restClient
				.get(DEFAULT_URI, params));
		Assert.assertNotNull(response);
		Assert.assertEquals("EMAIL", response.get(0).getMedium().toString());

		//test get defaults with sorting
		params = new Params();
		params.query("sorters", "-key");
		response = JsonUtil.parseList(TemplateDtoDefault.class, _restClient
				.get(DEFAULT_URI, params));
		Assert.assertNotNull(response);
		Assert.assertEquals("default_template", response.get(2).getKey());

		params = new Params();
		params.query("sorters", "key");
		response = JsonUtil.parseList(TemplateDtoDefault.class, _restClient
				.get(DEFAULT_URI, params));
		Assert.assertNotNull(response);
		Assert.assertEquals("access_request", response.get(0).getKey());

		//test get defaults with sorting by medium, locale, key
		params.query("sorters", "-key,locale,-medium");
		response = JsonUtil.parseList(TemplateDtoDefault.class, _restClient
				.get(DEFAULT_URI, params));
		Assert.assertNotNull(response);
		Assert.assertEquals("default_template", response.get(2).getKey());


		//test get defaults with unsupported filter field
		params = new Params();
		params.query("filters", "id eq true");

		try {
			_restClient.get(DEFAULT_URI, params);
			fail("Invalid filter property should result in 400 status.");
		} catch(HttpResponseException e) {
			assertEquals(400, e.getStatusCode());
		}

		//test get defaults with unsupported filter operation
		params = new Params();
		params.query("filters", "key ne true");

		try {
			_restClient.get(DEFAULT_URI, params);
			fail("Invalid filter property should result in 400 status.");
		} catch(HttpResponseException e) {
			assertEquals(400, e.getStatusCode());
		}
	}

	@Test
	public void templateConfigResourceSaveDeleteTest() {

		//get template from defaults with specific key
		Params params = new Params();
		params.query("filters", FILTER_KEY_MEDIUM_LOCALE);
		List<TemplateDtoDefault> responseDto = JsonUtil.parseList(TemplateDtoDefault.class, _restClient
				.get(DEFAULT_URI, params));
		Assert.assertNotNull(responseDto);

		//update template
		TemplateDtoDefault customerTemplate = responseDto.get(0);
		customerTemplate.setBody("test save");

		//create customer specific template
		TemplateDto result = _restClient.postJson(TemplateDto.class, CONFIG_URI, customerTemplate);
		Assert.assertNotNull(result);
		Assert.assertNotNull(result.getId());
		Assert.assertNotNull(result.getModified());
		Assert.assertNotNull(result.getCreated());

		//verify created
		List<TemplateDto> responseTemplates = JsonUtil.parseList(TemplateDto.class, _restClient
				.get(CONFIG_URI));

		Assert.assertNotNull(responseTemplates);
		Assert.assertEquals(1, responseTemplates.size());
		Assert.assertEquals("test save", responseTemplates.get(0).getBody());

		//update customer specific template
		TemplateDto customerTemplateDto = result;
		customerTemplateDto.setBody("test update template");


		result = _restClient.postJson(TemplateDto.class, CONFIG_URI, customerTemplateDto);
		Assert.assertNotNull(result);
		Assert.assertNotNull(result.getId());

		//verify updated
		responseTemplates = JsonUtil.parseList(TemplateDto.class, _restClient
				.get(CONFIG_URI, params));

		Assert.assertNotNull(responseTemplates);
		Assert.assertEquals(1, responseTemplates.size());
		Assert.assertEquals("test update template", responseTemplates.get(0).getBody());

		//verify versions
		params = new Params();
		params.query("filters", FILTER_VERSIONS_ALL);
		String versionURI = String.format(VERSION_URI_FORMAT,
				customerTemplateDto.getKey(), customerTemplateDto.getMedium(),
				customerTemplateDto.getLocale().toLanguageTag());
		List<TemplateDtoVersion> responseVersions = JsonUtil.parseList(TemplateDtoVersion.class, _restClient
				.get(versionURI, params));

		Assert.assertNotNull(responseVersions);
		Assert.assertEquals(2, responseVersions.size());

		//create for new locale
		//update template
		customerTemplateDto = result;
		customerTemplateDto.setBody("test create template for new locale");
		customerTemplateDto.setLocale(Locale.FRENCH);

		//save
		_restClient.post(CONFIG_URI, customerTemplateDto);

		//verify created
		params = new Params();
		params.query("filters", FILTER_KEY);
		responseTemplates = JsonUtil.parseList(TemplateDto.class, _restClient
				.get(CONFIG_URI, params));

		Assert.assertNotNull(responseTemplates);
		Assert.assertEquals(2, responseTemplates.size());

		//verify delete by key, medium, locale
		List<TemplateBulkDeleteDto> bulkDeleteDots = new ArrayList<>();
		TemplateBulkDeleteDto bulkDeleteDot = new TemplateBulkDeleteDto();
		bulkDeleteDot.setKey(customerTemplateDto.getKey());
		bulkDeleteDot.setMedium(customerTemplateDto.getMedium());
		bulkDeleteDot.setLocale(customerTemplateDto.getLocale().toLanguageTag());
		bulkDeleteDots.add(bulkDeleteDot);
		_restClient.post(CONFIG_DELETE_URI, bulkDeleteDots);

		//verify deleted
		responseTemplates = JsonUtil.parseList(TemplateDto.class, _restClient
				.get(CONFIG_URI, params));

		Assert.assertNotNull(responseTemplates);
		Assert.assertEquals(1, responseTemplates.size());

		//verify delete by key
		bulkDeleteDots = new ArrayList<>();
		bulkDeleteDots.add(new TemplateBulkDeleteDto(customerTemplateDto.getKey()));
		_restClient.post(CONFIG_DELETE_URI, bulkDeleteDots);

		//verify deleted
		responseTemplates = JsonUtil.parseList(TemplateDto.class, _restClient
				.get(CONFIG_URI));

		Assert.assertNotNull(responseTemplates);
		Assert.assertEquals(0, responseTemplates.size());

		//clean up
		cleanUpDatabase();
	}

	@Test
	public void templateConfigResourceSaveGetTest() {
		///get template from defaults with specific key
		Params params = new Params();
		params.query("filters", FILTER_KEY);
		List<TemplateDtoDefault> responseDto = JsonUtil.parseList(TemplateDtoDefault.class, _restClient
				.get(DEFAULT_URI, params));
		Assert.assertNotNull(responseDto);

		//update template
		TemplateDtoDefault customerTemplate = responseDto.get(0);
		customerTemplate.setBody("test get 01");

		//create customer specific templates
		_restClient.post(CONFIG_URI, customerTemplate);
		//new version
		customerTemplate.setBody("test get 02");
		_restClient.post(CONFIG_URI, customerTemplate);
		//new medium
		customerTemplate.setBody("test get phone");
		customerTemplate.setMedium(TemplateMediumDto.PHONE);
		_restClient.post(CONFIG_URI, customerTemplate);
		//new locale
		customerTemplate.setBody("test get French");
		customerTemplate.setLocale(Locale.FRENCH);
		_restClient.post(CONFIG_URI, customerTemplate);

		//new Templates
		customerTemplate.setKey("notificationKeyTest");
		customerTemplate.setMedium(TemplateMediumDto.EMAIL);
		customerTemplate.setLocale(Locale.JAPANESE);
		customerTemplate.setBody("test get new template");
		_restClient.post(CONFIG_URI, customerTemplate);
		customerTemplate.setKey("notificationKeyTest");
		customerTemplate.setLocale(Locale.CHINESE);
		_restClient.post(CONFIG_URI, customerTemplate);

		//verify
		//all
		List<TemplateDto> responseTemplates = JsonUtil.parseList(TemplateDto.class, _restClient
				.get(CONFIG_URI));
		Assert.assertNotNull(responseTemplates);
		Assert.assertEquals(5, responseTemplates.size());

		//by id
		TemplateDto dto = JsonUtil.parse(TemplateDto.class, _restClient
				.get(CONFIG_URI + "/" + responseTemplates.get(0).getId()));
		Assert.assertNotNull(dto);
		Assert.assertEquals(responseTemplates.get(0).getId(), dto.getId());

		//limits, sorting
		params = new Params();
		params.query("offset", 2);
		params.query("limit", 2);
		params.query("sorters", "key");
		responseTemplates = JsonUtil.parseList(TemplateDto.class, _restClient
				.get(CONFIG_URI, params));
		Assert.assertNotNull(responseTemplates);
		Assert.assertEquals(2, responseTemplates.size());
		Assert.assertEquals("notificationKeyTest", responseTemplates.get(0).getKey());
		Assert.assertEquals("notificationKeyTest", responseTemplates.get(1).getKey());

		//key parameter all
		params = new Params();
		params.query("filters", FILTER_KEY);
		responseTemplates = JsonUtil.parseList(TemplateDto.class, _restClient
				.get(CONFIG_URI, params));
		Assert.assertNotNull(responseTemplates);
		Assert.assertEquals(5, responseTemplates.size());

		//key parameter medium locale
		params = new Params();
		params.query("filters", FILTER_MEDIUM_LOCALE);
		responseTemplates = JsonUtil.parseList(TemplateDto.class, _restClient
				.get(CONFIG_URI, params));
		Assert.assertNotNull(responseTemplates);
		Assert.assertEquals(1, responseTemplates.size());

		params = new Params();
		params.query("filters", FILTER_KEY_MEDIUM_LOCALE);
		responseTemplates = JsonUtil.parseList(TemplateDto.class, _restClient
				.get(CONFIG_URI, params));
		Assert.assertNotNull(responseTemplates);
		Assert.assertEquals(1, responseTemplates.size());

		//clean up
		cleanUpDatabase();
	}

	@Test
	public void templateConfigResourceSaveGetWithSlackMediumTest() {
		//get template from defaults with specific key
		Params params = new Params();
		params.query("filters", FILTER_KEY_MEDIUM_SLACK_LOCALE);
		List<TemplateDtoDefault> responseDto = JsonUtil.parseList(TemplateDtoDefault.class, _restClient
				.get(DEFAULT_URI, params));
		Assert.assertNotNull(responseDto);
		TemplateDtoDefault customerTemplate = responseDto.get(0);
		customerTemplate.getSlackTemplate().setBlocks("test block update 01");
		customerTemplate.getSlackTemplate().setAutoApprovalData(SlackNotificationAutoApprovalData.builder()
				.autoApprovalMessageJSON("test block update 02")
				.autoApprovalTitle("test block update 03")
				.itemId("test block update 04")
				.itemType("test block update 05").build());

		try {
			//update slack template should not be allowed by default.
			_restClient.post(CONFIG_URI, customerTemplate);
			fail("update slack template should not be allowed by default.");
		} catch (Throwable ignore) {
		}

		//now allow customize slack template and test customization for slack template
		MockFeatureFlagClient mockFeatureFlagClient = getFeatureFlagClient();
		mockFeatureFlagClient.setBoolean(HERMES_TEMPLATES_SLACK_PERSISTENT_ENABLED, true);

		//create customer specific templates
		_restClient.post(CONFIG_URI, customerTemplate);
		//new version
		customerTemplate.getSlackTemplate().setBlocks("test block update 02");
		_restClient.post(CONFIG_URI, customerTemplate);

		//new Templates
		customerTemplate.setKey("access_request");
		customerTemplate.setMedium(TemplateMediumDto.SLACK);
		customerTemplate.setLocale(Locale.JAPANESE);
		customerTemplate.getSlackTemplate().setBlocks("test JAPANESE");
		_restClient.post(CONFIG_URI, customerTemplate);

		//verify
		//all
		List<TemplateDto> responseTemplates = JsonUtil.parseList(TemplateDto.class, _restClient
				.get(CONFIG_URI));
		Assert.assertNotNull(responseTemplates);
		Assert.assertEquals(2, responseTemplates.size());

		//by id
		TemplateDto dto = JsonUtil.parse(TemplateDto.class, _restClient
				.get(CONFIG_URI + "/" + responseTemplates.get(0).getId()));
		Assert.assertNotNull(dto);
		Assert.assertEquals(responseTemplates.get(0).getId(), dto.getId());

		//key parameter medium locale
		params = new Params();
		params.query("filters", FILTER_SLACK_MEDIUM_LOCALE_JA);
		responseTemplates = JsonUtil.parseList(TemplateDto.class, _restClient
				.get(CONFIG_URI, params));
		Assert.assertNotNull(responseTemplates);
		Assert.assertEquals(1, responseTemplates.size());
		Assert.assertEquals("test JAPANESE", responseTemplates.get(0).getSlackTemplate().getBlocks());

		//clean up
		cleanUpDatabase();
	}

	@Test
	public void templateConfigResourceInFilterTest() {
		///get template from defaults with specific key
		Params params = new Params();
		params.query("filters", FILTER_KEY);
		List<TemplateDtoDefault> responseDto = JsonUtil.parseList(TemplateDtoDefault.class, _restClient
				.get(DEFAULT_URI, params));
		Assert.assertNotNull(responseDto);

		//update template
		TemplateDtoDefault customerTemplate = responseDto.get(0);

		//new Templates
		customerTemplate.setKey("notificationKeyTest");
		_restClient.post(CONFIG_URI, customerTemplate);
		customerTemplate.setKey("notificationKey");
		customerTemplate.setMedium(TemplateMediumDto.SMS);
		customerTemplate.setLocale(Locale.FRENCH);
		_restClient.post(CONFIG_URI, customerTemplate);
		customerTemplate.setKey("default_template");
		_restClient.post(CONFIG_URI, customerTemplate);

		//verify
		//all
		List<TemplateDto> responseTemplates = JsonUtil.parseList(TemplateDto.class, _restClient
				.get(CONFIG_URI));
		Assert.assertNotNull(responseTemplates);
		Assert.assertEquals(3, responseTemplates.size());

		//with IN filter
		params = new Params();
		params.query("filters", FILTER_KEY_IN);
		responseTemplates = JsonUtil.parseList(TemplateDto.class, _restClient
				.get(CONFIG_URI, params));
		Assert.assertNotNull(responseTemplates);
		Assert.assertEquals(2, responseTemplates.size());


		//with IN filter for key and a locale
		params = new Params();
		params.query("filters", FILTER_KEY_IN + " and " + LOCALE_EQ_FR);
		responseTemplates = JsonUtil.parseList(TemplateDto.class, _restClient
				.get(CONFIG_URI, params));
		Assert.assertNotNull(responseTemplates);
		Assert.assertEquals(1, responseTemplates.size());

		//with IN filter for key and a medium
		params = new Params();
		params.query("filters", FILTER_KEY_IN + " and " + MEDIUM_EQ_SMS);
		responseTemplates = JsonUtil.parseList(TemplateDto.class, _restClient
				.get(CONFIG_URI, params));
		Assert.assertNotNull(responseTemplates);
		Assert.assertEquals(1, responseTemplates.size());

		// with IN filter for key and medium and sms
		//with IN filter for key and a medium
		params = new Params();
		params.query("filters", FILTER_KEY_IN + " and " + LOCALE_EQ_FR + " and " + MEDIUM_EQ_SMS);
		responseTemplates = JsonUtil.parseList(TemplateDto.class, _restClient
				.get(CONFIG_URI, params));
		Assert.assertNotNull(responseTemplates);
		Assert.assertEquals(1, responseTemplates.size());

		//clean up
		cleanUpDatabase();
	}

	@Test
	public void templateConfigResourceSaveVersionsTest() {
		//get template from defaults with specific key
		Params params = new Params();
		params.query("filters", FILTER_KEY);
		List<TemplateDtoDefault> responseDto = JsonUtil.parseList(TemplateDtoDefault.class, _restClient
				.get(DEFAULT_URI, params));
		Assert.assertNotNull(responseDto);

		//update template
		TemplateDtoDefault customerTemplate = responseDto.get(0);
		customerTemplate.setBody("test get 01");

		//create customer specific templates
		_restClient.post(CONFIG_URI, customerTemplate);
		//new version
		customerTemplate.setBody("test get 02");
		_restClient.post(CONFIG_URI, customerTemplate);
		//new version
		customerTemplate.setBody("test get 03");
		_restClient.post(CONFIG_URI, customerTemplate);

		//verify
		params = new Params();
		params.query("filters", FILTER_KEY_MEDIUM_LOCALE);
		List<TemplateDto> responseTemplates = JsonUtil.parseList(TemplateDto.class, _restClient
				.get(CONFIG_URI, params));
		Assert.assertNotNull(responseTemplates);
		Assert.assertEquals(1, responseTemplates.size());

		params = new Params();
		params.query("filters", FILTER_VERSION_V1);
		String versionURI = String.format(VERSION_URI_FORMAT,
				customerTemplate.getKey(), customerTemplate.getMedium(), customerTemplate.getLocale());
		List<TemplateDtoVersion> responseVersions = JsonUtil.parseList(TemplateDtoVersion.class, _restClient
				.get(versionURI, params));
		Assert.assertNotNull(responseVersions);
		Assert.assertEquals(1, responseVersions.size());

		//test sorters
		params = new Params();
		params.query("sorters", "-versionId");
		params.query("filters", FILTER_VERSIONS_ALL);
		versionURI = String.format(VERSION_URI_FORMAT,
				customerTemplate.getKey(), customerTemplate.getMedium(),
				customerTemplate.getLocale().toLanguageTag());
		responseVersions = JsonUtil.parseList(TemplateDtoVersion.class, _restClient
				.get(versionURI, params));

		Assert.assertNotNull(responseVersions);
		Assert.assertEquals(3, responseVersions.size());
		Assert.assertEquals("test get 03", responseVersions.get(2).getBody());

		params = new Params();
		params.query("sorters", "versionId");
		responseVersions = JsonUtil.parseList(TemplateDtoVersion.class, _restClient
				.get(versionURI, params));

		Assert.assertNotNull(responseVersions);
		Assert.assertEquals(3, responseVersions.size());
		Assert.assertEquals("test get 03", responseVersions.get(0).getBody());

		//restore version no user name
		TemplateDtoVersion dto = JsonUtil.parse(TemplateDtoVersion.class,  _restClient.post(VERSION_URI + "/restore/"
				+ responseVersions.get(2).getId(), null));
		Assert.assertNotNull(dto);
		Assert.assertEquals("V0", dto.getVersionInfo().getVersion());

		TemplateVersionUserInfoDto userInfo = new TemplateVersionUserInfoDto("1234", "john.smith");
		//restore version with user
		dto = JsonUtil.parse(TemplateDtoVersion.class,  _restClient.post(VERSION_URI + "/restore/"
				+ responseVersions.get(2).getId(), userInfo));
		Assert.assertNotNull(dto);
		Assert.assertEquals("V0", dto.getVersionInfo().getVersion());

		//verify
		responseVersions = JsonUtil.parseList(TemplateDtoVersion.class, _restClient
				.get(versionURI, params));
		Assert.assertNotNull(responseVersions);
		Assert.assertEquals(5, responseVersions.size());
		Assert.assertEquals("test get 02", responseVersions.get(0).getBody());

		//delete version
		_restClient.delete(VERSION_URI + "/" + responseVersions.get(2).getId());

		//verify
		responseVersions = JsonUtil.parseList(TemplateDtoVersion.class, _restClient
				.get(versionURI, params));
		Assert.assertNotNull(responseVersions);
		Assert.assertEquals(4, responseVersions.size());

		//clean up
		cleanUpDatabase();
	}

	@Test
	public void templateErrorConditionsTest() {
		//get template from defaults with specific key
		Params params = new Params();
		params.query("filters", FILTER_KEY);
		List<TemplateDtoDefault> responseDto = JsonUtil.parseList(TemplateDtoDefault.class, _restClient
				.get(DEFAULT_URI, params));
		Assert.assertNotNull(responseDto);

		//update template
		TemplateDtoDefault customerTemplate = responseDto.get(0);
		customerTemplate.setBody("test get 01");

		//create customer specific templates
		_restClient.post(CONFIG_URI, customerTemplate);

		//verify errors
		try {
			customerTemplate.setKey(null);
			customerTemplate.setLocale(null);
			customerTemplate.setMedium(null);
			_restClient.post(CONFIG_URI, customerTemplate);
			fail("Verify required missed fields in template");
		} catch (HttpResponseException ignore) {
		}

		//verify errors
		try {
			customerTemplate.setKey("some-key");
			customerTemplate.setLocale(Locale.ENGLISH);
			customerTemplate.setMedium(TemplateMediumDto.EMAIL);
			_restClient.post(CONFIG_URI, customerTemplate);
			fail("Verify default template exist");
		} catch (HttpResponseException ignore) {
		}

		params = new Params();
		params.query("filters", "versionId eq \"all\"");

		try {
			_restClient.get(CONFIG_URI, params);
			fail("Verify incorrect filters");
		} catch (HttpResponseException ignore) {
		}

		params = new Params();
		params.query("sorters", "key, versionId");
		try {
			_restClient.get(CONFIG_URI, params);
			fail("Verify incorrect sorters");
		} catch (HttpResponseException ignore) {
		}

		try {
			_restClient.get(CONFIG_URI + "/12345");
			fail("Verify get template by Id");
		} catch (Exception ignore) {
		}

		try {
			List<TemplateBulkDeleteDto> bulkDeleteDots = new ArrayList<>();
			_restClient.post(CONFIG_DELETE_URI, bulkDeleteDots);
			fail("Verify incorrect bulk delete empty");
		} catch (HttpResponseException ignore) {
		}

		try {
			_restClient.post(CONFIG_DELETE_URI, null);
			fail("Verify incorrect bulk delete null");
		} catch (HttpResponseException ignore) {
		}

		try {
			_restClient.post(CONFIG_DELETE_URI, "");
			fail("Verify incorrect bulk delete null");
		} catch (HttpResponseException ignore) {
		}

		try {
			List<TemplateBulkDeleteDto> bulkDeleteDots = new ArrayList<>();
			bulkDeleteDots.add(new TemplateBulkDeleteDto());
			_restClient.post(CONFIG_DELETE_URI, bulkDeleteDots);
			fail("Verify incorrect bulk delete empty params");
		} catch (HttpResponseException ignore) {
		}

		try {
			params = new Params();
			params.query("filters", FILTER_KEY_MEDIUM_LOCALE);
			List<TemplateDto> responseTemplates = JsonUtil.parseList(TemplateDto.class, _restClient
					.get(CONFIG_URI, params));
			Assert.assertNotNull(responseTemplates);
			Assert.assertEquals(1, responseTemplates.size());
			_restClient.delete(VERSION_URI_FORMAT + "/" + responseTemplates.get(0).getId());
			fail("Verify delete V0");
		} catch (Exception ignore) {
		}

		try {
			_restClient.delete(VERSION_URI_FORMAT + "/12345");
			fail("Verify delete wrong id");
		} catch (Exception ignore) {
		}

		try {
			JsonUtil.parse(TemplateDto.class,  _restClient.post(VERSION_URI_FORMAT + "/restore/1234",
					"john.smith"));
			fail("Verify restore not existing version");
		} catch (Exception ignore) {
		}

		//clean up
		cleanUpDatabase();
	}

	@Test
	public void templateSaveWithRaceConditionTest() throws Exception {
		//get template from defaults with specific key
		Params params = new Params();
		params.query("filters", FILTER_KEY);
		List<TemplateDtoDefault> responseDto = JsonUtil.parseList(TemplateDtoDefault.class, _restClient
				.get(DEFAULT_URI, params));
		Assert.assertNotNull(responseDto);

		//create custom template
		TemplateDtoDefault customerTemplate = responseDto.get(0);

		ExecutorService service = Executors.newFixedThreadPool(100);
		List<Callable<String>> tasks = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			final int id = i;
			tasks.add(() -> {
				//create customer specific templates and save it
				customerTemplate.setBody("test get " + id);
				return _restClient.post(CONFIG_URI, customerTemplate);
			});
		}

		for (Future<String> a : service.invokeAll(tasks)) {
			a.get();
		}

		//verify
		//only one should exist
		List<TemplateDto> responseTemplates = JsonUtil.parseList(TemplateDto.class, _restClient
				.get(CONFIG_URI));
		Assert.assertNotNull(responseTemplates);
		Assert.assertEquals(1, responseTemplates.size());

		//clean up
		cleanUpDatabase();
	}

	private RequestContext setDummyRequestContext() {
		OrgData orgData = new OrgData();
		orgData.setPod("dev");
		orgData.setOrg("acme-solar");
		orgData.setDebug(true);

		RequestContext requestContext = new RequestContext();
		requestContext.setSecurityContext(new SecurityContext() {
			@Override
			public Optional<String> getIdentity() {
				return Optional.of("admin");
			}

			@Override
			public Set<String> getGroups() {
				return Collections.emptySet();
			}

			@Override
			public Set<String> getRights() {
				return null;
			}

			@Override
			public boolean isAdministrator() {
				return true;
			}

			@Override
			public boolean isStrongAuth() {
				return true;
			}
		});
		requestContext.setOrgData(orgData);
		RequestContext.set(requestContext);

		return requestContext;
	}

	private void cleanUpDatabase() {

		List<TemplateDto> responseTemplates = JsonUtil.parseList(TemplateDto.class, _restClient
				.get(CONFIG_URI));

		if(responseTemplates.size() == 0) {
			return; //nothing to do.
		}

		//delete all remaining templates
		List<TemplateBulkDeleteDto> bulkDeleteDots = new ArrayList<>();
		for(TemplateDto td : responseTemplates) {
			bulkDeleteDots.add(new TemplateBulkDeleteDto(td.getKey()));
		}

		_restClient.post(CONFIG_DELETE_URI, bulkDeleteDots);

		//verify deleted
		responseTemplates = JsonUtil.parseList(TemplateDto.class, _restClient
				.get(CONFIG_URI));

		Assert.assertNotNull(responseTemplates);
		Assert.assertEquals(0, responseTemplates.size());
	}
}
