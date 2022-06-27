/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification;

import com.sailpoint.atlas.chronicle.AtlasChroniclePlugin;
import com.sailpoint.atlas.event.AtlasDefaultEventHandlerModule;
import com.sailpoint.atlas.event.AtlasEventPlugin;
import com.sailpoint.atlas.event.EventRegistry;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.health.AtlasHealthPlugin;
import com.sailpoint.atlas.idn.IdnAtlasApplication;
import com.sailpoint.atlas.logging.AtlasDynamicLoggingPlugin;
import com.sailpoint.atlas.metrics.AtlasMetricsPlugin;
import com.sailpoint.atlas.profiling.AtlasDynamicProfilingPlugin;
import com.sailpoint.atlas.plugin.AtlasPlugin;
import com.sailpoint.atlas.plugin.PluginConfigurationContext;
import com.sailpoint.atlas.plugin.PluginDeploymentContext;
import com.sailpoint.atlas.rest.RestConfig;
import com.sailpoint.atlas.rest.RestDeployment;
import com.sailpoint.atlas.service.AtlasServiceModule;
import com.sailpoint.atlas.tracing.plugin.AtlasTracingPlugin;
import com.sailpoint.atlas.usage.plugin.AtlasUsagePlugin;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.context.GlobalContextPlugin;
import com.sailpoint.notification.event.OrgDeletedEventHandler;
import com.sailpoint.notification.interest.matcher.NotificationInterestMatcherPlugin;
import com.sailpoint.notification.sender.NotificationSenderPlugin;
import com.sailpoint.notification.template.NotificationTemplatePlugin;
import com.sailpoint.notification.template.manager.NotificationTemplateManagerPlugin;
import com.sailpoint.notification.userpreferences.NotificationUserPreferencesPlugin;

/**
 * Base Application.
 * Main service for all the notification related parts
 */
public class NotificationServerApplication extends IdnAtlasApplication {

	public NotificationServerApplication() {

		IdnAtlasApplication.setStack("hermes");

		registerPlugin(new AtlasChroniclePlugin());
		registerPlugin(new AtlasEventPlugin());
		registerPlugin(new AtlasHealthPlugin());
		registerPlugin(new AtlasMetricsPlugin());
		registerPlugin(new AtlasDynamicLoggingPlugin());
		registerPlugin(new AtlasUsagePlugin());
		registerPlugin(new AtlasTracingPlugin());
		registerPlugin(new AtlasDynamicProfilingPlugin());
		registerPlugin(new NotificationInterestMatcherPlugin());
		registerPlugin(new NotificationUserPreferencesPlugin());
		registerPlugin(new GlobalContextPlugin());
		registerPlugin(new NotificationTemplatePlugin());
		registerPlugin(new NotificationSenderPlugin());
		registerPlugin(new NotificationTemplateManagerPlugin());
		registerPlugin(new NotificationPlugin());

		addServiceModule(new AtlasDefaultEventHandlerModule());
		addServiceModule(new AtlasServiceModule());

	}

	public static void main(String[] args) {
		IdnAtlasApplication.run(NotificationServerApplication.class, args);
	}
}
