/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.event.model;

import com.google.common.base.MoreObjects;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.atlas.search.util.MapUtils;
import com.sailpoint.atlas.search.util.ObjectUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class EventDescriptor {

	static final String TOPIC = "topic";

	static final String EVENT_TYPE = "eventType";

	static final String NORMALIZER = "normalizer";

	static final String EVENT = "event";

	static final String ACTION = "action";

	static final String TYPE = "type";

	static final String ACTOR = "actor";

	static final String TARGET = "target";

	static final String STACK = "stack";

	static final String IP_ADDRESS = "ipAddress";

	static final String DETAILS = "details";

	static final String ATTRIBUTES = "attributes";

	static final String OBJECTS = "objects";

	static final String OPERATION = "operation";

	static final String STATUS = "status";

	public static final EventDescriptor NULL = of(mixin(new LinkedHashMap<>(), Collections.singletonMap(EVENT, new LinkedHashMap<>())));

	public static final String NULL_VALUE = "null";

	private IdnTopic _topic;

	private String _eventType;

	private String _normalizer;

	private Event _event;

	public static EventDescriptor of(Map<?, ?> template) {

		return JsonUtils.parse(EventDescriptor.class, JsonUtils.toJsonExcludeNull(template));
	}

	public static Map<String, Object> toTemplate(EventDescriptor eventDescriptor) {

		return JsonUtils.parseMap(JsonUtils.toJsonExcludeNull(eventDescriptor));
	}

	@SuppressWarnings("unchecked")
	static Map<String, Object> mixin(Map<String, Object> target, Map<String, Object> source) {

		mixin(target, source, TOPIC);
		mixin(target, source, EVENT_TYPE);
		mixin(target, source, NORMALIZER);

		if (source.get(EVENT) instanceof Map ||
			(source.get(EVENT) == null && target.get(EVENT) instanceof Map)) {

			Map<String, Object> targetEvent = (Map<String, Object>) target.computeIfAbsent(EVENT, key -> new LinkedHashMap<>());
			Map<String, Object> sourceEvent = ObjectUtils.orElse((Map<String, Object>) source.get(EVENT), LinkedHashMap::new);

			mixin(targetEvent, sourceEvent, ACTION, () -> target.get(EVENT_TYPE), false);
			mixin(targetEvent, sourceEvent, TYPE);
			mixin(targetEvent, sourceEvent, ACTOR, true);
			mixin(targetEvent, sourceEvent, TARGET, true);
			mixin(targetEvent, sourceEvent, STACK);
			mixin(targetEvent, sourceEvent, IP_ADDRESS);
			mixin(targetEvent, sourceEvent, DETAILS);
			mixin(targetEvent, sourceEvent, ATTRIBUTES, true);
			mixin(targetEvent, sourceEvent, OBJECTS);
			mixin(targetEvent, sourceEvent, OPERATION);
			mixin(targetEvent, sourceEvent, STATUS);

		} else if (source.get(EVENT) != null) {

			throw new IllegalStateException(String.format(
				"Invalid Event Descriptor ('event' type is %s): event -> %s",
				source.get(EVENT).getClass().getSimpleName(), source.get(EVENT)));
		}

		return target;
	}

	private static void mixin(Map<String, Object> target, Map<String, Object> source, String field) {

		mixin(target, source, field, false);
	}

	private static void mixin(Map<String, Object> target, Map<String, Object> source, String field, boolean isComposite) {

		mixin(target, source, field, () -> null, isComposite);
	}

	@SuppressWarnings("unchecked")
	private static void mixin(Map<String, Object> target, Map<String, Object> source, String field, Supplier<Object> defaultValue, boolean isComposite) {

		Object value = MapUtils.get(source, field, defaultValue);

		if (value != null) {

			if (NULL_VALUE.equals(value)) {

				target.remove(field);
				return;
			}

			target.put(field, value);
		}

		if (isComposite && (target.get(field) instanceof Map || target.get(field) == null)) {

			source.entrySet().stream()
				.filter(entry -> entry.getKey().startsWith(field + "."))
				.forEach(entry -> ((Map<String, Object>) target.computeIfAbsent(field, key -> new LinkedHashMap<>()))
					.put(entry.getKey().replace(field + ".", ""), entry.getValue()));
		}
	}

	public IdnTopic getTopic() {

		return _topic;
	}

	public String getEventType() {

		return _eventType;
	}

	public String getNormalizer() {

		return _normalizer;
	}

	public Event getEvent() {

		return _event;
	}

	@Override
	public String toString() {

		if (isNull()) {

			return "EventDescriptor.NULL";
		}

		return MoreObjects.toStringHelper(this).omitNullValues()
			.add(TOPIC, _topic)
			.add(EVENT_TYPE, _eventType)
			.add(NORMALIZER, _normalizer)
			.add(EVENT, _event)
			.toString();
	}

	public boolean isNull() {

		return this == NULL;
	}

	public static class Event {

		String _action;

		String _type;

		Object _actor;

		Object _target;

		String _stack;

		String _ipAddress;

		String _details;

		Object _attributes;

		Object _objects;

		String _operation;

		String _status;

		public String getAction() {

			return _action;
		}

		public String getType() {

			return _type;
		}

		public Object getActor() {

			return _actor;
		}

		public Object getTarget() {

			return _target;
		}

		public String getStack() {

			return _stack;
		}

		public String getIpAddress() {

			return _ipAddress;
		}

		public String getDetails() {

			return _details;
		}

		public Object getAttributes() {

			return _attributes;
		}

		public Object getObjects() {

			return _objects;
		}

		public String getOperation() {

			return _operation;
		}

		public String getStatus() {

			return _status;
		}

		@Override
		public String toString() {

			return MoreObjects.toStringHelper(this).omitNullValues()
				.add(ACTION, _action)
				.add(TYPE, _type)
				.add(ACTOR, _actor)
				.add(TARGET, _target)
				.add(STACK, _stack)
				.add(IP_ADDRESS, _ipAddress)
				.add(DETAILS, _details)
				.add(ATTRIBUTES, _attributes)
				.add(OBJECTS, _objects)
				.add(OPERATION, _operation)
				.add(STATUS, _status)
				.toString();
		}
	}
}
