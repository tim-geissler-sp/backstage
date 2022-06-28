/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved
 */
package com.sailpoint.notification;

import com.sailpoint.atlas.event.EventRegistry;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.plugin.AtlasPlugin;
import com.sailpoint.atlas.plugin.PluginConfigurationContext;
import com.sailpoint.atlas.plugin.PluginDeploymentContext;
import com.sailpoint.atlas.rest.RestConfig;
import com.sailpoint.atlas.rest.RestDeployment;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.event.OrgDeletedEventHandler;

public class NotificationPlugin implements AtlasPlugin {
    @Override
    public void configure(PluginConfigurationContext context) {
        context.addGuiceModule(new NotificationModule());
    }

    @Override
    public void deploy(PluginDeploymentContext context) {
        RestConfig restConfig = context.getInstance(RestConfig.class);
        restConfig.addDeployment(new RestDeployment("/notification", NotificationRestApplication.class));

        EventRegistry eventRegistry = context.getInstance(EventRegistry.class);
        eventRegistry.register(IdnTopic.ORG_LIFECYCLE, EventType.ORG_DELETED, OrgDeletedEventHandler.class);
    }
}
