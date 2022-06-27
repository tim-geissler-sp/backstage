/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service.normalizer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.audit.service.mapping.DomainAuditEventsUtil;
import com.sailpoint.audit.service.util.AuditUtil;
import sailpoint.object.AuditEvent;

@Singleton
public class NormalizerFactory {

	@Inject
    DomainAuditEventsUtil _domainAuditEventsUtil;

	public Normalizer getNormalizer(AuditEvent auditEvent) {
		if (null != AuditUtil.inspectType(auditEvent.getAction())
				|| _domainAuditEventsUtil.isDomainAuditEvent(auditEvent.getAction())) {
			return new BaseNormalizer();
		} else if ( ("create".equals(auditEvent.getAction())
				|| "update".equals(auditEvent.getAction()) || "delete".equals(auditEvent.getAction()))
				&& AuditUtil.inspectType(AuditUtil.extractAction(auditEvent)) != null) {
			return new CRUDNormalizer();
		} else {
			return new NonWhitelistedNormalizer();
		}
	}
}