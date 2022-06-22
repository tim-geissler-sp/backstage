/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.event.normalizer;

import com.google.inject.Singleton;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.search.model.event.Event;
import com.sailpoint.atlas.search.model.event.NameType;
import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.atlas.search.util.MapUtils;
import com.sailpoint.audit.event.EventContext;
import com.sailpoint.audit.event.model.EventDescriptor;
import com.sailpoint.audit.service.util.AuditUtil;
import com.sailpoint.cloud.api.client.model.DtoType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.startsWith;

@Singleton
public class JsonPathNormalizer implements Normalizer {

	private static final Log _log = LogFactory.getLog(JsonPathNormalizer.class);

	private static final Configuration JSON_PATH_CONFIG = Configuration.builder()
		.options(Option.SUPPRESS_EXCEPTIONS)
		.build();

	@Override
	public Event normalize(EventContext context, EventDescriptor eventDescriptor) {

		RequestContext requestContext = RequestContext.ensureGet();
		DocumentContext documentContext = JsonPath.parse(context.getDomainEvent(), JSON_PATH_CONFIG);

		return Event.builder()
			.withOrg(requestContext.getOrg())
			.withPod(requestContext.getPod())
			.withId(context.getEventId())
			.withCreated(Date.from(context.getTimestamp().toInstant()))
			.withAction(getString(context, documentContext, eventDescriptor.getEvent().getAction()))
			.withType(getString(context, documentContext, eventDescriptor.getEvent().getType()))
			.withActor(getNameType(context, documentContext, eventDescriptor.getEvent().getActor()))
			.withTarget(getNameType(context, documentContext, eventDescriptor.getEvent().getTarget()))
			.withStack(getString(context, documentContext, eventDescriptor.getEvent().getStack()))
			.withTrackingNumber(context.getRequestId())
			.withIpAddress(getString(context, documentContext, eventDescriptor.getEvent().getIpAddress()))
			.withDetails(getString(context, documentContext, eventDescriptor.getEvent().getDetails()))
			.withAttributes(AuditUtil.convertToStringValues(
					getAttributes(context, documentContext, eventDescriptor.getEvent().getAttributes())))
			.withObjects(getObjects(context, documentContext, eventDescriptor.getEvent().getObjects()))
			.withOperation(getString(context, documentContext, eventDescriptor.getEvent().getOperation()))
			.withStatus(getString(context, documentContext, eventDescriptor.getEvent().getStatus()))
			.build();
	}

	private static NameType getNameType(EventContext context, DocumentContext documentContext, Object value) {

		if (value instanceof String) {

			value = getObject(context, documentContext, (String) value, Map.class);		// JsonPath must evaluate to a Map or result will be ignored ...
		}

		if (value instanceof Map) {

			return NameType
				.of(toString(context, documentContext, ((Map) value).get("name")))
				.withType(toType(context, documentContext, ((Map) value).get("type")));
		}

		return null;
	}

	private static DtoType toType(EventContext context, DocumentContext documentContext, Object value) {

		String type = toString(context, documentContext, value);

		try {

			if (type != null) {

				return DtoType.valueOf(type);
			}

		} catch (IllegalArgumentException e) {

			_log.warn("DtoType value was expected: " + value + ", context -> " + context);
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> getAttributes(EventContext context, DocumentContext documentContext, Object value) {

		if (value instanceof String) {

			return getObject(context, documentContext, (String) value, Map.class);		// JsonPath must evaluate to a Map or result will be ignored ...
		}

		if (value instanceof Map) {

			return toMap(context, documentContext, value);
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> toMap(EventContext context, DocumentContext documentContext, Object value) {

		return ((Map<?, ?>) value).entrySet().stream()
			.filter(entry -> toAttribute(context, documentContext, entry.getValue()) != null)
			.map(entry -> (Map.Entry<String, Object>) entry)
			.collect(MapUtils.toMap(Map.Entry::getKey, entry -> toAttribute(context, documentContext, entry.getValue())));
	}

	private static Object toAttribute(EventContext context, DocumentContext documentContext, Object value) {

		if (value instanceof String) {

			return toObject(context, documentContext, (String) value);
		}

		if (value instanceof Map) {

			return toMap(context, documentContext, value);
		}

		if (value instanceof List) {

			return toList(value, object -> toAttribute(context, documentContext, object));
		}

		return value;
	}

	private static Object toObject(EventContext context, DocumentContext documentContext, String value) {

		return isJsonPath(value) ? getObject(context, documentContext, value, Object.class) : value;
	}

	private static List<String> getObjects(EventContext context, DocumentContext documentContext, Object value) {

		if (value instanceof String) {

			value = getObject(context, documentContext, (String) value, List.class);		// JsonPath must evaluate to a List or result will be ignored ...
		}

		if (value instanceof List) {

			return toList(value, object -> toString(context, documentContext, object));
		}

		return Collections.emptyList();
	}

	private static <T> List<T> toList(Object value, Function<Object, T> mapper) {

		return ((List<?>) value).stream()
			.map(mapper)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	private static String toString(EventContext context, DocumentContext documentContext, Object value) {

		if (value != null) {

			if (value instanceof String) {

				return getString(context, documentContext, (String) value);
			}

			_log.warn("String value was expected: " + value + ", context -> " + context);
		}

		return null;
	}

	private static String getString(EventContext context, DocumentContext documentContext, String value) {

		return isJsonPath(value) ? getObject(context, documentContext, value, String.class) : value;
	}

	private static <T> T getObject(EventContext context, DocumentContext documentContext, String value, Class<T> type) {

		if (!isJsonPath(value)) {

			_log.warn("JsonPath expression was expected: " + value + ", context -> " + context);

			return null;
		}

		try {

			T result = documentContext.read(value, type);

			if (result != null) {

				return result;
			}

			Object object = documentContext.read(value);

			if (object != null) {

				throw new IllegalArgumentException("Result was: " + JsonUtils.toJson(object));
			}

		} catch(Throwable e) {

			_log.warn("Unable to convert value to expected type [" + type.getSimpleName() + "]: " + value + ", error -> " + e.getMessage() + ", context -> " + context);
		}

		return null;
	}

	private static boolean isJsonPath(String value) {

		return startsWith(value, "$.") || "$".equals(value);
	}
}
