/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.manager;

import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.dynamodb.DynamoDBService;
import com.sailpoint.atlas.dynamodb.DynamoDBServiceModule;
import com.sailpoint.atlas.plugin.AtlasPlugin;
import com.sailpoint.atlas.plugin.PluginConfigurationContext;
import com.sailpoint.atlas.plugin.PluginDeploymentContext;
import com.sailpoint.atlas.rest.RestConfig;
import com.sailpoint.atlas.rest.RestDeployment;
import com.sailpoint.atlas.service.ServiceFactory;
import com.sailpoint.notification.template.common.model.version.TemplateVersion;
import com.sailpoint.notification.template.common.repository.impl.dynamodb.entity.TemplatePersistentEntity;
import com.sailpoint.notification.template.manager.rest.NotificationTemplateManagerRestApplicationTemplates;
import com.sailpoint.notification.template.manager.rest.NotificationTemplateManagerRestApplicationDefault;
import com.sailpoint.notification.template.manager.rest.NotificationTemplateManagerRestApplicationVersions;
import com.sailpoint.notification.userpreferences.repository.impl.dynamodb.entity.UserPreferencesEntity;


/**
 * Atlas plugin implementation for NotificationTemplateManager.
 */
public class NotificationTemplateManagerPlugin implements AtlasPlugin {

	@Override
	public void configure(PluginConfigurationContext context) {
		context.addGuiceModule(new NotificationTemplateManagerModule());
		context.addGuiceModule(new DynamoDBServiceModule());
	}

	@Override
	public void deploy(PluginDeploymentContext context) {
		RestConfig restConfig = context.getInstance(RestConfig.class);
		restConfig.addDeployment(new RestDeployment("/v3/notification-template-defaults",
				NotificationTemplateManagerRestApplicationDefault.class));

		restConfig.addDeployment(new RestDeployment("/v3/notification-templates",
				NotificationTemplateManagerRestApplicationTemplates.class));

		AtlasConfig config = context.getInstance(AtlasConfig.class);

		//by default disable until we have agreement with API review board about version usage.
		boolean enableVersions = config.getBoolean(TemplateVersion.HERMES_CONFIG_ENABLE_VERSION_SUPPORT, false);

		if(enableVersions) {
			restConfig.addDeployment(new RestDeployment("/v3/notification-template-versions",
					NotificationTemplateManagerRestApplicationVersions.class));
		}

		DynamoDBService dynamoDBService = ServiceFactory.getService(DynamoDBService.class);
		dynamoDBService.createTable(TemplatePersistentEntity.class,
				DynamoDBService.PROJECTION_ALL);
		dynamoDBService.createTable(UserPreferencesEntity.class, null);
	}
}