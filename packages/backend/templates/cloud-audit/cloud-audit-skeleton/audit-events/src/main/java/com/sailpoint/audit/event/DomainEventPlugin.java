/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.event;

import com.amazonaws.services.sqs.AmazonSQS;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.sailpoint.atlas.event.EventRegistry;
import com.sailpoint.atlas.plugin.AtlasPlugin;
import com.sailpoint.atlas.plugin.PluginConfigurationContext;
import com.sailpoint.atlas.plugin.PluginDeploymentContext;
import com.sailpoint.audit.event.model.EventCatalog;
import com.sailpoint.audit.event.model.EventTemplates;
import com.sailpoint.audit.event.normalizer.NormalizerFactory;
import com.sailpoint.audit.event.util.ResourceUtils;
import com.sailpoint.audit.verification.AuditVerificationModule;

public class DomainEventPlugin implements AtlasPlugin {

	@Override
	public void configure(PluginConfigurationContext context) {

		context.addGuiceModule(new Module());
	}

	@Override
	public void deploy(PluginDeploymentContext context) {

		EventCatalog eventCatalog = context.getInstance(EventCatalog.class);
		EventRegistry eventRegistry = context.getInstance(EventRegistry.class);

		eventCatalog.stream().forEach(eventDescriptor -> {

			eventRegistry.register(eventDescriptor.getTopic(), eventDescriptor.getEventType(), DomainEventHandler.class);
		});
	}

	public static class Module extends AbstractModule {

		@Override
		protected void configure() {
			install(new AuditVerificationModule());
			bind(EventCatalog.class);
			bind(EventTemplates.class);
			bind(NormalizerFactory.class);
			bind(ResourceUtils.class);
			bind(DomainEventHandler.class);
		}
	}
}
