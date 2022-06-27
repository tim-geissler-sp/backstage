/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.context;

import com.sailpoint.atlas.dynamodb.DynamoDBService;
import com.sailpoint.atlas.event.EventRegistry;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.idn.IdnServiceModule;
import com.sailpoint.atlas.plugin.AtlasPlugin;
import com.sailpoint.atlas.plugin.PluginConfigurationContext;
import com.sailpoint.atlas.plugin.PluginDeploymentContext;
import com.sailpoint.atlas.rest.RestConfig;
import com.sailpoint.atlas.rest.RestDeployment;
import com.sailpoint.atlas.service.ServiceFactory;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.context.common.model.GlobalContextEntity;
import com.sailpoint.notification.context.event.BrandingChangedEventHandler;
import com.sailpoint.notification.context.event.EmailRedirectionEventHandler;
import com.sailpoint.notification.context.event.OrgLifecycleEventHandler;
import com.sailpoint.notification.context.rest.GlobalContextRestApplication;


/**
 * Atlas plugin implementation for GlobalContext.
 */
public class GlobalContextPlugin implements AtlasPlugin {

	@Override
	public void configure(PluginConfigurationContext context) {
		context.addGuiceModule(new GlobalContextModule());
		context.addGuiceModule(new IdnServiceModule());
	}

	@Override
	public void deploy(PluginDeploymentContext context) {
		EventRegistry eventRegistry = context.getInstance(EventRegistry.class);

		eventRegistry.register(IdnTopic.BRANDING, EventType.BRANDING_CREATED, BrandingChangedEventHandler.class);
		eventRegistry.register(IdnTopic.BRANDING, EventType.BRANDING_UPDATED, BrandingChangedEventHandler.class);
		eventRegistry.register(IdnTopic.BRANDING, EventType.BRANDING_DELETED, BrandingChangedEventHandler.class);


		eventRegistry.register(IdnTopic.CC, EventType.EMAIL_REDIRECTION_ENABLED, EmailRedirectionEventHandler.class);
		eventRegistry.register(IdnTopic.CC, EventType.EMAIL_REDIRECTION_DISABLED, EmailRedirectionEventHandler.class);

		//Listening to the Notification topic as well for tests
		eventRegistry.register(IdnTopic.NOTIFICATION, EventType.EMAIL_REDIRECTION_ENABLED, EmailRedirectionEventHandler.class);
		eventRegistry.register(IdnTopic.NOTIFICATION, EventType.EMAIL_REDIRECTION_DISABLED, EmailRedirectionEventHandler.class);

		eventRegistry.register(IdnTopic.ORG_LIFECYCLE, EventType.ORG_CREATED, OrgLifecycleEventHandler.class);
		eventRegistry.register(IdnTopic.ORG_LIFECYCLE, EventType.ORG_UPGRADED, OrgLifecycleEventHandler.class);

		RestConfig restConfig = context.getInstance(RestConfig.class);
		restConfig.addDeployment(new RestDeployment("/context",
				GlobalContextRestApplication.class));

		DynamoDBService dynamoDBService = ServiceFactory.getService(DynamoDBService.class);
		dynamoDBService.createTable(GlobalContextEntity.class,null);
	}
}