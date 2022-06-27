/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences;

import com.sailpoint.atlas.dynamodb.DynamoDBService;
import com.sailpoint.atlas.dynamodb.DynamoDBServiceModule;
import com.sailpoint.atlas.event.EventRegistry;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.plugin.AtlasPlugin;
import com.sailpoint.atlas.plugin.PluginConfigurationContext;
import com.sailpoint.atlas.plugin.PluginDeploymentContext;
import com.sailpoint.atlas.rest.RestConfig;
import com.sailpoint.atlas.rest.RestDeployment;
import com.sailpoint.atlas.service.ServiceFactory;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.orgpreferences.repository.impl.dynamodb.entity.TenantPreferencesEntity;
import com.sailpoint.notification.orgpreferences.repository.impl.dynamodb.entity.TenantUserPreferencesEntity;
import com.sailpoint.notification.orgpreferences.repository.rest.NotificationPreferencesRestApplication;
import com.sailpoint.notification.orgpreferences.repository.rest.resource.PreferencesV3Resource;
import com.sailpoint.notification.userpreferences.event.IdentityAttributesChangedEventHandler;
import com.sailpoint.notification.userpreferences.event.IdentityCreatedEventHandler;
import com.sailpoint.notification.userpreferences.event.IdentityDeletedEventHandler;
import com.sailpoint.notification.userpreferences.event.OrgLifecycleEventHandler;
import com.sailpoint.notification.userpreferences.event.UserPreferencesEventHandler;
import com.sailpoint.notification.userpreferences.repository.impl.dynamodb.entity.UserPreferencesEntity;
import com.sailpoint.notification.userpreferences.rest.NotificationUserPreferencesRestApplication;
import com.sailpoint.notification.userpreferences.rest.OrgLifecycleRestApplication;

/**
 * User Preferences Altas plugin implementation
 */
public class NotificationUserPreferencesPlugin implements AtlasPlugin {

	@Override
	public void configure(PluginConfigurationContext context) {
		context.addGuiceModule(new NotificationUserPreferencesModule());
		context.addGuiceModule(new DynamoDBServiceModule());
	}

	@Override
	public void deploy(PluginDeploymentContext context) {
		RestConfig restConfig = context.getInstance(RestConfig.class);
		restConfig.addDeployment(new RestDeployment("/user/preferences",
				NotificationUserPreferencesRestApplication.class));
		restConfig.addDeployment(new RestDeployment("/org-lifecycle",
				OrgLifecycleRestApplication.class));
		restConfig.addDeployment(new RestDeployment("/v3/notification-preferences",
				NotificationPreferencesRestApplication.class));

		EventRegistry eventRegistry = context.getInstance(EventRegistry.class);
		eventRegistry.register(IdnTopic.NOTIFICATION,
				EventType.NOTIFICATION_INTEREST_MATCHED,
				UserPreferencesEventHandler.class);
		eventRegistry.register(IdnTopic.ORG_LIFECYCLE,
				EventType.ORG_DELETED,
				OrgLifecycleEventHandler.class);

		eventRegistry.register(IdnTopic.IDENTITY_EVENT,
				EventType.IDENTITY_DELETED,
				IdentityDeletedEventHandler.class);

		eventRegistry.register(IdnTopic.IDENTITY_EVENT,
				EventType.IDENTITY_ATTRIBUTE_CHANGED,
				IdentityAttributesChangedEventHandler.class);

		eventRegistry.register(IdnTopic.IDENTITY_EVENT,
				EventType.IDENTITY_CREATED,
				IdentityCreatedEventHandler.class);

		DynamoDBService dynamoDBService = ServiceFactory.getService(DynamoDBService.class);
		dynamoDBService.createTable(UserPreferencesEntity.class, null);
		dynamoDBService.createTable(TenantPreferencesEntity.class, DynamoDBService.PROJECTION_ALL);
		dynamoDBService.createTable(TenantUserPreferencesEntity.class, DynamoDBService.PROJECTION_ALL);
	}
}
