/*
 *
 * Copyright (c) 2017. SailPoint Technologies, Inc. All rights reserved.
 *
 */

package com.sailpoint.audit.service;

import com.amazonaws.services.athena.AmazonAthena;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.sailpoint.audit.event.AuditEventS3Handler;
import com.sailpoint.audit.event.OrgDeleteHandler;
import com.sailpoint.audit.event.model.EventCatalog;
import com.sailpoint.audit.event.model.EventTemplates;
import com.sailpoint.audit.event.util.ResourceUtils;
import com.sailpoint.audit.service.mapping.AuditEventTypeToAction;
import com.sailpoint.audit.service.mapping.DomainAuditEventsUtil;
import com.sailpoint.audit.service.normalizer.NormalizerFactory;
import com.sailpoint.audit.service.util.AuditUtil;
import com.sailpoint.audit.verification.AuditVerificationModule;

/**
 * Created by mark.boyle on 9/29/17.
 */
public class AuditEventServiceModule extends AbstractModule {

	private static final int CLIENT_EXECUTION_TIMEOUT = 60000;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void configure() {
		binder().requireExplicitBindings();

		install(new AuditVerificationModule());
		bind(AuditEventService.class);
		bind(EventNormalizerService.class);
		bind(NormalizerFactory.class);
		bind(DomainAuditEventsUtil.class);
		bind(EventCatalog.class);
		bind(EventTemplates.class);
		bind(ResourceUtils.class);
		bind(AuditEventTypeToAction.class);
		bind(ObjectMapper.class);
		bind(AmazonAthena.class).annotatedWith(Names.named("AthenaClient"))
				.toProvider(AthenaClientProvider.class).in(Scopes.SINGLETON);;
		bind(DataCatalogService.class)
				.annotatedWith(Names.named("Athena")).to(AthenaDataCatalogService.class);
		bind(OrgDeleteHandler.class);
		bind(DeletedOrgsCacheService.class);
		bind(AuditEventS3Handler.class);
		bind(AuditUtil.class);
		bind(ResourceUtils.class);
	}
}
