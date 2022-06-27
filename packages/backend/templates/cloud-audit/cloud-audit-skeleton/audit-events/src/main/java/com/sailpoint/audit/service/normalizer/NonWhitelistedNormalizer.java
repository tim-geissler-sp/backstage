/*
 * Copyright (c) 2021 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service.normalizer;

import com.sailpoint.atlas.search.model.event.Event;
import sailpoint.object.AuditEvent;

import java.util.Collections;

public class NonWhitelistedNormalizer extends BaseNormalizer implements Normalizer {

    public static final String UNCLASSIFIED_TYPE = "UNCLASSIFIED";
    @Override
    public Event normalize(AuditEvent auditEvent, String eventType) {
        Event event = super.normalize(auditEvent, eventType);
        event.setType(UNCLASSIFIED_TYPE);
        event.setObjects(Collections.emptyList());
        event.setOperation(null);
        event.setStatus(null);
        event.setTechnicalName(auditEvent.getAction());
        event.setName(null);

        return event;
    }
}
