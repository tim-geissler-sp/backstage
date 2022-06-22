/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit;

import com.amazonaws.services.s3.AmazonS3;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.sailpoint.atlas.event.EventRegistry;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.idn.IdnMessageScope;
import com.sailpoint.atlas.message.MessageScopeBinding;
import com.sailpoint.atlas.message.MessagingConfig;
import com.sailpoint.atlas.plugin.AtlasPlugin;
import com.sailpoint.atlas.plugin.PluginConfigurationContext;
import com.sailpoint.atlas.plugin.PluginDeploymentContext;
import com.sailpoint.atlas.plugin.PluginStartupContext;
import com.sailpoint.audit.event.AuditEventS3Handler;
import com.sailpoint.audit.event.OrgDeleteHandler;
import com.sailpoint.audit.message.AuditEventHandler;
import com.sailpoint.audit.persistence.S3PersistenceManager;
import com.sailpoint.audit.service.AuditEventServiceModule;
import com.sailpoint.audit.service.AuditFirehoseServiceModule;
import com.sailpoint.audit.service.AuditKafkaEventType;
import com.sailpoint.audit.service.FirehoseCacheService;
import com.sailpoint.audit.service.S3ClientProvider;

public class AuditEventPlugin implements AtlasPlugin {
	@Override
	public void configure(PluginConfigurationContext context) {
		context.addGuiceModule(new AuditEventServiceModule());
		context.addGuiceModule(new AuditFirehoseServiceModule());
		context.addGuiceModule(new AbstractModule() {
			@Override
			protected void configure() {
				bind(AmazonS3.class).toProvider(S3ClientProvider.class).in(Scopes.SINGLETON);
				requestStaticInjection(S3PersistenceManager.class);
				bind(AuditEventHandler.class);
			}
		});
	}

	@Override
	public void deploy(PluginDeploymentContext context) {
		MessagingConfig messagingConfig = context.getInstance(MessagingConfig.class);

		messagingConfig.addBinding(new MessageScopeBinding(IdnMessageScope.AUDIT)
				.bind(AuditEventHandler.MessageType.AUDIT_EVENT, AuditEventHandler.class));

		EventRegistry eventRegistry = context.getInstance(EventRegistry.class);
		eventRegistry.register(IdnTopic.ORG_LIFECYCLE, "ORG_DELETED", OrgDeleteHandler.class);
		eventRegistry.register(IdnTopic.AUDIT,
				AuditKafkaEventType.AUDIT_WHITELISTED.name(),
				AuditEventS3Handler.class);
		eventRegistry.register(IdnTopic.AUDIT,
				AuditKafkaEventType.AUDIT_NONWHITELISTED.name(),
				AuditEventS3Handler.class);
	}

	@Override
	public void start(PluginStartupContext context) {
		FirehoseCacheService firehoseCacheService = context.getInstance(FirehoseCacheService.class);
		firehoseCacheService.primeFirehoseCache();
	}
}
