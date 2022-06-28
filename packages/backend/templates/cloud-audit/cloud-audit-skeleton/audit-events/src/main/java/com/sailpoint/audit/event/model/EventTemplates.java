/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.event.model;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.atlas.search.util.MapUtils;
import com.sailpoint.audit.event.util.ResourceUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static com.sailpoint.atlas.search.util.ListBuilder.listOf;
import static com.sailpoint.audit.event.model.EventDescriptor.NULL;

@Singleton
public class EventTemplates {

	static final String EVENT_TEMPLATES_URL = "eventTemplates.json";

	static final String TEMPLATES = "templates";

	static final String TEMPLATE = "template";

	private static final Log _log = LogFactory.getLog(EventTemplates.class);

	private final Map<String, EventDescriptor> _eventTemplates;

	@Inject
	public EventTemplates(ResourceUtils resourceUtils) {

		Map<String, Object> templateSource = resourceUtils.loadMap(EVENT_TEMPLATES_URL, "Event Templates");

		_log.info("templateSource -> " + toString(templateSource));

		Map<String, EventDescriptor> eventTemplates = new TreeMap<>();

		templateSource.keySet().forEach(name -> loadEventTemplate(eventTemplates, name, templateSource));

		_log.info("eventTemplates -> " + toString(eventTemplates));

		_eventTemplates = ImmutableMap.copyOf(eventTemplates);
	}

	private EventDescriptor loadEventTemplate(Map<String, EventDescriptor> eventTemplates, String name, Map<String, Object> templateSource) {

		return eventTemplates.computeIfAbsent(name, templateName -> loadEventTemplate(eventTemplates, templateName, templateSource, templateSource.get(name)));
	}

	@SuppressWarnings("unchecked")
	private EventDescriptor loadEventTemplate(Map<String, EventDescriptor> eventTemplates, String name, Map<String, Object> templateSource, Object template) {

		if (!(template instanceof Map)) {

			_log.error("Invalid Event Template: name=" + name + ", template=" + template);

			return NULL;
		}

		return loadEventDescriptor((Map<String, Object>) template, templateName -> loadEventTemplate(eventTemplates, templateName, templateSource));
	}

	public static <K, V> String toString(Map<K, V> map) {

		return toString(map, Object::toString);
	}

	public static <K, V> String toString(Map<K, V> map, Function<Map.Entry<K, V>, String> mapper) {

		return "{\n  " + map.entrySet().stream()
			.map(mapper)
			.collect(Collectors.joining("\n  ")) + "\n}";
	}

	EventDescriptor loadEventDescriptor(Map<String, Object> template, Function<String, EventDescriptor> mapper) {

		try {

			List<String> templates = getTemplates(template);

			Map<String, Object> superTemplate = templates.isEmpty() ? new LinkedHashMap<>() : templates.stream()
				.map(name -> validate(name, mapper.apply(name)))
				.map(EventDescriptor::toTemplate)
				.collect(Collector.of(LinkedHashMap::new, EventDescriptor::mixin, MapUtils.mapMerger()));

			return EventDescriptor.of(EventDescriptor.mixin(superTemplate, template));

		} catch (IllegalStateException e) {

			_log.error(e.getMessage() + "\n  template -> " + template);

			return NULL;
		}
	}

	private List<String> getTemplates(Map<String, Object> template) {

		if (template.get(TEMPLATES) instanceof List) {

			return getTemplates(new ArrayList<>((List<?>) template.get(TEMPLATES)));
		}

		if (template.get(TEMPLATE) instanceof String) {

			return listOf((String) template.get(TEMPLATE));
		}

		return Collections.emptyList();
	}

	private List<String> getTemplates(List<?> templates) {

		Collections.reverse(templates);

		return templates.stream()
			.map(this::validate)
			.collect(Collectors.toList());
	}

	private String validate(Object name) {

		if (!(name instanceof String)) {

			throw new NoSuchTemplateException(String.valueOf(name));
		}

		return (String) name;
	}

	private EventDescriptor validate(String name, EventDescriptor template) {

		if (template.isNull()) {

			throw new NoSuchTemplateException(name);
		}

		return template;
	}

	public EventDescriptor get(String name) {

		return _eventTemplates.getOrDefault(name, NULL);
	}
}
