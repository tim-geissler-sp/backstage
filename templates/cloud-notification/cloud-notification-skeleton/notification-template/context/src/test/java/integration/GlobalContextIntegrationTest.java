/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package integration;

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
import com.sailpoint.notification.context.common.model.BrandConfig;
import com.sailpoint.notification.context.event.BrandingChangedEventHandler;
import org.junit.Assert;
import org.junit.Test;

/**
 * Integration test that loads Atlas and Notification Template modules.
 */
@EnableKafkaServer(topics="notification")
@EnableInMemoryDynamoDB
public class GlobalContextIntegrationTest extends IdnAtlasIntegrationTest {

	private static String BRANDING_CREATED_DEBUG_ENDPOINT = "/context/debug/publish/event/branding/BRANDING_CREATED";

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
	}

	@Test
	public void BrandingChangedEventHandlerTest() {
		BrandingChangedEventHandler eventHandler = _application.getInjector()
				.getInstance(BrandingChangedEventHandler.class);
		Assert.assertNotNull(eventHandler);
	}

	@Test
	public void GlobalContextDebugTest() {
		BrandConfig brandConfig = new BrandConfig();
		brandConfig.setEmailFromAddress("no-reply@sailpoint.com");
		brandConfig.setProductName("Acme Solar Flare Company");
		brandConfig.setName("test");
		String key = _restClient.post(BRANDING_CREATED_DEBUG_ENDPOINT,
				brandConfig);
		Assert.assertNotNull(key);
	}
}
