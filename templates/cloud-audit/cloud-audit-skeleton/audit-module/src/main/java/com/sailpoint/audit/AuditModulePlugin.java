/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit;

import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.s3.AmazonS3;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.sailpoint.atlas.idn.IdnMessageScope;
import com.sailpoint.atlas.message.MessageScopeBinding;
import com.sailpoint.atlas.message.MessagingConfig;
import com.sailpoint.atlas.plugin.AtlasPlugin;
import com.sailpoint.atlas.plugin.PluginConfigurationContext;
import com.sailpoint.atlas.plugin.PluginDeploymentContext;
import com.sailpoint.atlas.plugin.PluginStartupContext;
import com.sailpoint.atlas.rest.RestConfig;
import com.sailpoint.atlas.rest.RestDeployment;
import com.sailpoint.atlas.service.AmazonService;
import com.sailpoint.atlas.task.schedule.service.TaskScheduleClientModule;
import com.sailpoint.audit.event.model.EventCatalog;
import com.sailpoint.audit.event.model.EventTemplates;
import com.sailpoint.audit.event.util.ResourceUtils;
import com.sailpoint.audit.message.AddAthenaPartitions;
import com.sailpoint.audit.message.BulkSyncAuditEvents;
import com.sailpoint.audit.message.BulkSyncS3AuditEvents;
import com.sailpoint.audit.message.BulkUploadAuditEvents;
import com.sailpoint.audit.message.OrgCreated;
import com.sailpoint.audit.message.OrgUpgraded;
import com.sailpoint.audit.message.PublishAuditCounts;
import com.sailpoint.audit.rest.AuditRestApplication;
import com.sailpoint.audit.service.AthenaClientProvider;
import com.sailpoint.audit.service.AthenaDataCatalogService;
import com.sailpoint.audit.service.AuditEventReportingService;
import com.sailpoint.audit.service.AuditEventService;
import com.sailpoint.audit.service.AuditFirehoseServiceModule;
import com.sailpoint.audit.service.AuditServiceModule;
import com.sailpoint.audit.service.BulkSyncS3AuditEventsService;
import com.sailpoint.audit.service.BulkUploadAuditEventsService;
import com.sailpoint.audit.service.DataCatalogService;
import com.sailpoint.audit.service.DeletedOrgsCacheService;
import com.sailpoint.audit.service.MetricsPublisherService;
import com.sailpoint.audit.service.S3ClientProvider;
import com.sailpoint.audit.service.SyncCisToS3Service;
import com.sailpoint.audit.service.SyncJobManager;
import com.sailpoint.audit.service.SyncJobScheduler;
import com.sailpoint.audit.service.mapping.DomainAuditEventsUtil;
import com.sailpoint.audit.service.util.AuditUtil;
import com.sailpoint.audit.util.AuditEventSearchQueryUtil;
import com.sailpoint.audit.util.BulkUploadUtil;
import com.sailpoint.audit.verification.AuditVerificationModule;
import com.sailpoint.audit.writer.BulkWriterFactory;

public class AuditModulePlugin implements AtlasPlugin {

	@Override
	public void configure(PluginConfigurationContext context) {
		context.addGuiceModule(new AuditServiceModule());
		context.addGuiceModule(new AuditFirehoseServiceModule());
		context.addGuiceModule(new TaskScheduleClientModule());
		context.addGuiceModule(new AbstractModule() {
			@Override
			protected void configure() {
				install(new AuditVerificationModule());
				bind(OrgUpgraded.class);
				bind(OrgCreated.class);
				bind(EventCatalog.class);
				bind(EventTemplates.class);
				bind(ResourceUtils.class);
				bind(DomainAuditEventsUtil.class);
				bind(BulkUploadUtil.class);
				bind(BulkSyncAuditEvents.class);
				bind(BulkUploadAuditEvents.class);
				bind(BulkSyncS3AuditEvents.class);
				bind(BulkUploadAuditEventsService.class);
				bind(BulkSyncS3AuditEventsService.class);
				bind(SyncCisToS3Service.class);
				bind(BulkWriterFactory.class);
				bind(SyncJobManager.class);
				bind(SyncJobScheduler.class);
				bind(AuditUtil.class);
				bind(AmazonService.class);
				bind(AmazonAthena.class).annotatedWith(Names.named("AthenaClient"))
						.toProvider(AthenaClientProvider.class).in(Scopes.SINGLETON);
				bind(DataCatalogService.class)
						.annotatedWith(Names.named("Athena")).to(AthenaDataCatalogService.class);
				bind(AddAthenaPartitions.class);
				bind(AmazonS3.class)
						.toProvider(S3ClientProvider.class).in(Scopes.SINGLETON);
				bind(PublishAuditCounts.class);
				bind(MetricsPublisherService.class);
				bind(AuditEventReportingService.class);
				bind(AuditEventSearchQueryUtil.class);
				bind(AuditEventService.class);
				bind(DeletedOrgsCacheService.class);
			}
		});
	}

	@Override
	public void deploy(PluginDeploymentContext context) {
		RestConfig restConfig = context.getInstance(RestConfig.class);
		restConfig.addDeployment(new RestDeployment("/audit", AuditRestApplication.class));

		MessagingConfig messagingConfig = context.getInstance(MessagingConfig.class);

		messagingConfig.addBinding(new MessageScopeBinding(IdnMessageScope.AUDIT)
				.bind(OrgUpgraded.PAYLOAD_TYPE.ORG_UPGRADED, OrgUpgraded.class)
				.bind(OrgCreated.PAYLOAD_TYPE.ORG_CREATED, OrgCreated.class)
				.bind(AddAthenaPartitions.PAYLOAD_TYPE.ADD_ATHENA_PARTITIONS, AddAthenaPartitions.class)
				.bind(PublishAuditCounts.PAYLOAD_TYPE.PUBLISH_AUDIT_COUNTS, PublishAuditCounts.class)
				.bind(BulkSyncAuditEvents.PAYLOAD_TYPE.BULK_SYNCHRONIZE_AUDIT_EVENTS,
						BulkSyncAuditEvents.class)
				.bind(BulkUploadAuditEvents.PAYLOAD_TYPE.BULK_UPLOAD_AUDIT_EVENTS,
						BulkUploadAuditEvents.class)
				.bind(BulkSyncS3AuditEvents.PAYLOAD_TYPE.BULK_SYNCHRONIZE_S3_AUDIT_EVENTS,
						BulkSyncS3AuditEvents.class));
	}

	@Override
	public void start(PluginStartupContext context) {
		SyncJobScheduler syncJobScheduler = context.getInstance(SyncJobScheduler.class);
		syncJobScheduler.init("schedule_properties.json");
	}
}
