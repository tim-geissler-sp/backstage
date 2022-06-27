/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.infrastructure;

import com.sailpoint.atlas.boot.event.EventService;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.metrics.annotation.Timed;
import com.sailpoint.sp.identity.event.domain.IdentityEventHeaders;
import com.sailpoint.sp.identity.event.domain.IdentityEventPublisher;
import com.sailpoint.sp.identity.event.domain.event.IdentityEvent;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;

/**
 * KafkaIdentityEventPublisher
 */
@RequiredArgsConstructor
@CommonsLog
public class KafkaIdentityEventPublisher implements IdentityEventPublisher {

	public static final String SP_IDENTITY_EVENT_PUBLISHER_LOGGING = "SP_IDENTITY_EVENT_PUBLISHER_LOGGING";

	private final EventService _eventService;

	private final FeatureFlagService _featureFlagService;

	@Timed
	@Override
	public void publish(IdentityEvent event, OffsetDateTime identityChangedTimestamp) {
		final String identityId = event.getIdentity().getId();
		final String eventType = event.getClass().getSimpleName();
		EventBuilder irisEventBuilder = EventBuilder.withTypeAndContent(eventType, event)
			.addHeader(EventHeaders.PARTITON_KEY, identityId);
		if (identityChangedTimestamp != null) {
			irisEventBuilder.addHeader(IdentityEventHeaders.IDENTITY_CHANGED_TIMESTAMP, DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(identityChangedTimestamp));
		}

		if (_featureFlagService.getBoolean(SP_IDENTITY_EVENT_PUBLISHER_LOGGING, false)) {
			log.info(String.format("Publishing %s for identity %s", eventType, identityId));
		}

		_eventService.publish(IdnTopic.IDENTITY_EVENT, irisEventBuilder.build());
	}

}
