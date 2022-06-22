/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.sailpoint.ets.domain.trigger.EtsFeatureStore;
import com.sailpoint.ets.infrastructure.web.dto.TriggerAllDto;
import com.sailpoint.ets.infrastructure.web.dto.TriggerDto;
import com.sailpoint.ets.service.TriggerService;
import com.sailpoint.metrics.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TriggerController
 */
@RestController
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@RequestMapping(value = "/triggers")
public class TriggerController extends EtsBaseController<TriggerDto> {


	private static final Map<String, Comparator<TriggerDto>> _comparatorMap = ImmutableMap.of("id", Comparator.comparing(TriggerDto::getId),
		"name", Comparator.comparing(TriggerDto::getName));
	private static final Map<String, String> _dtoPropertyMap = ImmutableMap.of("id", "_id");

	private final TriggerService _triggerService;
	private final EtsFeatureStore _etsFeatureStore;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Set<String> getQueryableFields() {
		return _dtoPropertyMap.keySet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Set<String> getSortableFields() {
		return _comparatorMap.keySet();
	}

	@Timed
	@PreAuthorize("hasRole('idn:trigger-service-triggers:read')")
	@GetMapping
	public ResponseEntity listTriggers() {
		Gson gson = new Gson();
		return okResponse(_triggerService.listTriggers()
			.stream()
			.filter(t -> t.isEnabledForTenant(_etsFeatureStore))
			.map(t ->
				TriggerDto.builder()
					.id(t.getId().toString())
					.description(t.getDescription().toString())
					.name(t.getName().toString())
					.type(t.getType())
					.inputSchema(gson.toJson(t.getInputSchema()))
					.outputSchema(t.getOutputSchema() == null ? null : gson.toJson(t.getOutputSchema()))
					.exampleInput(t.getExampleInput())
					.exampleOutput(t.getExampleOutput())
					.build()
			)
			.collect(Collectors.toList()), getQueryOptions(), _comparatorMap, _dtoPropertyMap);
	}

	@PreAuthorize("hasRole('idn:trigger-service-internal:read')")
	@GetMapping("/all")
	public ResponseEntity listAllTriggers() {
		Gson gson = new Gson();
		return okResponse(_triggerService.listTriggers()
			.stream()
			.map(t ->
				TriggerAllDto.TriggerAllDtoBuilder()
					.id(t.getId().toString())
					.description(t.getDescription().toString())
					.name(t.getName().toString())
					.type(t.getType())
					.inputSchema(gson.toJson(t.getInputSchema()))
					.outputSchema(t.getOutputSchema() == null ? null : gson.toJson(t.getOutputSchema()))
					.exampleInput(t.getExampleInput())
					.exampleOutput(t.getExampleOutput())
					.featureStoreKey(EtsFeatureStore.getFeatureKey(t.getId()))
					.build()
			)
			.collect(Collectors.toList()));
	}
}
