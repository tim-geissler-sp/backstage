/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.domain.command;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.sailpoint.atlas.boot.core.web.SailPointHeaders;
import com.sailpoint.ets.EtsProperties;
import com.sailpoint.ets.domain.Secret;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.event.EventPublisher;
import com.sailpoint.ets.domain.event.TriggerWorkflowEvent;
import com.sailpoint.ets.domain.invocation.InvocationCallbackUrlProvider;
import com.sailpoint.ets.domain.subscription.ScriptLanguageType;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.domain.trigger.EtsFeatureStore;
import com.sailpoint.ets.domain.trigger.TriggerType;
import com.sailpoint.ets.infrastructure.aws.HttpInvocationPayload;
import com.sailpoint.ets.infrastructure.aws.InvocationPayload;
import com.sailpoint.ets.infrastructure.aws.Invoker;
import com.sailpoint.ets.infrastructure.aws.MetadataPayload;
import com.sailpoint.ets.infrastructure.aws.ScriptInvocationPayload;
import com.sailpoint.ets.infrastructure.util.CamelCaseUtil;
import com.sailpoint.ets.infrastructure.util.HTTPConfigConverter;
import com.sailpoint.ets.infrastructure.util.ScriptConfigConverter;
import com.sailpoint.ets.infrastructure.web.dto.HttpConfigDto;
import com.sailpoint.ets.infrastructure.web.dto.ResponseMode;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.utils.CircuitBreakerUtil;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;

import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.sailpoint.ets.infrastructure.util.EventBridgeConfigConverter.AWS_PARTNER_EVENT_SOURCE_NAME;
import static com.sailpoint.ets.infrastructure.util.EventBridgeConfigConverter.AWS_REGION;
import static com.sailpoint.ets.infrastructure.util.HTTPConfigConverter.SUBSCRIPTION_ID;
import static com.sailpoint.ets.infrastructure.util.MetricsReporter.reportResponseMode;
import static com.sailpoint.ets.infrastructure.util.MetricsReporter.reportTriggerType;
import static com.sailpoint.ets.infrastructure.util.ScriptConfigConverter.LANGUAGE;
import static com.sailpoint.ets.infrastructure.util.TriggerEventLogUtil.dispatchInvocationLog;
import static com.sailpoint.ets.infrastructure.util.WorkflowConfigConverter.WORKFLOW_ID;

/*
 * Class DispatchInvocationCommand.
 */
@Value
@Builder
@CommonsLog
public class DispatchInvocationCommand {
	private final static String AUTHORIZATION = "Authorization";
	private final static String BASIC = "Basic ";
	private final static String BEARER = "Bearer ";
	private final static String RESPONSE_MODE = "responseMode";
	private final static String METADATA = "_metadata";
	private final static String TRIGGER_ID = "triggerId";
	private final static String TRIGGER_TYPE = "triggerType";
	private final static String CALLBACK_URL = "callbackURL";
	private final static String SECRET = "secret";
	private final static String INVOCATION_ID = "invocationId";

	@NonNull private final TenantId _tenantId;
	@NonNull private final String _triggerId;
	@NonNull private final String _requestId;
	@NonNull private final UUID _invocationId;
	@NonNull private final Secret _secret;
	@NonNull private final TriggerType _triggerType;
	@NonNull private final String _subscriptionId;
	@NonNull private final SubscriptionType _subscriptionType;
	@NonNull private final Map<String, Object> _subscriptionConfig;
	@NonNull private final Map<String, String> _headers;
	private final Map<String, Object> _input;
	private final String scriptSource;

	/*
	 * Handle Dispatch Invocation Command.
	 */
	public void handle(Invoker invoker, InvocationCallbackUrlProvider invocationCallbackUrlProvider,
					   EtsProperties properties, HTTPConfigConverter converter, ScriptConfigConverter scriptConfigConverter,
					   CircuitBreaker circuitBreaker, ObjectMapper objectMapper, EventPublisher publisher, EtsFeatureStore flagEtsFeatureStore) {

		// Check circuit breaker and make sure we can dispatch the invocation
		CircuitBreakerUtil.isCallPermitted(circuitBreaker);
		ResponseMode responseMode;
		String invocationFunctionName;
		InvocationPayload.InvocationPayloadBuilder invocationPayloadBuilder;
		final Map<String, String> encryptionContext = Collections.singletonMap(SUBSCRIPTION_ID, _subscriptionId);

		// Build subscription type specific attributes
		switch(_subscriptionType) {
			case HTTP:
				HttpConfigDto configDto = converter.convertToHttpConfigDto(_subscriptionConfig, encryptionContext);

				invocationPayloadBuilder = HttpInvocationPayload.builder()
					.url(configDto.getUrl())
					.headers(getHeaders(configDto));

				responseMode = configDto.getHttpDispatchMode();
				invocationFunctionName = properties.getLambdaNamePrefix() + "http";

				Map<String, String> metadataMap = new HashMap<>();
				metadataMap.put(TRIGGER_ID, _triggerId);
				metadataMap.put(TRIGGER_TYPE, CamelCaseUtil.toCamelCase(_triggerType));

				if(_triggerType == TriggerType.FIRE_AND_FORGET){
					metadataMap.put(INVOCATION_ID, _invocationId.toString());
				}
				else {
					metadataMap.put(RESPONSE_MODE, CamelCaseUtil.toCamelCase(responseMode));
					if(responseMode != ResponseMode.SYNC){
						metadataMap.put(CALLBACK_URL, invocationCallbackUrlProvider.getCallbackUrl(_tenantId, _invocationId));
						metadataMap.put(SECRET, _secret.getValue());
					}
				}

				//adding to existing InvocationPayload
				invocationPayloadBuilder.metadata(metadataMap);

				break;
			case SCRIPT:
				if (ScriptLanguageType.valueOf(_subscriptionConfig.get(LANGUAGE).toString()) != ScriptLanguageType.JAVASCRIPT) {
					throw new UnsupportedOperationException("unsupported script language: " + _subscriptionConfig.get(LANGUAGE).toString());
				}

				invocationPayloadBuilder = ScriptInvocationPayload.builder()
					.scriptCode(Base64.getEncoder().encodeToString(scriptConfigConverter.decrypt(scriptSource, encryptionContext).getBytes()))
					.headers(ImmutableMap.of(SailPointHeaders.REQUEST_ID_HEADER, _requestId));

				responseMode = ResponseMode.valueOf(_subscriptionConfig.get(RESPONSE_MODE).toString());
				invocationFunctionName = properties.getLambdaNamePrefix() + "script-js";
				break;
			case EVENTBRIDGE:

				// Add common attributes to all invocations
				MetadataPayload.MetadataPayloadBuilder metadataPayloadBuilder = MetadataPayload.builder()
					.invocationId(_invocationId.toString())
					.triggerId(_triggerId)
					.triggerType(CamelCaseUtil.toCamelCase(_triggerType));

				// Add request-response specific attributes
				if(_triggerType == TriggerType.REQUEST_RESPONSE) {
					metadataPayloadBuilder
						.callbackURL(invocationCallbackUrlProvider.getCallbackUrl(_tenantId, _invocationId))
						.secret(_secret.getValue());
				}

				// Set metadata
				Map<String, Object> metadata = objectMapper.convertValue(metadataPayloadBuilder.build(), new TypeReference<Map<String, Object>>() {});
				metadata.values().removeIf(Objects::isNull);
				_input.put(METADATA, metadata);

				// Invoke trigger and log results
				invoker.sendPartnerEvent(_subscriptionConfig.get(AWS_REGION).toString(),
					_subscriptionConfig.get(AWS_PARTNER_EVENT_SOURCE_NAME).toString(), _triggerId, _input);
				reportTriggerType(_triggerType);

				return;
			case WORKFLOW:
				handleWorkflowSubscriptionType(invocationCallbackUrlProvider, publisher, invoker, flagEtsFeatureStore);
				return;
			default:
				throw new UnsupportedOperationException("unsupported subscription type: " + _subscriptionType);
		}

		// Add request-response specific attributes
		if(_triggerType == TriggerType.REQUEST_RESPONSE) {
			invocationPayloadBuilder
				.callbackUrl(invocationCallbackUrlProvider.getCallbackUrl(_tenantId, _invocationId))
				.secret(_secret.getValue())
				.responseMode(CamelCaseUtil.toCamelCase(responseMode));
		}

		// Add common attributes to payloads of all subscription types
		invocationPayloadBuilder
			.triggerId(_triggerId)
			.triggerType(CamelCaseUtil.toCamelCase(_triggerType))
			.invocationId(_invocationId.toString())
			.input(_input);

		// Dispatch invocation
		InvocationPayload payload = invocationPayloadBuilder.build();
		log.info(dispatchInvocationLog("Dispatching invocation to Lambda.", _subscriptionId, _subscriptionType, payload));
		invoker.invokeLambdaFunction(invocationFunctionName, payload);

		reportTriggerType(_triggerType);
		reportResponseMode(responseMode);
	}

	private void handleWorkflowSubscriptionType(InvocationCallbackUrlProvider invocationCallbackUrlProvider, EventPublisher publisher, Invoker invoker, EtsFeatureStore flagEtsFeatureStore) {
		HashMap<String, Object> runMetadata = new HashMap<>();
		runMetadata.put("invocationId", _invocationId.toString());
		runMetadata.put("subscriptionId", _subscriptionId);
		runMetadata.put("triggerType", _triggerType.toString());
		if(_triggerType == TriggerType.REQUEST_RESPONSE) {
			runMetadata.put("callbackUrl", invocationCallbackUrlProvider.getCallbackUrl(_tenantId, _invocationId));
			runMetadata.put("secret", _secret.getValue());
		}
		Map inputShallowCopy = new HashMap(_input);
		inputShallowCopy.putIfAbsent("_meta", runMetadata);

		TriggerWorkflowEvent event = TriggerWorkflowEvent.builder()
			.workflowId((String) _subscriptionConfig.get(WORKFLOW_ID))
			.requestId(_requestId)
			.tenantId(_tenantId.toString())
			.triggerId(_triggerId)
			.workflowId(_subscriptionConfig.get(WORKFLOW_ID).toString())
			.input(inputShallowCopy)
			.headers(_headers)
			.build();

		publisher.publish(event);
		log.info("Workflow execution event published for workflow id " + _subscriptionConfig.get(WORKFLOW_ID));
		reportTriggerType(_triggerType);
	}

	/**
	 * Get headers for HTTP invocations
	 * @param configDto the http config
	 * @return a map of headers
	 */
	private Map<String, String> getHeaders(HttpConfigDto configDto) {
		Map<String, String> headers = new HashMap<>();
		headers.put(SailPointHeaders.REQUEST_ID_HEADER, _requestId);
		switch(configDto.getHttpAuthenticationType()) {
			case BASIC_AUTH:
				String basicHeader = configDto.getBasicAuthConfig() != null ? configDto.getBasicAuthConfig().getUserName() +
					":" + configDto.getBasicAuthConfig().getPassword() : null;
				if(basicHeader != null) {
					headers.put(AUTHORIZATION, BASIC
						+ Base64.getEncoder().encodeToString(basicHeader.getBytes()));
				}
				break;
			case BEARER_TOKEN:
				String bearerHeader = configDto.getBearerTokenAuthConfig() != null ? configDto.getBearerTokenAuthConfig()
					.getBearerToken() : null;
				if(bearerHeader != null) {
					headers.put(AUTHORIZATION, BEARER
						+ bearerHeader);
				}
				break;
		}
		return headers;
	}

}
