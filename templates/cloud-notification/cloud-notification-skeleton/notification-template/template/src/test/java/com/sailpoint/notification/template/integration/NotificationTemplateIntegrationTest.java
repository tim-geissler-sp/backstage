/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.integration;

import com.sailpoint.atlas.AtlasApplication;
import com.sailpoint.atlas.dynamodb.DynamoDBServiceModule;
import com.sailpoint.atlas.event.AtlasDefaultEventHandlerModule;
import com.sailpoint.atlas.event.AtlasEventPlugin;
import com.sailpoint.atlas.health.AtlasHealthPlugin;
import com.sailpoint.atlas.plugin.AtlasPlugin;
import com.sailpoint.atlas.plugin.PluginConfigurationContext;
import com.sailpoint.atlas.test.integration.IdnAtlasIntegrationTest;
import com.sailpoint.atlas.test.integration.IdnAtlasIntegrationTestApplication;
import com.sailpoint.atlas.test.integration.dynamodb.EnableInMemoryDynamoDB;
import com.sailpoint.atlas.test.integration.kafka.EnableKafkaServer;
import com.sailpoint.notification.context.GlobalContextPlugin;
import com.sailpoint.notification.template.NotificationTemplatePlugin;
import com.sailpoint.notification.template.event.NotificationTemplateEventHandler;
import org.junit.Assert;
import org.junit.Test;

/**
 * Integration test that loads Atlas and Notification Template modules.
 */
@EnableInMemoryDynamoDB
@EnableKafkaServer(topics="notification")
public class NotificationTemplateIntegrationTest extends IdnAtlasIntegrationTest {

	@Override
	protected AtlasApplication createApplication() {
		return new IdnAtlasIntegrationTestApplication() {{
			registerPlugin(new AtlasHealthPlugin());
			registerPlugin(new AtlasEventPlugin());
			registerPlugin(new NotificationTemplatePlugin());
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
	}

	@Test
	public void NotificationTemplateEventHandlerTest() {
		NotificationTemplateEventHandler eventHandler = _application.getInjector()
				.getInstance(NotificationTemplateEventHandler.class);
		Assert.assertNotNull(eventHandler);
	}
}
