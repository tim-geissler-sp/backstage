/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web;

import com.google.common.collect.ImmutableMap;
import com.sailpoint.ets.domain.invocation.Invocation;
import com.sailpoint.ets.infrastructure.util.WebUtil;
import com.sailpoint.ets.infrastructure.web.dto.ActiveInvocationDto;
import com.sailpoint.ets.service.TriggerService;
import com.sailpoint.metrics.annotation.Metered;
import com.sailpoint.metrics.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Active Invocation Controller
 */
@RestController
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@RequestMapping(value = "/trigger-invocations")
@CommonsLog
public class ActiveInvocationController extends EtsBaseController<ActiveInvocationDto> {

	private static final Map<String, Comparator<ActiveInvocationDto>> _activeInvocationDtoComparatorMap =
		ImmutableMap.of(
			"triggerId", Comparator.comparing(ActiveInvocationDto::getTriggerId)
		);
	private static final Map<String, String> _activeInvocationDtoPropertyMap =
		ImmutableMap.of(
			"id", "_id",
			"triggerId", "_triggerId"
		);

	private final TriggerService _triggerService;
	private final ModelMapper _modelMapper;

	@PostConstruct
	public void init() {
		_modelMapper.addConverter(toActiveInvocationDto, Invocation.class, ActiveInvocationDto.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Set<String> getQueryableFields() {
		return _activeInvocationDtoPropertyMap.keySet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Set<String> getSortableFields() {
		return _activeInvocationDtoComparatorMap.keySet();
	}

	/**
	 * Get list of active trigger invocations.
	 *
	 * @return {@link ResponseEntity} of {@code List<ActiveInvocationDto>}
	 */
	@Timed
	@Metered
	@PreAuthorize("hasRole('idn:trigger-service-invocation-status:read')")
	@GetMapping
	public ResponseEntity listActiveInvocations() {
		List<ActiveInvocationDto> activeInvocations = _triggerService.listActiveInvocations(WebUtil.getCurrentTenantId())
			.stream()
			.map(invocation -> _modelMapper.map(invocation, ActiveInvocationDto.class))
			.collect(Collectors.toList());

		return okResponse(activeInvocations, getQueryOptions(), _activeInvocationDtoComparatorMap,
			_activeInvocationDtoPropertyMap);
	}

	/**
	 * Converter from {@link Invocation} to {@link ActiveInvocationDto}.
	 */
	private Converter<Invocation, ActiveInvocationDto> toActiveInvocationDto =
		new AbstractConverter<Invocation, ActiveInvocationDto>() {
			@Override
			protected ActiveInvocationDto convert(Invocation invocation) {
				if (invocation == null) {
					return null;
				}

				final String invocationId = invocation.getId() == null
					? null
					: invocation.getId().toString();
				final String triggerId = invocation.getTriggerId() == null
					? null
					: invocation.getTriggerId().getValue();
				final OffsetDateTime created = invocation.getCreated() == null
					? null
					: invocation.getCreated().withOffsetSameInstant(ZoneOffset.UTC);
				final OffsetDateTime deadline = invocation.getDeadline() == null
					? null
					: invocation.getDeadline().withOffsetSameInstant(ZoneOffset.UTC);

				return ActiveInvocationDto.builder()
					.id(invocationId)
					.triggerId(triggerId)
					.created(created)
					.deadline(deadline)
					.build();
			}
		};
}
