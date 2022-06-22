/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.infrastructure;


import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.boot.event.EventService;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.iris.client.Event;
import com.sailpoint.sp.identity.event.domain.ReferenceType;
import com.sailpoint.sp.identity.event.domain.event.IdentityAttributesChangedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityCreatedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityDeletedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit test for KafkaIdentityEventPublisher
 */
@RunWith(MockitoJUnitRunner.class)
public class KafkaIdentityEventPublisherTest {

	@Mock
	private EventService _eventService;

	@Mock
	private FeatureFlagService _featureFlagService;

	@Captor
	ArgumentCaptor<Event> _irisEventCaptor;

	@Before
	public void setup() {
		RequestContext requestContext = new RequestContext();
		requestContext.setOrg("acme-solar");
		requestContext.setPod("dev");
		RequestContext.set(requestContext);
	}

	@After
	public void cleanup() {
		RequestContext.set(null);
	}

	@Test
	public void testKafkaIdentityEventPublisher() {
		KafkaIdentityEventPublisher publisher = new KafkaIdentityEventPublisher(_eventService, _featureFlagService);
		OffsetDateTime timestamp = OffsetDateTime.now();
		String identityChangedTimestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(timestamp);
		IdentityEvent event = IdentityAttributesChangedEvent.builder()
			.identity(IdentityReference.builder()
				.id("12345")
				.name("jon.smith")
				.type(ReferenceType.IDENTITY)
				.build())
			.changes(Collections.emptyList())
			.build();
		publisher.publish(event, timestamp);

		verify(_eventService, times(1)).publish(eq(IdnTopic.IDENTITY_EVENT), _irisEventCaptor.capture());

		Assert.assertEquals("12345", _irisEventCaptor.getValue().getHeader("partitionKey").get());
		Assert.assertEquals("IdentityAttributesChangedEvent", _irisEventCaptor.getValue().getType());
		Assert.assertEquals(identityChangedTimestamp, _irisEventCaptor.getValue().getHeader("identityChangedTimestamp").get());

		event = IdentityCreatedEvent.builder()
			.identity(IdentityReference.builder()
				.id("67890")
				.name("jon.smith")
				.type(ReferenceType.IDENTITY)
				.build())
			.attributes(Collections.emptyMap())
			.build();
		publisher.publish(event, timestamp);

		verify(_eventService, times(2)).publish(eq(IdnTopic.IDENTITY_EVENT), _irisEventCaptor.capture());

		Assert.assertEquals("67890", _irisEventCaptor.getValue().getHeader("partitionKey").get());
		Assert.assertEquals("IdentityCreatedEvent", _irisEventCaptor.getValue().getType());
		Assert.assertEquals(identityChangedTimestamp, _irisEventCaptor.getValue().getHeader("identityChangedTimestamp").get());

		event = IdentityDeletedEvent.builder()
			.identity(IdentityReference.builder()
				.id("abcdef")
				.name("jon.smith")
				.type(ReferenceType.IDENTITY)
				.build())
			.attributes(Collections.emptyMap())
			.build();
		publisher.publish(event, timestamp);

		verify(_eventService, times(3)).publish(eq(IdnTopic.IDENTITY_EVENT), _irisEventCaptor.capture());

		Assert.assertEquals("abcdef", _irisEventCaptor.getValue().getHeader("partitionKey").get());
		Assert.assertEquals("IdentityDeletedEvent", _irisEventCaptor.getValue().getType());
		Assert.assertEquals(identityChangedTimestamp, _irisEventCaptor.getValue().getHeader("identityChangedTimestamp").get());
	}
}
