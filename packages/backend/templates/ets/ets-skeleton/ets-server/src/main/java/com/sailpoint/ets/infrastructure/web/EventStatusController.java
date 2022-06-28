/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web;

import com.google.common.collect.ImmutableMap;
import com.sailpoint.ets.infrastructure.event.PersistedEvent;
import com.sailpoint.ets.infrastructure.web.dto.EventStatusDto;
import com.sailpoint.ets.service.TriggerService;
import com.sailpoint.metrics.annotation.Metered;
import com.sailpoint.metrics.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;

/**
 * Event Status Controller
 */
@RestController
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@RequestMapping(value = "/trigger-invocations")
public class EventStatusController extends EtsBaseController<EventStatusDto> {

	private static final Map<String, Comparator<EventStatusDto>> _eventsStatusDtoComparatorMap =
		ImmutableMap.of("topic", Comparator.comparing(EventStatusDto::getTopic));
	private static final Map<String, String> _eventsStatusDtoPropertyMap =
		ImmutableMap.of("topic", "_topic");

	private final TriggerService _triggerService;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Set<String> getQueryableFields() {
		return _eventsStatusDtoPropertyMap.keySet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Set<String> getSortableFields() {
		return _eventsStatusDtoComparatorMap.keySet();
	}

	/**
	 * Get list of persisted, pending events.
	 *
	 * @return {@link ResponseEntity} of {@code List<EventStatusDto>}
	 */
	@Timed
	@Metered
	@PreAuthorize("hasRole('idn:trigger-service-invocation-status:read')")
	@GetMapping("/pending-events")
	public ResponseEntity listEvents() {
		List<EventStatusDto> eventStatusDtos = new ArrayList<>();

		_triggerService.listEvents()
			.stream()
			.collect(groupingBy(PersistedEvent::getTopic))
			.forEach((topic, persistedEvents) ->
				eventStatusDtos.add(
					EventStatusDto.builder()
						.topic(topic)
						.count(persistedEvents.size())
						.build()
				)
			);

		return okResponse(eventStatusDtos, getQueryOptions(), _eventsStatusDtoComparatorMap,
			_eventsStatusDtoPropertyMap);
	}
}
