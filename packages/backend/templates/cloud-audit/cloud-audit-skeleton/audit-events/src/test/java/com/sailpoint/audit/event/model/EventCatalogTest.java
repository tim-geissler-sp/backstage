/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.event.model;

import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.audit.event.util.ResourceUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.sailpoint.audit.event.model.EventCatalog.EVENT_CATALOG_URL;
import static com.sailpoint.audit.event.model.EventDescriptor.NULL;
import static com.sailpoint.audit.event.model.EventTemplates.EVENT_TEMPLATES_URL;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EventCatalogTest {

	@Mock
	ResourceUtils _mockResourceUtils;

	ResourceUtils _resourceUtils = new ResourceUtils();

	EventCatalog _eventCatalog;

	public void setupEventCatalog() {

		_eventCatalog = new EventCatalog(_resourceUtils, new EventTemplates(_resourceUtils));
	}

	@Test
	public void stream() {

		setupEventCatalog();

		List<EventDescriptor> eventDescriptors = _eventCatalog.stream().collect(Collectors.toList());
		assertEquals(82, eventDescriptors.size());
	}

	@Test
	public void get() {

		setupEventCatalog();

		assertEquals(
			"Event{action=SAVED_SEARCH_CREATE_PASSED, type=SYSTEM_CONFIG, actor={name=$.actor.name}, target={name=system}, stack=sds, attributes={name=$.model.name, description=$.model.description}, objects=[SAVED, SEARCH], operation=CREATE, status=PASSED}",
			_eventCatalog.get(IdnTopic.SEARCH, "SAVED_SEARCH_CREATE_PASSED").orElse(NULL).getEvent().toString());
	}

	@Test
	public void loadErrors() {

		when(_mockResourceUtils.loadList(eq(Map.class), eq(EVENT_CATALOG_URL), eq("Event Catalog")))
			.thenReturn(_resourceUtils.loadList(Map.class, "eventCatalog_test.json", "Event Catalog"));

		when(_mockResourceUtils.loadMap(eq(EVENT_TEMPLATES_URL), eq("Event Templates")))
			.thenReturn(_resourceUtils.loadMap("eventTemplates_test.json", "Event Templates"));

		_eventCatalog = new EventCatalog(_mockResourceUtils, new EventTemplates(_mockResourceUtils));

		List<EventDescriptor> eventDescriptors = _eventCatalog.stream().collect(Collectors.toList());

		assertEquals(1, eventDescriptors.size());

		assertEquals("status-1", _eventCatalog.get(IdnTopic.SEARCH, "Super Template Order").orElse(NULL).getEvent().getStatus());
	}
}
