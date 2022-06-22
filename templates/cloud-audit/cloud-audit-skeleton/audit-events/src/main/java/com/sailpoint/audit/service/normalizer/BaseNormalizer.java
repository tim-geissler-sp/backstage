/*
 * Copyright (c) 2018-2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.service.normalizer;

import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.search.model.event.Event;
import com.sailpoint.atlas.search.model.event.NameType;
import com.sailpoint.audit.service.mapping.SearchableEventFactory;
import com.sailpoint.audit.service.model.SearchableEvent;
import com.sailpoint.audit.service.util.AuditUtil;
import com.sailpoint.audit.service.util.StackExtractor;
import com.sailpoint.seaspray.JsonUtil;
import sailpoint.object.AuditEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static com.sailpoint.atlas.search.model.FieldReferences.STACK;

/**
 * This is used during 2 places: Bulk Upload and live audit events flow. We are not losing account name informaiton
 * during live processing; possibly just during bulk processing.
 */
public class BaseNormalizer implements Normalizer {

	public static final String SOURCE_ID = "sourceId";
	public static final String SOURCE_NAME = "sourceName";
	private static final String APPLICATION = "application";
	public static final String INFO = "info";
	public static final String ACCOUNT_NAME = "accountName";
	public static final String INTERFACE = "interface";
	public static final String ATTRIBUTE_NAME = "attributeName";
	public static final String ATTRIBUTE_VALUE = "attributeValue";
	public static final String HOST_NAME = "hostName";

	@Override
	public Event normalize(AuditEvent auditEvent, String type) {

		RequestContext requestContext = RequestContext.ensureGet();
		Map<String, String> parsedFields = StackExtractor.getStack(auditEvent.getApplication());

		SearchableEvent searchableEvent = SearchableEventFactory.get(AuditUtil.extractAction(auditEvent),
				Objects.toString(type, ""));

		return Event.builder()
			.withOrg(requestContext.getOrg())
			.withPod(requestContext.getPod())
			.withCreated(auditEvent.getCreated())
			.withId(auditEvent.getId())
			.withAction(auditEvent.getAction())
			.withType(type)
			.withActor(NameType.of(auditEvent.getSource()))
			.withTarget(NameType.of(auditEvent.getTarget()))
			.withStack(parsedFields.get(STACK))
			.withTrackingNumber(auditEvent.getTrackingId())
			.withIpAddress(auditEvent.getString2())
			.withDetails(auditEvent.getString3())
			.withAttributes(getAttributes(auditEvent, parsedFields))
			.withObjects(searchableEvent.getDomainObjects())
			.withOperation(searchableEvent.getActionVerb())
			.withStatus(searchableEvent.getStatus())
			.build();
	}

	private Map<String, Object> getAttributes(AuditEvent auditEvent, Map<String, String> parsedFields) {

		Map<String, Object> attributes = new LinkedHashMap<>();

		if (auditEvent.getAttributes() != null) {
			auditEvent.getAttributes().forEach((k, v) -> {
				if (v instanceof String) {
					attributes.put(k, v);
				} else {
					attributes.put(k, JsonUtil.toJson(v));
				}
			});
		}

		// Note: All these attributges of the AuditEvent are getting pushed down into the Attirbutes XML/CLOB.
		attributes.put(SOURCE_ID, parsedFields.get(SOURCE_ID));
		attributes.put(SOURCE_NAME, parsedFields.get(APPLICATION));
		attributes.put(INFO, auditEvent.getString4());

		attributes.put(ACCOUNT_NAME, auditEvent.getAccountName());
		attributes.put(INTERFACE, auditEvent.getInterface());
		attributes.put(HOST_NAME, auditEvent.getString1());
		attributes.put(ATTRIBUTE_NAME, auditEvent.getAttributeName());
		attributes.put(ATTRIBUTE_VALUE, auditEvent.getAttributeValue());

		return attributes;
	}
}
