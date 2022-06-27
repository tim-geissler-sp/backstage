/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.util.AwsEncryptionServiceUtil;
import com.sailpoint.atlas.util.JsonPathUtil;
import com.sailpoint.cloud.api.client.model.BaseReferenceDto;
import com.sailpoint.cloud.api.client.model.DtoType;
import com.sailpoint.config.export.ExportedObject;
import com.sailpoint.config.export.Message;
import com.sailpoint.config.export.ObjectImportResult;
import com.sailpoint.ets.domain.OffsetBasedPageRequest;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.command.SubscribeCommand;
import com.sailpoint.ets.domain.command.UnsubscribeCommand;
import com.sailpoint.ets.domain.command.UpdateSubscriptionCommand;
import com.sailpoint.ets.domain.subscription.Subscription;
import com.sailpoint.ets.domain.subscription.SubscriptionSpecification;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.domain.trigger.TriggerId;
import com.sailpoint.ets.exception.ValidationException;
import com.sailpoint.ets.infrastructure.util.EventBridgeConfigConverter;
import com.sailpoint.ets.infrastructure.util.HTTPConfigConverter;
import com.sailpoint.ets.infrastructure.util.InlineConfigConverter;
import com.sailpoint.ets.infrastructure.util.ScriptConfigConverter;
import com.sailpoint.ets.infrastructure.util.TriggerIdToTriggerNameConverter;
import com.sailpoint.ets.infrastructure.util.ValidatorService;
import com.sailpoint.ets.infrastructure.util.WebUtil;
import com.sailpoint.ets.infrastructure.util.WorkflowConfigConverter;
import com.sailpoint.ets.infrastructure.web.dto.BasicAuthConfigDto;
import com.sailpoint.ets.infrastructure.web.dto.BearerTokenAuthConfigDto;
import com.sailpoint.ets.infrastructure.web.dto.HttpAuthenticationType;
import com.sailpoint.ets.infrastructure.web.dto.HttpConfigDto;
import com.sailpoint.ets.infrastructure.web.dto.SubscriptionDto;
import com.sailpoint.ets.infrastructure.web.dto.SubscriptionFilterValidationDto;
import com.sailpoint.ets.service.TriggerService;
import com.sailpoint.metrics.annotation.Timed;
import com.sailpoint.utilities.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.sailpoint.atlas.boot.usage.filter.RequestContextRestFilter.CUSTOM_FIELDS;
import static com.sailpoint.atlas.boot.usage.filter.RequestContextRestFilter.EVENT_TYPE;

/**
 * SubscriptionController
 */
@RestController
@RequiredArgsConstructor(onConstructor_={@Autowired})
@RequestMapping(value = "/trigger-subscriptions")
@CommonsLog
public class SubscriptionController extends EtsBaseController<SubscriptionDto> {

	private static final Map<String, Comparator<SubscriptionDto>> _comparatorMap = ImmutableMap.of(
		"triggerId", Comparator.comparing(SubscriptionDto::getTriggerId),
		"triggerName", Comparator.comparing(SubscriptionDto::getTriggerName)
	);

	private static final Set<String> _filters = ImmutableSet.of("id", "triggerId", "type", "name");

	private final TriggerService _triggerService;
	private final ModelMapper _modelMapper;
	private final ValidatorService _validatorService;
	private final AwsEncryptionServiceUtil _awsEncryptionServiceUtil;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Set<String> getQueryableFields() {
		return _filters;
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
		HTTPConfigConverter httpConfigConverter = new HTTPConfigConverter(_awsEncryptionServiceUtil);
		InlineConfigConverter inlineConfigConverter = new InlineConfigConverter();
		ScriptConfigConverter scriptConfigConverter = new ScriptConfigConverter(_awsEncryptionServiceUtil);
		EventBridgeConfigConverter eventbridgeConfigConverter = new EventBridgeConfigConverter();
		WorkflowConfigConverter workflowConfigConverter = new WorkflowConfigConverter();
		TriggerIdToTriggerNameConverter triggerIdToTriggerNameConverter = new TriggerIdToTriggerNameConverter(_triggerService);
		_modelMapper.typeMap(Subscription.class, SubscriptionDto.class)
			.addMappings(mapper -> {
				mapper.using(httpConfigConverter).map(Subscription::getConfig, SubscriptionDto::setHttpConfig);
				mapper.using(inlineConfigConverter).map(Subscription::getConfig, SubscriptionDto::setInlineConfig);
				mapper.using(scriptConfigConverter).map(Subscription::getConfig, SubscriptionDto::setScriptConfig);
				mapper.using(eventbridgeConfigConverter).map(Subscription::getConfig, SubscriptionDto::setEventBridgeConfig);
				mapper.using(workflowConfigConverter).map(Subscription::getConfig, SubscriptionDto::setWorkflowConfig);
				mapper.using(triggerIdToTriggerNameConverter).map(Subscription::getTriggerId, SubscriptionDto::setTriggerName);
			});

	}

	@Timed
	@PreAuthorize("hasRole('idn:trigger-service-subscriptions:read')")
	@GetMapping
	public ResponseEntity<List<SubscriptionDto>> listSubscriptions() {
		final EtsQueryOptions queryOptions = getQueryOptions();
		Page<Subscription> page = listSubscriptions(queryOptions);
		if (isCountHeaderRequested()) {
			return okResponse(
				page.stream()
					.map(s -> _modelMapper.map(s, SubscriptionDto.class))
					// obfuscate passwords and bearer tokens
					.map(s -> obfuscateSecrets(s))
					.sorted(getComparator(queryOptions.getSorterList(), _comparatorMap))
					.collect(Collectors.toList()),
				(int) page.getTotalElements()
			);
		} else {
			return ResponseEntity.ok(
				page.stream()
					.map(s -> _modelMapper.map(s, SubscriptionDto.class))
					.map(s -> obfuscateSecrets(s))
					.sorted(getComparator(queryOptions.getSorterList(), _comparatorMap))
					.collect(Collectors.toList())
			);
		}
	}

	@Timed
	@PreAuthorize("hasRole('idn:trigger-service-subscriptions:read')")
	@GetMapping( path="/{id}")
	public ResponseEntity<SubscriptionDto> getSubscription(@Valid @PathVariable UUID id) {
		Optional<Subscription> subscriptionOptional = _triggerService.getSubscription(WebUtil.getCurrentTenantId(),id);

		Subscription entity = subscriptionOptional.orElseThrow(() -> {
			return new NotFoundException("Subscription id " + id + " not found.");
		});

		return okResponse(obfuscateSecrets(_modelMapper.map(entity, SubscriptionDto.class)));
	}

	@Timed
	@PreAuthorize("hasRole('idn:trigger-service-subscriptions:create')")
	@PostMapping
	public ResponseEntity subscribe(@RequestBody SubscriptionDto subscriptionDto) {
		Subscription subscription = validateAndCreate(subscriptionDto);

		RequestContext rc = RequestContext.ensureGet();
		Map<String, Object> customFields = new HashMap<>();
		customFields.put(EVENT_TYPE, "ETS_Subscription_Activity");
		customFields.put("SubscriptionType", SubscriptionType.valueOf(subscriptionDto.getType()).toString());
		customFields.put("TriggerID", subscriptionDto.getTriggerId());
		customFields.put("SubscriptionID", UUID.fromString(subscriptionDto.getId()).toString());
		rc.setAttribute(CUSTOM_FIELDS, customFields, true);

		return createdResponse(obfuscateSecrets(_modelMapper.map(subscription, SubscriptionDto.class)));
	}

	@Timed
	@PreAuthorize("hasRole('idn:trigger-service-subscriptions:update')")
	@PutMapping("/{id}")
	public ResponseEntity updateSubscription(@PathVariable("id") String id, @RequestBody SubscriptionDto subscriptionDto) {
		subscriptionDto.setId(id);
		_validatorService.validate(subscriptionDto);
		UpdateSubscriptionCommand cmd = buildUpdateCommand(id, subscriptionDto);

		Subscription subscription = _triggerService.updateSubscription(cmd);
		return okResponse(obfuscateSecrets(_modelMapper.map(subscription, SubscriptionDto.class)));
	}

	// This PATCH method behaves similarly to the PUT method (updateSubscription).
	// Since this is a PATCH method, a complete, whole subscription object is unnecessary.
	// We must try to then merge a given subscription into the existing subscription.
	@Timed
	@PreAuthorize("hasRole('idn:trigger-service-subscriptions:update')")
	@PatchMapping(path="/{id}", consumes = "application/json-patch+json")
	public ResponseEntity patchSubscription(@PathVariable("id") String id, @RequestBody JsonPatch patch) {
		TenantId tenantId = WebUtil.getCurrentTenantId();
		// retrieve the existing subscription. If it doesn't exist, then the path parameter given is invalid.
		Optional<Subscription> existingSubscription = _triggerService.getSubscription(tenantId, UUID.fromString(id));
		if (!existingSubscription.isPresent()) {
			throw new ValidationException("id", id);
		}
		// patch the given subscription into the existing subscription
		SubscriptionDto existingSubscriptionDto = _modelMapper.map(existingSubscription.get(), SubscriptionDto.class);
		SubscriptionDto patchedSubscriptionDto;
		try {
			patchedSubscriptionDto = applyPatchToSubscription(patch, existingSubscriptionDto);
		} catch (JsonPatchException | JsonProcessingException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
		// run validation against the merged subscription. If there is validation errors, it's implied that they
		// come from the partial subscription.
		_validatorService.validate(patchedSubscriptionDto);

		// similar logic flow for PUT. Build UpdateSubscriptionCommand and execute it.
		UpdateSubscriptionCommand cmd = buildUpdateCommand(id, patchedSubscriptionDto);
		Subscription subscription = _triggerService.updateSubscription(cmd);
		return okResponse(obfuscateSecrets(_modelMapper.map(subscription, SubscriptionDto.class)));
	}

	@Timed
	@PreAuthorize("hasRole('idn:trigger-service-subscriptions:delete')")
	@DeleteMapping("/{id}")
	public ResponseEntity unsubscribe(@PathVariable("id") String id) {
		RequestContext rc = RequestContext.ensureGet();
		UnsubscribeCommand cmd = UnsubscribeCommand.builder()
				.tenantId(WebUtil.getCurrentTenantId())
				.subscriptionId(UUID.fromString(id))
				.build();

		_triggerService.unsubscribe(cmd);
		//custom metrics for Tenant Usage Service
		Map<String, Object> customFields = new HashMap<>();
		customFields.put(EVENT_TYPE, "ETS_Unsubscription_Activity");
		customFields.put("SubscriptionID", cmd.getSubscriptionId().toString());
		rc.setAttribute(CUSTOM_FIELDS, customFields, true);
		return noContentResponse();
	}

	@Timed
	@PreAuthorize("hasRole('idn:trigger-service-subscriptions-validate-filter:create')")
	@PostMapping("/validate-filter")
	public ResponseEntity validateFilter(@Valid @RequestBody SubscriptionFilterValidationDto filterValidationDto) {
		boolean isValidPath = JsonPathUtil.isValidPath(filterValidationDto.getFilter());
		boolean isPathExist = false;
		if (isValidPath) {
			String input = JsonUtil.toJson(filterValidationDto.getInput());
			try {
				isPathExist = JsonPathUtil.isPathExist(input, filterValidationDto.getFilter());
			} catch (Exception ignored) {
				isPathExist = false;
			}
		}
		boolean result = isValidPath && isPathExist;
		return ResponseEntity.ok("{ \"isValid\" : " + result + " }");
	}

	@Timed
	@PreAuthorize("hasRole('idn:trigger-service-subscriptions:read')")
	@GetMapping("/export")
	public ResponseEntity<List<ExportedObject<SubscriptionDto>>> exportSubscriptions() {
		final EtsQueryOptions queryOptions = getQueryOptions();
		Page<Subscription> page = listSubscriptions(queryOptions);

		if (isCountHeaderRequested()) {
			return ResponseEntity.ok()
				.header(TOTAL_COUNT_HEADER, String.valueOf((int) page.getTotalElements()))
				.body(page.stream()
					.map(s -> _modelMapper.map(s, SubscriptionDto.class))
					.map(s -> obfuscateSecrets(s))
					.sorted(getComparator(queryOptions.getSorterList(), _comparatorMap))
					.map(s -> toSubscriptionExportDto(s))
					.collect(Collectors.toList()));
		} else {
			return ResponseEntity.ok(
				page.stream()
					.map(s -> _modelMapper.map(s, SubscriptionDto.class))
					.map(s -> obfuscateSecrets(s))
					.sorted(getComparator(queryOptions.getSorterList(), _comparatorMap))
					.map(s -> toSubscriptionExportDto(s))
					.collect(Collectors.toList()));
		}
	}

	@Timed
	@PreAuthorize("hasRole('idn:trigger-service-subscriptions:create')")
	@PostMapping("/import")
	public ResponseEntity<ObjectImportResult> importSubscriptions(@RequestBody List<ExportedObject<SubscriptionDto>> importDto) {
		ObjectImportResult results = new ObjectImportResult();
		List <Message> errors = new ArrayList();
		List <Message> warnings = new ArrayList();
		List <BaseReferenceDto> importedObjects = new ArrayList();
		for(ExportedObject<SubscriptionDto> dto : importDto) {
			try {
				Subscription subscription = validateAndCreate(dto.getObject());
				BaseReferenceDto ref = new BaseReferenceDto();
				ref.setId(subscription.getId().toString());
				ref.setType(DtoType.TRIGGER_SUBSCRIPTION);
				ref.setName(subscription.getName());
				importedObjects.add(ref);
			} catch(Exception e) {
				String msgText = String.format("There was an error importing '%s' with name '%s' and id '%s'", dto.getSelf().getType().name(), dto.getSelf().getName(), dto.getSelf().getId());
				log.warn(msgText, e);
				Message msg = Message.builder()
					.text(msgText + " Exception " + e.toString())
					.build();
				errors.add(msg);
			}
		}
		results.setErrors(errors);
		results.setWarnings(warnings);
		results.setImportedObjects(importedObjects);
		return ResponseEntity.status(HttpStatus.OK).body(results);
	}

	private ExportedObject<SubscriptionDto> toSubscriptionExportDto(SubscriptionDto s) {
		ExportedObject<SubscriptionDto> exportDto = new ExportedObject<>();
		exportDto.setObject(s);
		BaseReferenceDto ref = new BaseReferenceDto();
		ref.setId(s.getId());
		ref.setType(DtoType.TRIGGER_SUBSCRIPTION);
		ref.setName(s.getName());
		exportDto.setSelf(ref);
		exportDto.setVersion(1);
		return exportDto;
	}

	private Page<Subscription> listSubscriptions(EtsQueryOptions options) {
		final List<EtsFilter> filters = Optional.ofNullable(options.getFilter())
			.map(EtsFilter::getFilters)
			.orElseGet(Collections::emptyList);

		Specification<Subscription> spec = Specification.where(
			new SubscriptionSpecification(EtsFilter.eq("tenantId", WebUtil.getCurrentTenantId()))
		);

		for (EtsFilter filter : filters) {
			spec = spec.and(new SubscriptionSpecification(filter));
		}

		Pageable pageable = new OffsetBasedPageRequest(options.getOffset(), options.getLimit(), Sort.by("created").descending().and(Sort.by("id")));

		return _triggerService.listSubscriptions(spec, pageable);
	}

	/**
	 * Helper method to validate subscriptionDto and create a subscription.
	 *
	 * @param subscriptionDto
	 *
	 * @return Subscription which was subscribed
	 */
	private Subscription validateAndCreate(SubscriptionDto subscriptionDto) {
		_validatorService.validate(subscriptionDto);
		SubscriptionType subscriptionType = SubscriptionType.valueOf(subscriptionDto.getType());
		SubscribeCommand cmd = SubscribeCommand.builder()
			.id(UUID.fromString(subscriptionDto.getId()))
			.tenantId(WebUtil.getCurrentTenantId())
			.triggerId(new TriggerId(subscriptionDto.getTriggerId()))
			.type(subscriptionType)
			.responseDeadline(subscriptionDto.getResponseDeadline())
			.config(_validatorService.getAndValidateSubscriptionConfig(subscriptionDto, subscriptionType))
			.filter(subscriptionDto.getFilter())
			.scriptSource(_validatorService.getAndValidateScript(subscriptionDto, subscriptionType))
			.name(subscriptionDto.getName())
			.description(subscriptionDto.getDescription())
			.enabled(subscriptionDto.isEnabled())
			.build();
		return _triggerService.subscribe(cmd);
	}

	/**
	 * Helper method for patching a SubscriptionDto
	 * @param patch
	 * @param targetSubscription
	 *
	 * @return SubscriptionDto
	 */
	private SubscriptionDto applyPatchToSubscription(
		JsonPatch patch, SubscriptionDto targetSubscription) throws JsonPatchException, JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		// this call allows us to avoid an error when deserializing into a SubscriptionDto, the problem
		// being that Duration has no default constructor
		objectMapper.registerModule(new JavaTimeModule());
		JsonNode patched = patch.apply(objectMapper.convertValue(targetSubscription, JsonNode.class));
		return objectMapper.treeToValue(patched, SubscriptionDto.class);
	}

	/**
	 * Helper method for building UpdateSubscriptionCommand for both PUT and PATCH operations to consume
	 * @param subscriptionId
	 * @param subscriptionDto
	 *
	 * @return UpdateSubscriptionCommand to run
	 */
	private UpdateSubscriptionCommand buildUpdateCommand (String subscriptionId, SubscriptionDto subscriptionDto) {
		SubscriptionType subscriptionType = SubscriptionType.valueOf(subscriptionDto.getType());
		UpdateSubscriptionCommand cmd = UpdateSubscriptionCommand.builder()
			.id(UUID.fromString(subscriptionId))
			.tenantId(WebUtil.getCurrentTenantId())
			.triggerId(new TriggerId(subscriptionDto.getTriggerId()))
			.type(subscriptionType)
			.responseDeadline(subscriptionDto.getResponseDeadline())
			.config(_validatorService.getAndValidateSubscriptionConfig(subscriptionDto, subscriptionType))
			.filter(subscriptionDto.getFilter())
			.scriptSource(_validatorService.getAndValidateScript(subscriptionDto, subscriptionType))
			.name(subscriptionDto.getName())
			.description(subscriptionDto.getDescription())
			.enabled(subscriptionDto.isEnabled())
			.build();
		return cmd;
	}

	/**
	 * Helper method to obfuscate secrets when returning subscriptions, particularly those
	 * found in the httpconfig. If there is a basic auth or bearer token configuration, set it
	 * to null and mark that there is auth configured.
	 *
	 * @param subscriptionDto
	 *
	 * @return Subscription which was subscribed
	 */
	private SubscriptionDto obfuscateSecrets (SubscriptionDto subscriptionDto) {
		HttpConfigDto httpConfigDto = subscriptionDto.getHttpConfig();
		if (subscriptionDto.getHttpConfig() != null) {
			switch (httpConfigDto.getHttpAuthenticationType()) {
				case BASIC_AUTH:
					BasicAuthConfigDto basicAuthConfigDto = httpConfigDto.getBasicAuthConfig();
					basicAuthConfigDto.setPassword(null);
					httpConfigDto.setBasicAuthConfig(basicAuthConfigDto);
					break;
				case BEARER_TOKEN:
					BearerTokenAuthConfigDto bearerTokenAuthConfigDto = httpConfigDto.getBearerTokenAuthConfig();
					bearerTokenAuthConfigDto.setBearerToken(null);
					httpConfigDto.setBearerTokenAuthConfig(bearerTokenAuthConfigDto);
					break;
			}
			subscriptionDto.setHttpConfig(httpConfigDto);
		}
		return subscriptionDto;
	}

}
