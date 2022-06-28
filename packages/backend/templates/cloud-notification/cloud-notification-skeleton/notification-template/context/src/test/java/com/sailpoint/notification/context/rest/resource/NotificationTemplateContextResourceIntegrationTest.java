/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.context.rest.resource;

import com.google.common.collect.ImmutableMap;
import com.sailpoint.atlas.AtlasApplication;
import com.sailpoint.atlas.dynamodb.DynamoDBService;
import com.sailpoint.atlas.dynamodb.DynamoDBServiceModule;
import com.sailpoint.atlas.event.AtlasDefaultEventHandlerModule;
import com.sailpoint.atlas.event.AtlasEventPlugin;
import com.sailpoint.atlas.health.AtlasHealthPlugin;
import com.sailpoint.atlas.plugin.AtlasPlugin;
import com.sailpoint.atlas.plugin.PluginConfigurationContext;
import com.sailpoint.atlas.service.ServiceFactory;
import com.sailpoint.atlas.test.integration.IdnAtlasIntegrationTest;
import com.sailpoint.atlas.test.integration.IdnAtlasIntegrationTestApplication;
import com.sailpoint.atlas.test.integration.dynamodb.EnableInMemoryDynamoDB;
import com.sailpoint.atlas.test.integration.kafka.EnableKafkaServer;
import com.sailpoint.mantisclient.HttpResponseException;
import com.sailpoint.notification.context.GlobalContextPlugin;
import com.sailpoint.notification.context.common.model.GlobalContext;
import com.sailpoint.notification.context.common.model.GlobalContextEntity;
import com.sailpoint.notification.context.common.model.NotificationTemplateContextDto;
import com.sailpoint.notification.context.common.repository.GlobalContextRepository;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Integration tests to validate the Notification Template Context.
 */
@EnableKafkaServer(topics="notification")
@EnableInMemoryDynamoDB
public class NotificationTemplateContextResourceIntegrationTest extends IdnAtlasIntegrationTest {

	final static String PATH_TEST_RESOURCE = "/context/v3/notification-template-context";

	GlobalContextRepository _globalContextRepository;

	@Override
	protected AtlasApplication createApplication() {
		return new IdnAtlasIntegrationTestApplication() {{
			registerPlugin(new AtlasHealthPlugin());
			registerPlugin(new AtlasEventPlugin());
			registerPlugin(new GlobalContextPlugin());
			addServiceModule(new AtlasDefaultEventHandlerModule());
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

		DynamoDBService dynamoDBService = ServiceFactory.getService(DynamoDBService.class);
		dynamoDBService.createTable(GlobalContextEntity.class, null);

		_globalContextRepository = ServiceFactory.getService(GlobalContextRepository.class);
	}


	/**
	 * Single test method to take advantage of the restClient instance. The tenant in the
	 * requestContext comes from the BaseRestClient initialization in IdnAtlasIntegrationTest.
	 */
	@Test
	public void notFoundTenantAndExistingTenantTest() {

		try {
			// Given no tenant in the respository
			_restClient.getJson(NotificationTemplateContextDto.class, PATH_TEST_RESOURCE);
		} catch (HttpResponseException e) {
			// Then should receive a 404
			assertEquals(404, e.getStatusCode());
		}

		// Given GlobalContext in the repository.
		GlobalContext globalContext = new GlobalContext("acme-solar");
		globalContext.setAttributes(ImmutableMap.of("key", "value"));
		_globalContextRepository.save(globalContext);

		// When query is made
		NotificationTemplateContextDto notificationTemplateContextDto = _restClient.getJson(NotificationTemplateContextDto.class, PATH_TEST_RESOURCE);

		// Then response is valid
		notificationTemplateContextDto.getAttributes();
		assertEquals("value", notificationTemplateContextDto.getAttributes().get("key"));
	}
}
