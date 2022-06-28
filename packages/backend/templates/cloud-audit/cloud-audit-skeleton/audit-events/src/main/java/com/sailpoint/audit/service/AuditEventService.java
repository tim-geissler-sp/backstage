/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.sailpoint.atlas.OrgDataCache;
import com.sailpoint.atlas.OrgDataProvider;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.messaging.client.MessageHeaders;
import com.sailpoint.atlas.messaging.server.MessageHandlerContext;
import com.sailpoint.atlas.search.model.event.Event;
import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.atlas.service.AtomicMessageService;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.audit.message.AuditEventPayload;
import com.sailpoint.audit.persistence.S3AuditEventEnvelope;
import com.sailpoint.audit.persistence.S3PersistenceManager;
import com.sailpoint.audit.service.model.AuditEventDTO;
import com.sailpoint.audit.service.model.AuditTimeAndIdsRecord;
import com.sailpoint.audit.service.util.AuditUtil;
import com.sailpoint.audit.service.util.StackExtractor;
import com.sailpoint.audit.verification.AuditVerificationRequest;
import com.sailpoint.audit.verification.AuditVerificationService;
import com.sailpoint.mantis.core.service.CrudService;
import com.sailpoint.mantis.core.service.XmlService;
import com.sailpoint.metrics.MetricsUtil;
import com.sailpoint.utilities.StringUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.util.Strings;
import sailpoint.object.Attributes;
import sailpoint.object.AuditEvent;
import sailpoint.object.Configuration;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static com.sailpoint.audit.service.normalizer.BaseNormalizer.INFO;
import static com.sailpoint.audit.service.normalizer.BaseNormalizer.SOURCE_ID;
import static com.sailpoint.audit.service.normalizer.BaseNormalizer.SOURCE_NAME;

/**
 * Created by mark.boyle on 9/20/17.
 */
@Singleton
public class AuditEventService {

	static Log log = LogFactory.getLog(AuditEventService.class);

	@VisibleForTesting
	static final String FIREHOSE_ATTRIBUTE_NAME = "auditFirehose";

	@VisibleForTesting
	public static final int FIREHOSE_RECORD_LIMIT_KB = 990 * 1024;

	public static final String METRIC_TO_CIS_WRITTEN = "com.sailpoint.aer.persistence.written.cis";
	public static final String METRIC_TO_S3_WRITTEN  = "com.sailpoint.aer.persistence.written.s3";

	@Inject
	CrudService _crudService;

	@Inject
	XmlService _xmlService;

	@Inject
	EventNormalizerService _eventNormalizerService;

	@Inject
	Provider<AtomicMessageService> _atomicMessageService;

	@Inject
	FeatureFlagService _featureFlagService;

	@Inject
	Provider<EventService> _eventService;

	@Inject
	ObjectMapper _mapper;

	@Inject
	OrgDataProvider _orgDataProvider;

	@Inject
	OrgDataCache _orgDataCache;

	@Inject
	DeletedOrgsCacheService _deletedOrgsCache;

	@Inject
	AuditUtil _util;

	@Inject
	AuditVerificationService _auditVerificationService;

	/**
	 * Increments a given metric with the local RequestContext taken into account.
	 *
	 * @param metricName
	 */
	public void incrementMetricForContext(String metricName) {

		RequestContext requestContext = RequestContext.ensureGet();

		HashMap<String,String> metricLabels = new HashMap<>();
		metricLabels.put("org", requestContext.getOrg());
		metricLabels.put("pod", requestContext.getPod());
		metricLabels.put("tenantId", requestContext.getTenantId().get());

		MetricsUtil.getCounter(metricName, metricLabels).inc();
	}

	/**
	 * Handle the AUDIT_EVENT message.
	 *
	 * @param payload
	 * @param context
	 */
	public void processAuditMessage(AuditEventPayload payload, MessageHandlerContext context) {
		AuditEvent auditEvent = null;
		AuditEvent auditEventDuplicate = null;
		AuditEventDTO auditEventDTO = null;

		if (payload.hasXml()) {
			auditEvent = _xmlService.parse(payload.getAuditEventXml());
			auditEventDuplicate = _xmlService.parse(payload.getAuditEventXml());
			auditEventDTO = new AuditEventDTO(auditEvent);
		} else {
			auditEventDTO = payload.getAuditEventJson();
			auditEvent = AuditUtil.transform(auditEventDTO);
			auditEventDuplicate = AuditUtil.transform(auditEventDTO);
		}

		Date d = auditEvent.getCreated();
		if(d == null){
			d = getCreatedDate(context.getMessage().getHeader(MessageHeaders.TIMESTAMP));
		}
		auditEvent.setCreated(d);
		auditEventDuplicate.setCreated(d);
		// If no id initialize id to derivable value
		String id = auditEvent.getId();
		if(StringUtil.isNullOrEmpty(id)) {
			id = AuditUtil.generateHash(auditEvent);
			auditEvent.setId(id);
		}
		auditEventDuplicate.setId(id);

		submitVerification(auditEvent);

		boolean iWillStoreThis = payload.isUseAerStorage();
		boolean publishToKafka = true;
		boolean sentToSearch = false;

		if (isOrgDeleteInProcess()) {
			log.info("Org already marked deleted. Not processing further");
			return;	// Don't store audit event
		}

		if (iWillStoreThis) {
			auditEvent = storeAuditEvent(auditEvent);
			if (auditEvent != null) {
				auditEventDuplicate.setId(auditEvent.getId());
			} else {
				publishToKafka = false;
				auditEvent = auditEventDuplicate;
			}
		}

		RecordMetadata recordMetadata = null;
		if (publishToKafka && iWillStoreThis) {
			recordMetadata = normalizeAndEmit(auditEventDuplicate);
			sentToSearch = recordMetadata != null;
		}

		logIt(auditEvent, auditEventDTO, iWillStoreThis, sentToSearch, recordMetadata);
	}

	/**
	 * If there was a date in the message header use that otherwise fall back to now.
	 *
	 * @param maybeDateTime Value from message maybeDateTime
	 * @return The created date
	 */
	private Date getCreatedDate(Optional<String> maybeDateTime) {
		// Just being cautious here, the header should always have a timestamp
		if(!maybeDateTime.isPresent() || StringUtil.isNullOrEmpty(maybeDateTime.get())) {
			return new Date();
		}
		String stringDate = maybeDateTime.get();
		OffsetDateTime dateTime = OffsetDateTime.parse(stringDate);
		return Date.from(dateTime.toInstant());
	}

	/**
	 * Submit audit verification message
	 * @param auditEvent Event to source audit event from
	 */
	private void submitVerification(final AuditEvent auditEvent) {
		final RequestContext requestContext = RequestContext.ensureGet();
		final String tenantId = requestContext.getTenantId().orElseThrow(() -> new RuntimeException("No tenantId found in RequestContext"));
		final String pod = requestContext.ensurePod();
		final String org = requestContext.ensureOrg();
		List<AuditVerificationRequest.VerificationTarget> verificationTargets = new ArrayList<>();
		verificationTargets.add(AuditVerificationRequest.VerificationTarget.S3);
		if(_util.isAlwaysAllowAudit(auditEvent)) {
			verificationTargets.add(AuditVerificationRequest.VerificationTarget.SEARCH);
		}
		final AuditVerificationRequest verificationRequest = AuditVerificationRequest.builder()
			.id(auditEvent.getId())
			.tenantId(tenantId)
			.pod(pod)
			.org(org)
			.verifyIn(verificationTargets)
			.created(auditEvent.getCreated())
			.build();

		_auditVerificationService.submitForVerification(verificationRequest);
	}

	public AuditEvent storeAuditEvent(Event event) {

		AuditEvent auditEvent = new AuditEvent();

		auditEvent.setId(event.getId());
		auditEvent.setCreated(event.getCreated());
		auditEvent.setAction(event.getAction());
		auditEvent.setSource(event.getActor() != null ? event.getActor().getName() : null);
		auditEvent.setTarget(event.getTarget() != null ? event.getTarget().getName() : null);
		auditEvent.setAttributes(new Attributes<>(event.getAttributes()));

		if (auditEvent.getAttribute(SOURCE_NAME) != null) {

			auditEvent.setApplication(auditEvent.getAttribute(SOURCE_NAME) + (auditEvent.getAttribute(SOURCE_ID) != null ? " [" + auditEvent.getAttribute(SOURCE_ID) + "]" : ""));
		}

		auditEvent.setInstance(event.getType());
		auditEvent.setTrackingId(event.getTrackingNumber());
		auditEvent.setString2(event.getIpAddress());
		auditEvent.setString3(event.getDetails());
		auditEvent.setString4(auditEvent.getAttribute(INFO) != null ? auditEvent.getAttribute(INFO).toString() : null);

		return storeAuditEvent(auditEvent);
	}

	/**
	 * Persist the given Audit Event to CIS MySQL, S3 or both.  Has fail-safes to ensure that the
	 * record gets written to at least one of the locations in the event of mis-configuration.
	 *
	 * Has fail-safes to ensure the same AuditEvent.id property is passed to both CIS and S3.
	 *
	 * @param auditEvent
	 * @return
	 */
	protected AuditEvent saveToDurablePersistence(AuditEvent auditEvent) {

		AuditEvent savedAuditEvent = null;

		boolean toCis = _featureFlagService.getBoolean(FeatureFlags.PLTDP_PERSIST_TO_CIS_MYSQL,  true);
		boolean toS3  = _featureFlagService.getBoolean(FeatureFlags.PLTDP_PERSIST_TO_S3_BY_TIME, false);

		// Make sure we are writing audit events at least some where. Do not allow them to be dropped via mis-config.
		if ( !toCis && !toS3 ) {
			String orgName = RequestContext.ensureGet().getOrg();
			log.error("Org '" + orgName + "' not FF configured to write auditing anywhere!  Using CIS fail-safe.");
			toCis = true;
			toS3 = false;
		}

		if (toCis) {
			savedAuditEvent = _crudService.save(auditEvent);
			incrementMetricForContext(METRIC_TO_CIS_WRITTEN);
			if (toS3) {
				auditEvent = savedAuditEvent; // Pass what CIS saved (id, createdDate) to S3.
			}
		}

		// Older CIS/IIQ code assigns the 'id' and 'created' time stamps when .save() gets invoked. That is why the
		// reference was returned from .save(), so the calling code could get the ID and committted time stamp.  When
		// skipping CIS and writing only to S3 then we need to emulate this behavior in our S3 persistence layer.  We
		// verify we have a UUID for the Audit Event before saving it.
		if (Strings.isBlank(auditEvent.getId())) {
			auditEvent.setId(UUID.randomUUID().toString().replaceAll("-", ""));
		}
		if (null == auditEvent.getCreated()) {
			auditEvent.setCreated(new Date());
		}

		S3AuditEventEnvelope s3AuditEventEnvelope = null;
		if (toS3) {
			s3AuditEventEnvelope = S3PersistenceManager.saveAuditEvent(auditEvent);
			incrementMetricForContext(METRIC_TO_S3_WRITTEN);
			if (!toCis) {
				savedAuditEvent = s3AuditEventEnvelope.getAuditEvent();
			}
		}

		// This was verbose for roll-out.  Now that we're cut over to S3, only log when persisting to CIS.
		if (toCis) {
			log.info("Saved AuditEvent: [" + savedAuditEvent.getId() + "] toCis:" + toCis + " toS3:" + toS3 +
					" path:" +
					(toS3 ? S3PersistenceManager.BUCKET_BY_TIME + ":" + s3AuditEventEnvelope.getS3ObjectKey() : ""));
		}

		return savedAuditEvent;
	}

	/**
	 * Audit event persistence
	 * CIS endpoint
	 *
	 * @param auditEvent
	 */
	private AuditEvent storeAuditEvent(AuditEvent auditEvent) {
		AuditEvent savedAuditEvent = null;
		try {
			//We are transforming the auditevent object again, as this method modifies the auditEvent
			//object before getting stored in ES
			Map<String, String> parsedFields = StackExtractor.getStack(auditEvent.getApplication());
			StringBuilder appName = new StringBuilder();
			appName.append(parsedFields.get("application"));
			if( parsedFields.containsKey("sourceId") ){
				appName.append(" [" + parsedFields.get("sourceId") + "]");
			}
			auditEvent.setApplication(appName.toString());
			auditEvent.checkLimits();
			auditEvent.setSource(AuditEvent.limit(auditEvent.getSource(), 128));
			auditEvent.setApplication(AuditEvent.limit(auditEvent.getApplication(), 128));

			// Persist in either CIS, S3 or both, depending on FF settings.
			savedAuditEvent = saveToDurablePersistence(auditEvent);

		} catch (Exception e) {
			log.error("Failed to commit AuditEvent ", e);
			//Do not send the audit event to Kafka if the table does not exist.
			//This usually happens in Bermuda when an org is deleted and cache is stale.
			String org = RequestContext.ensureGet().getOrg();

			if (Throwables.getRootCause(e).toString().matches(".*Table.*doesn't exist.*")) {

				_orgDataCache.evict(org);
			}

			if (!_orgDataProvider.find(org).isPresent()) {

				return null;	// Don't publish to Kafka
			}

			throw e;
		}

		return savedAuditEvent;
	}

	/**
	 * Takes an Audit Event and normalizes it to an Event for Search to consume.
	 *
	 * @param auditEvent
	 * @return boolean sentToSearch
	 */
	public RecordMetadata normalizeAndEmit(AuditEvent auditEvent) {
		boolean isAlwaysAllowedAudit = _util.isAlwaysAllowAudit(auditEvent);
		Event event = _eventNormalizerService.normalize(auditEvent);

		if (event == null) {
			return null;
		} else if (isOrgDeleteInProcess()) {
			log.info("Org already marked deleted. Not processing further");
			return null;	// Don't publish to Kafka
		}

		return _util.publishAuditEvent(event, isAlwaysAllowedAudit);
	}

	private boolean isOrgDeleteInProcess() {
		final Optional<String> tenantId = RequestContext.ensureGet().getTenantId();
		if (tenantId.isPresent() && _deletedOrgsCache.isOrgDeleted(tenantId.get())) {
			return true;
		}
		return false;
	}

	public void addCheckpoint(long created, String id, String checkpoint) {
		//Storing the timestamp and Id of the first audit event through firehose
		//This part will be deleted once the bulk sync is completed
		Configuration config = _crudService.findByName(Configuration.class, "CloudConfiguration").get();
		AuditTimeAndIdsRecord firehoseStatus = getCheckpoint(config, checkpoint);

		if (firehoseStatus == null || firehoseStatus.getTimestamp() == created) {

			_crudService.withTransactionLock(Configuration.class, config.getId(),
					saveFirehoseStatus(created, id, checkpoint));
		}
	}

	public void updateCheckpoint(long created, String id, String checkpoint) {
		Configuration config = _crudService.findByName(Configuration.class, "CloudConfiguration").get();

		_crudService.withTransactionLock(Configuration.class, config.getId(),
				writeFirehoseStatus(created, id, checkpoint));

	}

	/*
	Method to write/over-write firehose status
	 */
	@VisibleForTesting
	Consumer<Configuration> writeFirehoseStatus(long created, String id, String checkpoint) {
		return config -> {

			AuditTimeAndIdsRecord firehoseStatus = new AuditTimeAndIdsRecord();

			if (id == null) {
				firehoseStatus.setIds(Collections.emptyList());
			} else {
				firehoseStatus.setIds(Arrays.asList(id));
			}
			firehoseStatus.setTimestamp(created);

			config.put(checkpoint, JsonUtils.toJson(firehoseStatus));

			_crudService.save(config);
		};
	}

	private AuditTimeAndIdsRecord getCheckpoint(Configuration config, String checkpoint) {
		String firehoseStatus = config.getString(checkpoint);

		return firehoseStatus != null ? JsonUtils.parse(AuditTimeAndIdsRecord.class, firehoseStatus) : null;
	}

	@VisibleForTesting
	Consumer<Configuration> saveFirehoseStatus(long created, String id, String checkpoint) {

		return config -> {

			AuditTimeAndIdsRecord firehoseStatus = getCheckpoint(config, checkpoint);

			if (firehoseStatus == null) {

				firehoseStatus = AuditTimeAndIdsRecord.of(created, id);

			} else if (created > firehoseStatus.getTimestamp()) {

				return;

			} else {

				firehoseStatus.addId(id);
			}

			config.put(checkpoint, JsonUtils.toJson(firehoseStatus));

			_crudService.save(config);
		};
	}

	/**
	 * Log the auditEvent
	 *  @param auditEvent
	 * @param auditEventDTO
	 * @param useAerStorage
	 * @param recordMetadata
	 */
	private void logIt(AuditEvent auditEvent, AuditEventDTO auditEventDTO, boolean useAerStorage, boolean sentToSearch, RecordMetadata recordMetadata) {
		try (final CloseableThreadContext.Instance logCtc = CloseableThreadContext
				.put("auditAction", auditEvent.getAction())
				.put("auditOrigin", auditEvent.getApplication())) {
			ObjectNode auditNode = _mapper.createObjectNode();
			auditNode.put("action", auditEventDTO.getAction());
			auditNode.put("target", auditEventDTO.getTarget());
			auditNode.put("source", auditEventDTO.getSource());
			auditNode.put("application", auditEventDTO.getApplication());
			auditNode.put("uuid", auditEventDTO.getUuid());
			auditNode.put("type", auditEventDTO.getType());
			auditNode.put("trackingId", auditEvent.getTrackingId());

			auditNode.put("message", "Audit Event: " + auditEvent.getAction() + " - " +
					auditEvent.getSource() + " - " + auditEvent.getTarget());

			auditNode.put("useAerStorage", useAerStorage);
			auditNode.put("sentToSearch", sentToSearch);
			auditNode.put("id", auditEvent.getId());

			long offset = recordMetadata != null ? recordMetadata.offset() : 0L;
			log.info("published offset: " + offset + " event : " + auditNode);
		} catch (Exception e) {
			log.error("Failed to log audit event ", e);
			throw e;
		}
	}
}
