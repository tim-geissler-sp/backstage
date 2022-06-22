/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service.normalizer;

import com.sailpoint.atlas.search.model.event.Event;
import com.sailpoint.atlas.search.model.event.NameType;
import sailpoint.object.AuditEvent;

public class CRUDNormalizer extends BaseNormalizer implements Normalizer {

	@Override
	public Event normalize(AuditEvent auditEvent, String type) {

		Event event = super.normalize(auditEvent, type);

		if (auditEvent.getTarget() != null && auditEvent.getTarget().split(":").length >= 2) {

			event.setTarget(NameType.of(auditEvent.getTarget().substring(auditEvent.getTarget().indexOf(":")+1).trim()));
		}

		return event;
	}
}
