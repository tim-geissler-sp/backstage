/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service.normalizer;

import com.sailpoint.atlas.search.model.event.Event;
import sailpoint.object.AuditEvent;

public interface Normalizer {

	Event normalize(AuditEvent auditEvent, String eventType);
}
