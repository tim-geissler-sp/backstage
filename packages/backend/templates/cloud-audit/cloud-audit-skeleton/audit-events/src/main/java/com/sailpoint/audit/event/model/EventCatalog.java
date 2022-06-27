/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.event.model;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.audit.event.util.ResourceUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.sailpoint.atlas.search.util.JsonUtils.PRETTY;
import static com.sailpoint.audit.event.model.EventDescriptor.EVENT;
import static com.sailpoint.audit.event.model.EventDescriptor.EVENT_TYPE;
import static com.sailpoint.audit.event.model.EventDescriptor.TOPIC;

@Singleton
public class EventCatalog {

	static final String EVENT_CATALOG_URL = "eventCatalog.json";

	private static final Log _log = LogFactory.getLog(EventCatalog.class);

	private final EventTemplates _eventTemplates;

	private final Map<IdnTopic, Map<String, EventDescriptor>> _eventDescriptors;

	@Inject
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public EventCatalog(ResourceUtils resourceUtils, EventTemplates eventTemplates) {

		this._eventTemplates = eventTemplates;

		List<Map> eventCatalog = resourceUtils.loadList(Map.class, EVENT_CATALOG_URL, "Event Catalog");

		_log.info("eventCatalog -> " + PRETTY.toJson(eventCatalog));

		Map<IdnTopic, Map<String, EventDescriptor>> eventDescriptors = new LinkedHashMap<>();

		eventCatalog.forEach(template -> loadEventDescriptor(eventDescriptors, template));

		_log.info("eventDescriptors -> " + PRETTY.toJson(eventDescriptors));

		eventDescriptors.entrySet().forEach(entry -> entry.setValue(ImmutableMap.copyOf(entry.getValue())));

		_eventDescriptors = ImmutableMap.copyOf(eventDescriptors);
	}

	private void loadEventDescriptor(Map<IdnTopic, Map<String, EventDescriptor>> eventDescriptors, Map<String, Object> template) {

		EventDescriptor eventDescriptor = _eventTemplates.loadEventDescriptor(template, _eventTemplates::get);

		if (!isValid(eventDescriptor, template)) {

			return;
		}

		eventDescriptors
			.computeIfAbsent(eventDescriptor.getTopic(), key -> new LinkedHashMap<>())
			.put(eventDescriptor.getEventType(), eventDescriptor);
	}

	private boolean isValid(EventDescriptor eventDescriptor, Map<String, Object> template) {

		if (eventDescriptor.isNull()) {

			return false;
		}

		if (eventDescriptor.getTopic() == null) {

			return logError(TOPIC, eventDescriptor, template);
		}

		if (StringUtils.isBlank(eventDescriptor.getEventType())) {

			return logError(EVENT_TYPE, eventDescriptor, template);
		}

		if (eventDescriptor.getEvent() == null) {

			return logError(EVENT, eventDescriptor, template);
		}

		return true;
	}

	private boolean logError(String field, EventDescriptor eventDescriptor, Map<String, Object> template) {

		_log.error("Invalid Event Descriptor ('" + field + "' is required):\n" +
			"  template -> " + template + "\n" +
			"  descriptor -> " + eventDescriptor);

		return false;
	}

	public Stream<EventDescriptor> stream() {

		return _eventDescriptors.values().stream()
			.flatMap(eventTypes -> eventTypes.values().stream());
	}

	public Optional<EventDescriptor> get(IdnTopic topic, String eventType) {

		return Optional.ofNullable(_eventDescriptors.getOrDefault(topic, Collections.emptyMap()).get(eventType));
	}
}
