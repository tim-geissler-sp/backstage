/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.event.normalizer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.search.model.event.Event;
import com.sailpoint.atlas.search.util.MapBuilder;
import com.sailpoint.audit.event.EventContext;
import com.sailpoint.audit.event.model.EventCatalog;
import com.sailpoint.audit.event.model.EventDescriptor;
import com.sailpoint.audit.event.model.EventTemplates;
import com.sailpoint.audit.event.util.ResourceUtils;
import com.sailpoint.audit.utils.TestUtils;
import com.sailpoint.cloud.api.client.model.DtoType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JsonPathNormalizerTest {

	private ResourceUtils _resourceUtils = new ResourceUtils();

	private EventCatalog _eventCatalog = new EventCatalog(_resourceUtils, new EventTemplates(_resourceUtils));

	private EventDescriptor _eventDescriptor;

	private NormalizerFactory _normalizerFactory = new NormalizerFactory();

	private Normalizer _normalizer;

	private Map<String, Object> _domainEvent;

	@Mock
	private EventContext _context;

	private Map<String, Object> _eventTemplate;

	private Map<String, Object> _eventTemplateEvent;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() {

		TestUtils.setDummyRequestContext();

		_eventDescriptor = _eventCatalog.get(IdnTopic.SEARCH, "SAVED_SEARCH_CREATE_PASSED").orElse(EventDescriptor.NULL);

		_normalizer = _normalizerFactory.get(_eventDescriptor.getNormalizer());

		_domainEvent = MapBuilder.mapOf(
			"actor", MapBuilder.mapOf("name", "actorName"),
			"model", MapBuilder.mapOf("name", "name", "description", "description"));

		when(_context.getRequestId()).thenReturn("requestId");

		_eventTemplate = EventDescriptor.toTemplate(EventDescriptor.NULL);

		_eventTemplateEvent = ((Map<String, Object>) _eventTemplate.get("event"));
	}

	@Test
	public void normalize() {

		Event event = normalize(_eventDescriptor);

		assertEquals("SAVED_SEARCH_CREATE_PASSED", event.getAction());
		assertEquals("SYSTEM_CONFIG", event.getType());
		assertEquals("actorName", event.getActor().getName());
		assertNull(event.getActor().getType());
		assertEquals("system", event.getTarget().getName());
		assertNull(event.getTarget().getType());
		assertEquals("sds", event.getStack());
		assertEquals("requestId", event.getTrackingNumber());
		assertNull(event.getIpAddress());
		assertNull(event.getDetails());
		assertEquals("name", event.getAttributes().get("name"));
		assertEquals("description", event.getAttributes().get("description"));
		assertEquals(ImmutableList.of("SAVED", "SEARCH"), event.getObjects());
		assertEquals("CREATE", event.getOperation());
		assertEquals("PASSED", event.getStatus());
	}

	@Test
	public void testNormalizeNoActor() {
		_domainEvent = MapBuilder.mapOf(
			"model", MapBuilder.mapOf("name", "name", "description", "description")
		);

		Event event = normalize(_eventDescriptor);

		assertNotNull(event);
	}

	@Test
	public void getNameTypeJsonPath() {

		_eventTemplateEvent.put("actor", "$.actor");

		_domainEvent.put("actor", ImmutableMap.of("type", "IDENTITY"));

		Event event = normalize(EventDescriptor.of(_eventTemplate));

		assertEquals(DtoType.IDENTITY, event.getActor().getType());
	}

	@Test
	public void toType() {

		_eventTemplateEvent.put("actor", ImmutableMap.of("type", "$.actor.type"));

		_domainEvent.put("actor", ImmutableMap.of("type", "IDENTITY"));

		Event event = normalize(EventDescriptor.of(_eventTemplate));

		assertEquals(DtoType.IDENTITY, event.getActor().getType());
	}

	@Test
	public void toTypeNull() {

		_eventTemplateEvent.put("actor", ImmutableMap.of("type", "$.actor.type"));
		_eventTemplateEvent.put("target", ImmutableMap.of("type", "$.target.type"));

		_domainEvent.put("actor", Collections.singletonMap("type", null));
		_domainEvent.put("target", ImmutableMap.of("type", "garbage"));

		Event event = normalize(EventDescriptor.of(_eventTemplate));

		assertNull(event.getActor().getType());
		assertNull(event.getTarget().getType());
	}

	@Test
	public void getAttributesJsonPath() {

		_eventTemplateEvent.put("attributes", "$.attributes");

		_domainEvent.put("attributes", ImmutableMap.of("key", "value"));

		Event event = normalize(EventDescriptor.of(_eventTemplate));

		assertEquals("value", event.getAttributes().get("key"));
	}

	@Test
	public void toMapNull() {

		_domainEvent.clear();

		Event event = normalize(_eventDescriptor);

		assertEquals(Collections.emptyMap(), event.getAttributes());
	}

	@Test
	public void toAttribute() {

		_eventTemplateEvent.put("attributes", new MapBuilder<>()
			.put("nested", new MapBuilder<>()
				.put("group", ImmutableMap.of("x", "member", "y", "$.values[0]"))
				.put("reference", "$.values[1]")
				.put("series", ImmutableList.of("pod", "save", "$.values[2]", true))
				.build())
			.put("toObject", "literal")
			.build());

		_domainEvent.put("values", ImmutableList.of("A", "B", "america"));

		Event event = normalize(EventDescriptor.of(_eventTemplate));

		String nested = (String) event.getAttributes().get("nested");
		assertTrue(nested.contains("series"));

		assertEquals("literal", event.getAttributes().get("toObject"));
	}

	@Test
	public void getObjectsJsonPath() {

		_eventTemplateEvent.put("objects", "$.objects");

		_domainEvent.put("objects", ImmutableList.of("OBJECT", 999));

		Event event = normalize(EventDescriptor.of(_eventTemplate));

		assertEquals(ImmutableList.of("OBJECT"), event.getObjects());
	}

	@Test
	public void toStringError() {

		_eventTemplateEvent.put("actor", ImmutableMap.of("name", 3.14));

		Event event = normalize(EventDescriptor.of(_eventTemplate));

		assertNull(event.getActor().getName());
		assertNull(event.getActor().getType());
	}

	@Test
	public void getStringConvertBoolean() {

		_eventTemplateEvent.put("details", "$.details");

		_domainEvent.put("details", true);

		Event event = normalize(EventDescriptor.of(_eventTemplate));

		assertEquals("true", event.getDetails());
	}

	@Test
	public void getObjectNotJsonPath() {

		_eventTemplateEvent.put("target", "not a JsonPath");

		Event event = normalize(EventDescriptor.of(_eventTemplate));

		assertNull(event.getTarget());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getObjectNull() {

		((Map) _domainEvent.get("actor")).put("name", null);

		Event event = normalize(_eventDescriptor);

		assertNull(event.getActor().getName());
	}

	@Test
	public void getObjectConvertError() {

		_eventTemplateEvent.put("details", "$.details");

		_domainEvent.put("details", ImmutableList.of("element"));

		Event event = normalize(EventDescriptor.of(_eventTemplate));

		assertNull(event.getDetails());
	}

	@Test
	public void getObjectInferredError() {

		_eventTemplateEvent.put("actor", "$.actor");

		_domainEvent.put("actor", ImmutableList.of("element"));

		Event event = normalize(EventDescriptor.of(_eventTemplate));

		assertNull(event.getActor());
	}

	@SuppressWarnings("unchecked")
	private Event normalize(EventDescriptor eventDescriptor) {

		when(_context.getDomainEvent()).thenReturn((Map) _domainEvent);
		when(_context.getTimestamp()).thenReturn(OffsetDateTime.now());

		return _normalizer.normalize(_context, eventDescriptor);
	}
}
