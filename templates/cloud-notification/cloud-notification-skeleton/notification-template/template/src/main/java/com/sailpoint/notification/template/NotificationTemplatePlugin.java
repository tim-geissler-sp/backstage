/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template;

import com.sailpoint.atlas.event.EventRegistry;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.plugin.AtlasPlugin;
import com.sailpoint.atlas.plugin.PluginConfigurationContext;
import com.sailpoint.atlas.plugin.PluginDeploymentContext;
import com.sailpoint.atlas.rest.RestConfig;
import com.sailpoint.atlas.rest.RestDeployment;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.template.event.NotificationTemplateEventHandler;
import com.sailpoint.notification.template.rest.NotificationTemplateRestApplication;

/**
 * Atlas plugin implementation for NotificationTemplate.
 */
public class NotificationTemplatePlugin implements AtlasPlugin {

	@Override
	public void configure(PluginConfigurationContext context) {
		context.addGuiceModule(new NotificationTemplateModule());
	}

	@Override
	public void deploy(PluginDeploymentContext context) {
		EventRegistry eventRegistry = context.getInstance(EventRegistry.class);
		eventRegistry.register(IdnTopic.NOTIFICATION, EventType.NOTIFICATION_USER_PREFERENCES_MATCHED , NotificationTemplateEventHandler.class);

		RestConfig restConfig = context.getInstance(RestConfig.class);
		restConfig.addDeployment(new RestDeployment("/notification/template",
				NotificationTemplateRestApplication.class));
	}
}