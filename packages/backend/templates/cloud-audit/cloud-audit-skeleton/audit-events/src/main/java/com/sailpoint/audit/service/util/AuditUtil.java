/*
 *
 *  * Copyright (c) 2019.  SailPoint Technologies, Inc. All rights reserved.
 *
 */

package com.sailpoint.audit.service.util;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.google.common.hash.Hashing;
import com.google.inject.Inject;
import com.mysql.jdbc.StringUtils;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.search.model.event.Event;
import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.atlas.service.RemoteFileService;
import com.sailpoint.audit.service.AuditKafkaEventType;
import com.sailpoint.audit.service.mapping.AuditEventTypeToAction;
import com.sailpoint.audit.service.mapping.DomainAuditEventsUtil;
import com.sailpoint.audit.service.model.AuditEventDTO;
import com.sailpoint.audit.service.model.BulkUploadDTO;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.metrics.annotation.ExceptionMetered;
import com.sailpoint.seaspray.JsonUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.producer.RecordMetadata;
import sailpoint.object.Attributes;
import sailpoint.object.AuditEvent;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.sailpoint.audit.AuditEventConstants.CRUD_TYPE_ACTIONS_MAP;
import static com.sailpoint.audit.service.AuditEventService.FIREHOSE_RECORD_LIMIT_KB;
import static com.sailpoint.audit.service.normalizer.BaseNormalizer.INFO;
import static com.sailpoint.audit.service.normalizer.BaseNormalizer.SOURCE_ID;
import static com.sailpoint.audit.service.normalizer.BaseNormalizer.SOURCE_NAME;

public class AuditUtil {
	private static final Log _log = LogFactory.getLog(AuditUtil.class);

	public static final String PARQUET_DATA_S3_PREFIX = "parquet";
	private static final String AUDIT_TABLE_PREFIX = "_audit_data";

	@Inject
	EventService _eventService;

	@Inject
	RemoteFileService _remoteFileService;

	@Inject
	DomainAuditEventsUtil _domainEventActions;

	/**
	 * Utility function copied over from AuditEvent class.
	 * Copied since the plan is to not depend on CIS AuditEvent class
 	 * @param src
	 * @param max
	 * @return
	 */
	public static String limit(String src, int max) {
		String limited = src;
		if (src != null && src.length() > max) {
			String suffix = "...";
			int trim = max - suffix.length() - 4;
			if (trim > 0) {
				limited = src.substring(0, trim) + suffix;
			}
		}

		return limited;
	}

	public static LocalDate toDate(Map<String, Object> arguments, String fieldName) {

		String date = (String) arguments.get(fieldName);

		try {

			if (date != null) {

				return LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(date));
			}

		} catch (DateTimeParseException e) {

		}

		_log.error("invalid " + fieldName + " argument");

		return null;
	}

	public static Event checkEventSizeAndFieldLimits(Event event) {
		//Not mentioning the charSet since this is the same version of getBytes() getting used in KinesisFirehoseService
		//method: sendToFirehose, Line 42 : Record record = new Record().withData(ByteBuffer.wrap(json.getBytes()));
		int length = JsonUtils.toJson(event).getBytes().length;
		//950kb
		if (length <= FIREHOSE_RECORD_LIMIT_KB) {
			return event;
		} else {
			_log.info("Audit event attributes will be trimmed due to abnormal size");
			Map<String, Object> attributes = event.getAttributes();
			event.setAttributes((trimLength(attributes)));
		}

		//Check the length again
		length = JsonUtils.toJson(event).getBytes().length;
		if (length > FIREHOSE_RECORD_LIMIT_KB) {
			event.setAttributes(shrinkAttributes(event.getAttributes()));
		}
		return event;
	}

	private static Map<String, Object> trimLength(Map<String, Object> attributes) {
		if (attributes != null) {
			for (String s : attributes.keySet()) {
				if (attributes.get(s) instanceof String) {
					attributes.put(s, AuditUtil.limit((String) attributes.get(s), 1000));
				}
			}
			attributes = trimErrors(attributes);
		}
		return attributes;
	}

	private static Map<String, Object> trimErrors(Map<String, Object> attributes) {
		if (attributes.containsKey("errors")) {
			List<String> newErrors = new ArrayList<>();
			if (attributes.get("errors") instanceof List) {
				List<String> errors = (List) attributes.get("errors");
				for (String error : errors) {
					newErrors.add(AuditUtil.limit(error, 1000));
				}
				attributes.put("errors", newErrors);
			} else if (attributes.get("errors") instanceof String) {
				attributes.put("errors", AuditUtil.limit((String) attributes.get("errors"), 1000));
			}
		}
		return attributes;
	}

	private static Map<String, Object> shrinkAttributes(Map<String, Object> attributes) {
		Map<String, Object> newAttributes = new HashMap<>();
		if (attributes != null && attributes.containsKey(SOURCE_ID)) {
			newAttributes.put(SOURCE_ID, AuditUtil.limit((String) attributes.get(SOURCE_ID), 1000));
		}
		if (attributes != null && attributes.containsKey(SOURCE_NAME)) {
			newAttributes.put(SOURCE_NAME, AuditUtil.limit((String) attributes.get(SOURCE_NAME), 1000));
		}
		if (attributes != null && attributes.containsKey(INFO)) {
			newAttributes.put(INFO, AuditUtil.limit((String) attributes.get(INFO), 1000));
		}
		return newAttributes;
	}

	public static Map<String, Object> convertToStringValues(Map<String, Object> attributes) {

		if (attributes != null) {
			attributes.forEach((k, v) -> {
				if (!(v instanceof String)) {
					attributes.put(k, JsonUtil.toJson(v));
				}
			});
		}

		return attributes;
	}

	/**
	 * helper function - transforms DTO to auditevent ( SSO,CC events) for persistence
	 *
	 * @param bulkUploadDTO
	 * @return
	 */
	public static AuditEvent transformBulkEvent(BulkUploadDTO bulkUploadDTO) {
		AuditEvent ae = transform(bulkUploadDTO);
		ae.setId(bulkUploadDTO.getId());
		return ae;
	}

	/**
	 * helper function - transforms DTO to auditevent ( SSO,CC events) for persistence
	 *
	 * @param auditEventDTO
	 * @return
	 */
	public static AuditEvent transform(AuditEventDTO auditEventDTO) {
		AuditEvent ae = new AuditEvent(auditEventDTO.getSource(), auditEventDTO.getAction(), auditEventDTO.getTarget());
		String stack = auditEventDTO.getStack();
		String application = auditEventDTO.getApplication();
		if (!StringUtils.isNullOrEmpty(stack)) {
			if (!StringUtils.isNullOrEmpty(application) && !application.startsWith("[")) {
				auditEventDTO.setApplication(String.format("[%s] %s", stack, application));
			}
		}
		ae.setApplication(auditEventDTO.getApplication());
		ae.setInstance(auditEventDTO.getType());
		ae.setAccountName(auditEventDTO.getAccountName());
		ae.setInterface(auditEventDTO.getInterface());
		ae.setAttributeName(auditEventDTO.getAttributeName());
		ae.setAttributeValue(auditEventDTO.getAttributeValue());
		ae.setString1(auditEventDTO.getHostname());
		ae.setString2(auditEventDTO.getIpaddr());
		ae.setString3(auditEventDTO.getContextId());
		ae.setString4(auditEventDTO.getInfo());
		ae.setTrackingId(auditEventDTO.getRequestId());
		ae.setAttributes(new Attributes<>(auditEventDTO.getAttributes()));
		ae.setCreated(Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(auditEventDTO.getCreated()))));
		return ae;
	}

	/**
	 * This function will look up the type of this auditevent and return the
	 * string type(s) classifier for using the correct normalizer.
	 *
	 * @param action
	 * @return Set of audit event types
	 */
	public static String inspectType(String action) {
		return AuditEventTypeToAction.ACTION_TO_TYPE_MAPPING.get(action);
	}

	@ExceptionMetered
	public RecordMetadata publishAuditEvent(Event event, boolean isWhitelisted) {
		try {
		String type = isWhitelisted ? AuditKafkaEventType.AUDIT_WHITELISTED.toString() :
				AuditKafkaEventType.AUDIT_NONWHITELISTED.toString();

		Future<?> future = _eventService.publishAsync(IdnTopic.AUDIT, EventBuilder
			.withTypeAndContentJson(type, JsonUtils.toJsonExcludeNull(event))
			.build());

			return (RecordMetadata) future.get();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("timeout waiting for event to publish", ex);
		} catch (ExecutionException ex) {
			throw new IllegalStateException("error publishing event", ex.getCause());
		}

	}

	public static String getOrgAuditAthenaTableName(String orgName) {
		//Only alpha numeric and underscore (_) characters are allowed as table name.
		orgName = orgName.replaceAll("\\W", "_").toLowerCase();
		return orgName + AUDIT_TABLE_PREFIX;
	}

	public Set<String> getS3AuditPartitionPaths(String s3Bucket, String startPrefix, String containsString) {
		_log.debug(String.format("Getting list of s3 paths from %s bucket with startPrefix: %s, containsString:%s",
				s3Bucket, startPrefix, containsString));
		List<String> s3Paths = _remoteFileService.listFiles(s3Bucket, startPrefix);
		Set<String> resultS3Paths = new HashSet<String>();

		for (String s3Path : s3Paths) {
			if (s3Path.contains(containsString)) {
				resultS3Paths.add("s3://" + s3Bucket + "/" + s3Path.substring(0, (s3Path.indexOf(containsString) + containsString.length() + 1)));
			}
		}

		return resultS3Paths;
	}

	/**
	 * This block is to handle the generic audit events.Action contains one of create, update or delete
	 * @param auditEvent
	 */
	public static String extractAction(AuditEvent auditEvent) {
		String domainObject;
		final String action = auditEvent.getAction();

		if (("create".equals(action) || "update".equals(action) || "delete".equals(action)) &&
				auditEvent.getTarget() != null && auditEvent.getTarget().split(":").length >= 2) {
			domainObject = auditEvent.getTarget().split(":")[0];

			if (CRUD_TYPE_ACTIONS_MAP.containsKey(action+domainObject)) {
				return CRUD_TYPE_ACTIONS_MAP.get(action + domainObject);
			}
		}

		return action;
	}

	public static String generateHash(AuditEvent auditEvent) {
		return Hashing.sha256().hashString(com.sailpoint.utilities.JsonUtil.toJson(auditEvent), StandardCharsets.UTF_8).toString();
	}

	public boolean isAlwaysAllowAudit(AuditEvent auditEvent) {
		return getLegacyType(auditEvent) != null
				|| isDomainAuditEvent(auditEvent.getAction());
	}

	public static String getLegacyType(AuditEvent auditEvent) {
		return AuditUtil.inspectType(AuditUtil.extractAction(auditEvent));
	}

	/**
	 * Returns true if an audit event is a domain audit event
	 * @param action
	 * @return
	 */
	public boolean isDomainAuditEvent(String action) {
		return _domainEventActions.isDomainAuditEvent(action);
	}

	public static String getCurrentRegion(String defaultRegion) {
		Region region = Regions.getCurrentRegion();
		if (region == null) {
			_log.warn("Admin current region is not set, falling back to config");
			region = Region.getRegion(Regions.fromName(defaultRegion));
		}
		return region.getName();
	}

}
