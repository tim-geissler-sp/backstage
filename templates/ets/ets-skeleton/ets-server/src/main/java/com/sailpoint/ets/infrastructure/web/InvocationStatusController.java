/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web;

import com.google.common.collect.ImmutableMap;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.cloud.api.client.model.errors.ErrorMessageDto;
import com.sailpoint.ets.domain.command.InvokeTestTriggerCommand;
import com.sailpoint.ets.domain.status.CompleteInvocationInput;
import com.sailpoint.ets.domain.status.InvocationStatus;
import com.sailpoint.ets.domain.trigger.TriggerId;
import com.sailpoint.ets.infrastructure.util.DateTimeToUTCConverter;
import com.sailpoint.ets.infrastructure.util.ValidatorService;
import com.sailpoint.ets.infrastructure.util.WebUtil;
import com.sailpoint.ets.infrastructure.web.dto.InvocationDto;
import com.sailpoint.ets.infrastructure.web.dto.TestInvocationDto;
import com.sailpoint.ets.infrastructure.web.dto.status.CompleteInvocationInputDto;
import com.sailpoint.ets.infrastructure.web.dto.status.InvocationStatusDto;
import com.sailpoint.ets.service.TriggerService;
import com.sailpoint.metrics.annotation.Metered;
import com.sailpoint.metrics.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.spi.MappingContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.ws.rs.NotFoundException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 *  InvocationStatusController controller for handle external API end points.
 */
@RestController
@RequiredArgsConstructor(onConstructor_={@Autowired})
@RequestMapping(value = "/trigger-invocations")
@CommonsLog
public class InvocationStatusController extends EtsBaseController<InvocationStatusDto> {

	private final TriggerService _triggerService;
	private final ModelMapper _modelMapper;
	private final ValidatorService _validatorService;

	private final DateTimeToUTCConverter _dateTimeToUTCConverter;

	private static final Map<String, Comparator<InvocationStatusDto>> _comparatorMap = ImmutableMap.of("triggerId", Comparator.comparing(InvocationStatusDto::getTriggerId)
																						, "subscriptionName", Comparator.comparing(InvocationStatusDto::getSubscriptionName)
																						, "created", Comparator.comparing(InvocationStatusDto::getCreated)
																						, "completed", Comparator.comparing(InvocationStatusDto::getCompleted));

	private static final Map<String, String> _dtoPropertyMap = ImmutableMap.of("id", "_id", "triggerId", "_triggerId", "type", "_type", "subscriptionId", "_subscriptionId");

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

	@PostConstruct
	public void init() {
		_modelMapper.typeMap(InvocationStatus.class, InvocationStatusDto.class)
			.addMapping(InvocationStatus::getStartInvocationInput, InvocationStatusDto::setStartInvocationInput)
			.addMappings(mapper -> {
					mapper.using(new CompleteInvocationConverter())
						.map(InvocationStatus::getCompleteInvocationInput, InvocationStatusDto::setCompleteInvocationInput);
					mapper.using(_dateTimeToUTCConverter).map(InvocationStatus::getCompleted, InvocationStatusDto::setCompleted);
					mapper.using(_dateTimeToUTCConverter).map(InvocationStatus::getCreated, InvocationStatusDto::setCreated);
				});
	}

	@Timed
	@Metered
	@GetMapping("/status")
	@PreAuthorize("hasRole('idn:trigger-service-invocation-status:read')")
	public ResponseEntity<List<InvocationStatusDto>> getInvocationStatuses() {
		return okResponse(
			_triggerService.listInvocationStatuses(WebUtil.getCurrentTenantId())
				.map(s -> _modelMapper.map(s, InvocationStatusDto.class)),
			getQueryOptions(),
			_comparatorMap,
			_dtoPropertyMap
		);
	}

	@Timed
	@Metered
	@GetMapping("/status/{invocationStatusId}")
	@PreAuthorize("hasRole('idn:trigger-service-invocation-status:read')")
	public ResponseEntity<InvocationStatusDto> getInvocationStatusesById(@PathVariable("invocationStatusId") String invocationStatusId) {
		Optional<InvocationStatus> invocationOptional = _triggerService.listInvocationStatusesById(WebUtil.getCurrentTenantId(), UUID.fromString(invocationStatusId));

		InvocationStatus entity = invocationOptional.orElseThrow(() -> {
			return new NotFoundException("Invocation status id " + invocationStatusId + " not found.");
		});
		return okResponse(_modelMapper.map(entity, InvocationStatusDto.class));
	}

	@Timed
	@Metered
	@PreAuthorize("hasRole('idn:trigger-service-invocation-test:create')")
	@PostMapping("/test")
	public ResponseEntity<List<InvocationDto>> startTestInvocation(@RequestBody TestInvocationDto input) {
		_validatorService.validate(input);
		if(input.getInput() != null) {
			_validatorService.isValidInputForTriggerSchema(input.getTriggerId(), input.getInput());
		}

		// Handle optional input subscriptionIds
		Optional<Set<UUID>> subscriptionIds = Optional.empty();
		if (input.getSubscriptionIds() != null) {
			try {
				subscriptionIds = Optional.of(input.getSubscriptionIds().stream()
					.map(id -> UUID.fromString(id))
					.collect(Collectors.toSet()));
			} catch (Exception e) {
				return ResponseEntity.badRequest().build();
			}
		}

		InvokeTestTriggerCommand cmd = InvokeTestTriggerCommand.builder()
			.tenantId(WebUtil.getCurrentTenantId())
			.requestId(RequestContext.ensureGet().getId())
			.triggerId(new TriggerId(input.getTriggerId()))
			.context(input.getContentJson())
			.subscriptionIds(subscriptionIds)
			.input(Optional.ofNullable(input.getInput()))
			.headers(WebUtil.getHeaders())
			.build();

		List<InvocationDto> invocationDtos = _triggerService.invokeTestTrigger(cmd)
			.stream()
			.map(invocation -> _modelMapper.map(invocation, InvocationDto.class))
			.collect(Collectors.toList());

		return okResponseListInvocationDto(invocationDtos);
	}

	static class CompleteInvocationConverter implements Converter<CompleteInvocationInput, CompleteInvocationInputDto> {
		@Override
		public CompleteInvocationInputDto convert(MappingContext<CompleteInvocationInput, CompleteInvocationInputDto> context) {
			CompleteInvocationInputDto result = new CompleteInvocationInputDto();
			CompleteInvocationInput input = context.getSource();
			if(input != null) {
				result.setOutput(input.getOutput());
				if(input.getError() != null) {
					result.setLocalizedError(new ErrorMessageDto(input.getError()));
				}
			}
			return result;
		}
	}

	/**
	 * Build an OK response of InvocationDto list with X-Total-Count header if requested.
	 *
	 * @param list list of dto
	 * @return {@link ResponseEntity}
	 */
	private ResponseEntity<List<InvocationDto>> okResponseListInvocationDto(List<InvocationDto> list) {
		if (isCountHeaderRequested()) {
			return okResponse(list, list.size());
		} else {
			return ResponseEntity.ok(list);
		}
	}
}
