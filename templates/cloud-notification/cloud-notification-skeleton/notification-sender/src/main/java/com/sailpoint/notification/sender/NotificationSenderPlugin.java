/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.sender;

import com.sailpoint.atlas.dynamodb.DynamoDBServiceModule;
import com.sailpoint.atlas.event.EventRegistry;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.event.lifecycle.EventLifecycleRegistry;
import com.sailpoint.atlas.plugin.AtlasPlugin;
import com.sailpoint.atlas.plugin.PluginConfigurationContext;
import com.sailpoint.atlas.plugin.PluginDeploymentContext;
import com.sailpoint.atlas.rest.RestConfig;
import com.sailpoint.atlas.rest.RestDeployment;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.sender.common.event.NotificationRenderedEventHandler;
import com.sailpoint.notification.sender.common.lifecycle.NotificationLifecycleMetricsReporter;

public class NotificationSenderPlugin implements AtlasPlugin {

	@Override
	public void configure(PluginConfigurationContext context) {
		context.addGuiceModule(new NotificationSenderModule());
		context.addGuiceModule(new DynamoDBServiceModule());
	}

	@Override
	public void deploy(PluginDeploymentContext context) {
		RestConfig restConfig = context.getInstance(RestConfig.class);
		restConfig.addDeployment(new RestDeployment("/sender", NotificationSenderRestApplication.class));

		EventLifecycleRegistry lifecycleRegistry = context.getInstance(EventLifecycleRegistry.class);
		NotificationLifecycleMetricsReporter metricsReporter = context.getInstance(NotificationLifecycleMetricsReporter.class);
		lifecycleRegistry.register(metricsReporter);

		EventRegistry eventRegistry = context.getInstance(EventRegistry.class);
		eventRegistry.register(IdnTopic.NOTIFICATION, EventType.NOTIFICATION_RENDERED, NotificationRenderedEventHandler.class);
	}
}
