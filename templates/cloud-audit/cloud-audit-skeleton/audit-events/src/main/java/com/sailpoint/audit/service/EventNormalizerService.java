/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.sailpoint.atlas.search.model.event.Event;
import com.sailpoint.audit.service.mapping.DomainAuditEventsUtil;
import com.sailpoint.audit.service.normalizer.Normalizer;
import com.sailpoint.audit.service.normalizer.NormalizerFactory;
import com.sailpoint.audit.service.util.AuditUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.CloseableThreadContext;
import sailpoint.object.AuditEvent;

public class EventNormalizerService {

	static Log log = LogFactory.getLog(EventNormalizerService.class);

	@Inject
	ObjectMapper _mapper;

	@Inject
	NormalizerFactory _normalizerFactory;

	@Inject
	DomainAuditEventsUtil _domainEventActions;

	public Event normalize(AuditEvent auditEvent) {
		return normalize(auditEvent, false);
	}

	/**
	 * Check for type and uses its defined normalizer.
	 *
	 * @param auditEvent
	 * @return
	 */
	public Event normalize(AuditEvent auditEvent, boolean isBulkSync) {
		Normalizer normalizer = _normalizerFactory.getNormalizer(auditEvent);

		String type = AuditUtil.getLegacyType(auditEvent) != null ?
				AuditUtil.getLegacyType(auditEvent) : _domainEventActions.getDomainType(auditEvent.getAction());
		Event event = normalizer.normalize(auditEvent, type);

		if (!isBulkSync) {
			logAudit(event);
		}

		return event;
	}

	/**
	 * Log the auditEvent
	 *
	 * @param auditEvent
	 */
	private void logAudit(Event auditEvent) {
		if (auditEvent != null) {
			try (final CloseableThreadContext.Instance logCtc = CloseableThreadContext
					.put("auditAction", auditEvent.getAction())) {
				ObjectNode auditNode = _mapper.createObjectNode();
				auditNode.put("technicalName", auditEvent.getTechnicalName());
				auditNode.put("target", String.valueOf(auditEvent.getTarget()));
				auditNode.put("actor", String.valueOf(auditEvent.getActor()));
				auditNode.put("stack", auditEvent.getStack());
				auditNode.put("id", auditEvent.getId());
				auditNode.put("type", auditEvent.getType());
				auditNode.put("trackingNumber", auditEvent.getTrackingNumber());

				auditNode.put("message", "Audit Event sent to search: " + auditEvent.getTechnicalName() + " - "
						+ auditEvent.getType() + " - " + auditEvent.getActor() + " - " + auditEvent.getTarget());

				log.info(auditNode.toString());
			} catch (Exception e) {
				log.error("Failed to log audit event ", e);
				throw e;
			}
		}
	}
}
