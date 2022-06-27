/*
 * Copyright (c) 2017. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.sailpoint.audit.service.mapping.AuditEventTypeToAction;
import com.sailpoint.audit.service.normalizer.NormalizerFactory;
import com.sailpoint.mantis.platform.service.search.BulkSynchronizationService;


/**
 *
 * Created by mark.boyle on 4/3/17.
 */
public class AuditServiceModule extends AbstractModule {
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void configure() {
		binder().requireExplicitBindings();

		bind(RDEServiceClient.class);
		bind(BulkSynchronizationService.class);
		bind(EventNormalizerService.class);
		bind(NormalizerFactory.class);
		bind(AuditReportService.class);
		bind(AuditEventTypeToAction.class);
		bind(ObjectMapper.class);
	}
}
