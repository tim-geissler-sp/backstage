/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web;

import com.sailpoint.atlas.RequestContext;
import com.sailpoint.ets.domain.Secret;
import com.sailpoint.ets.domain.command.CompleteInvocationCommand;
import com.sailpoint.ets.domain.command.InvokeTriggerCommand;
import com.sailpoint.ets.domain.invocation.Invocation;
import com.sailpoint.ets.domain.trigger.TriggerId;
import com.sailpoint.ets.infrastructure.util.ValidatorService;
import com.sailpoint.ets.infrastructure.util.WebUtil;
import com.sailpoint.ets.infrastructure.web.dto.InvocationDto;
import com.sailpoint.ets.service.TriggerService;
import com.sailpoint.metrics.annotation.Metered;
import com.sailpoint.metrics.annotation.Timed;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * InvocationController
 */
@RestController
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@RequestMapping(value = "/trigger-invocations")
@CommonsLog
public class InvocationController extends EtsBaseController<InvocationDto> {
	private final TriggerService _triggerService;
	private final ModelMapper _modelMapper;
	private final ValidatorService _validatorService;

	@PostConstruct
	public void init() {
		_modelMapper.typeMap(Invocation.class, InvocationDto.class)
			.addMapping(Invocation::getContext, InvocationDto::setContentJson);
	}

	@Timed
	@Metered
	@PreAuthorize("hasRole('idn:trigger-service-invocations:create')")
	@PostMapping("/start")
	public ResponseEntity<List<InvocationDto>> startInvocation(@RequestBody StartInvocationInput input) {
		_validatorService.validate(input);
		InvokeTriggerCommand cmd = InvokeTriggerCommand.builder()
				.tenantId(WebUtil.getCurrentTenantId())
				.requestId(RequestContext.ensureGet().getId())
				.triggerId(new TriggerId(input.getTriggerId()))
				.input(input.getInput())
				.context(input.getContentJson())
			    .headers(WebUtil.getHeaders())
				.build();

		List<InvocationDto> invocationDtos = _triggerService.invokeTrigger(cmd)
			.stream()
			.map(invocation -> _modelMapper.map(invocation, InvocationDto.class))
			.collect(Collectors.toList());

		return okResponse(invocationDtos);
	}

	@Timed
	@Metered
	@PostMapping("/{id}/complete")
	public ResponseEntity completeInvocation(@PathVariable("id") String id, @RequestBody CompleteInvocationInput input) {
		CompleteInvocationCommand cmd = CompleteInvocationCommand.builder()
				.tenantId(WebUtil.getCurrentTenantId())
				.requestId(RequestContext.ensureGet().getId())
				.invocationId(UUID.fromString(id))
				.output(input.getOutput())
				.error(input.getError())
				.secret(new Secret(input.getSecret()))
				.build();

		_triggerService.completeInvocation(cmd);
		return noContentResponse();
	}

	@Data
	private static class StartInvocationInput {
		@NotEmpty
		private String _triggerId;
		@NotNull
		private Map<String, Object> _input;
		private Map<String, Object> _contentJson;
	}

	@Data
	private static class CompleteInvocationInput {
		private Map<String, Object> _output;
		private String _error;
		private String _secret;
	}
}
