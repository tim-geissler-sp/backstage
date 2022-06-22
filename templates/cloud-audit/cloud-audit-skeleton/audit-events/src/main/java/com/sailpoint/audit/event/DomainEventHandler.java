/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.event;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.search.model.event.Event;
import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.audit.event.model.EventCatalog;
import com.sailpoint.audit.event.model.EventDescriptor;
import com.sailpoint.audit.event.normalizer.NormalizerFactory;
import com.sailpoint.audit.service.AuditEventService;
import com.sailpoint.audit.service.util.AuditUtil;
import com.sailpoint.audit.verification.AuditVerificationRequest;
import com.sailpoint.audit.verification.AuditVerificationService;
import com.sailpoint.iris.server.EventHandler;
import com.sailpoint.iris.server.EventHandlerContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.producer.RecordMetadata;
import sailpoint.object.AuditEvent;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DomainEventHandler implements EventHandler {

	private static final Log _log = LogFactory.getLog(DomainEventHandler.class);

	private static final List<AuditVerificationRequest.VerificationTarget> VERIFICATION_TARGETS = Arrays.asList(AuditVerificationRequest.VerificationTarget.S3, AuditVerificationRequest.VerificationTarget.SEARCH);

	private final EventCatalog _eventCatalog;

	private final NormalizerFactory _normalizerFactory;

	private final AuditEventService _auditEventService;

	private final AuditUtil _util;

	private final AuditVerificationService _verificationService;

	@Override
	public void handleEvent(EventHandlerContext context) {
		try {
			submitVerification(context);
			processEvent(new EventContext(context));
		} catch (Exception e) {
			_log.error("Failed to process AUDIT_EVENT from kafka", e);
			throw e;
		}
	}

	/**
	 * Submit audit verification message
	 *
	 * @param context event to source audit event from
	 */
	private void submitVerification(final EventHandlerContext context) {
		final RequestContext requestContext = RequestContext.ensureGet();
		final String tenantId = requestContext.getTenantId().orElseThrow(() -> new RuntimeException("No tenantId found in RequestContext"));
		final String org = requestContext.ensureOrg();
		final String pod = requestContext.ensurePod();
		Instant timestamp = Optional.ofNullable(context.getEvent().getTimestamp()).orElseGet(() -> {
			// This should not be null but just being defensive for the time being
			_log.warn("Null timestamp in event: " + context.getEvent().getId());
			return OffsetDateTime.now();
		}).toInstant();
		final AuditVerificationRequest verificationRequest = AuditVerificationRequest.builder()
			.id(context.getEvent().getId())
			.tenantId(tenantId)
			.pod(pod)
			.org(org)
			.created(Date.from(timestamp))
			.verifyIn(VERIFICATION_TARGETS)
			.build();

		_verificationService.submitForVerification(verificationRequest);
	}

	private void processEvent(EventContext context) {

		Optional<EventDescriptor> eventDescriptor = _eventCatalog.get(context.getIdnTopic(), context.getEventType());

		if (!eventDescriptor.isPresent()) {

			_log.error("No EventDescriptor found in EventCatalog for context -> " + context);
			return;
		}

		publishEvent(context, eventDescriptor.get());
	}

	private void publishEvent(EventContext context, EventDescriptor eventDescriptor) {

		Event event = _normalizerFactory.get(eventDescriptor.getNormalizer()).normalize(context, eventDescriptor);

		AuditEvent auditEvent = _auditEventService.storeAuditEvent(event);
		RecordMetadata recordMetadata = null;
		if (auditEvent != null) {
			event.setId(auditEvent.getId());

			recordMetadata = _util.publishAuditEvent(event, true);
		}
		long offset = recordMetadata != null ? recordMetadata.offset() : 0L;
		_log.info("Publishing EDA Event:" +
			"\n context -> " + context + "" +
			"\n event -> " + JsonUtils.toJsonExcludeNull(event) +
			"\n published offset: " + offset);
	}
}
