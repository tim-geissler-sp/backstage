/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.aws;

import com.amazonaws.services.eventbridge.AmazonEventBridge;
import com.amazonaws.services.eventbridge.model.CreatePartnerEventSourceRequest;
import com.amazonaws.services.eventbridge.model.CreatePartnerEventSourceResult;
import com.amazonaws.services.eventbridge.model.DeletePartnerEventSourceRequest;
import com.amazonaws.services.eventbridge.model.PutPartnerEventsRequest;
import com.amazonaws.services.eventbridge.model.PutPartnerEventsRequestEntry;
import com.amazonaws.services.eventbridge.model.PutPartnerEventsResult;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.sailpoint.atlas.idn.RestClientProvider;
import com.sailpoint.ets.exception.ValidationException;
import com.sailpoint.metrics.annotation.Metered;
import com.sailpoint.utilities.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * A composed invoker used in dispatch invocation
 */
@Component
@CommonsLog
@RequiredArgsConstructor(onConstructor_={@Autowired})
public class Invoker implements EventBridge {
	private final AWSLambda _awsLambda;
	private final LoadingCache<String, AmazonEventBridge> _regionalEventBridge;
	private final RestClientProvider _restClientProvider;

	/**
	 * Invoke Aws lambda function
	 * @param name the name of the lambda function
	 * @param payload the payload to be sent to lambda
	 */
	@Metered
	public void invokeLambdaFunction(String name, Object payload) {
		InvokeRequest req = new InvokeRequest()
				.withFunctionName(name)
				.withInvocationType(InvocationType.Event)
				.withPayload(JsonUtil.toJson(payload));

		InvokeResult result = _awsLambda.invoke(req);

		Map<String, String> extraLogs = ImmutableMap.of("statusCode", result.getStatusCode().toString());
		if (HttpStatus.valueOf(result.getStatusCode()).is2xxSuccessful()) {
			log.info(JsonUtil.toJson(buildInvocationResultLog("Lambda invocation dispatch succeeded.", payload, extraLogs)));
		} else {
			log.error(JsonUtil.toJson(buildInvocationResultLog("Lambda invocation dispatch failed.", payload, extraLogs)));
		}
	}

	/**
	 * Send an EventBridge event to partner's event source
	 * @param region The region where the event source is created
	 * @param source The name of the event source
	 * @param detailType The detail type of the event
	 * @param payload The event body
	 */
	@Metered
	public void sendPartnerEvent(String region, String source, String detailType, Object payload) {
		String pay = JsonUtil.toJson(payload);
		PutPartnerEventsRequest req = new PutPartnerEventsRequest()
			.withEntries(new PutPartnerEventsRequestEntry()
				.withSource(source)
				.withDetailType(detailType)
				.withDetail(pay));

		AmazonEventBridge amazonEventBridge;
		try {
			amazonEventBridge= _regionalEventBridge.get(region);
		} catch (ExecutionException e) {
			throw new IllegalArgumentException("Failed to get event bridge client", e);
		}

		PutPartnerEventsResult result = amazonEventBridge.putPartnerEvents(req);

		if (result.getFailedEntryCount() == 0) {
			log.info(JsonUtil.toJson(buildInvocationResultLog("Event bridge invocation dispatch succeeded.",
				payload, ImmutableMap.of("eventBridgeEventId", result.getEntries().get(0).getEventId()))));
		} else {
			log.warn(JsonUtil.toJson(buildInvocationResultLog("Event bridge invocation dispatch failed.",
				payload, ImmutableMap.of("eventBridgeErrorMessage", result.getEntries().get(0).getErrorMessage()))));
		}
	}

	/**
	 * Sanitized map used for logging
	 *
	 * @param message Log message
	 * @param payload Trigger invocation payload
	 * @param extraLogs Extra logs to be appended
	 * @return Log as a Map to be serialized to JSON
	 */
	private static Map<String, String> buildInvocationResultLog(String message, Object payload, Map<String, String> extraLogs) {
		String invocationId = payload instanceof InvocationPayload ?
			((InvocationPayload) payload).getInvocationId() : "unreadable";

		Map<String, String> baseLog = ImmutableMap.of(
			"message", message,
			"invocationId", invocationId
		);

		return ImmutableMap.<String, String>builder().putAll(baseLog).putAll(extraLogs).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String createPartnerEventSource(String account, String region, String name) {
		CreatePartnerEventSourceRequest req = new CreatePartnerEventSourceRequest()
			.withName(name)
			.withAccount(account);
		CreatePartnerEventSourceResult res = getCachedEventBridgeClient(region).createPartnerEventSource(req);

		return res.getEventSourceArn();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deletePartnerEventSource(String account, String region, String name) {
		DeletePartnerEventSourceRequest request = new DeletePartnerEventSourceRequest()
			.withName(name)
			.withAccount(account);

		getCachedEventBridgeClient(region).deletePartnerEventSource(request);
	}

	/**
	 * Get cached event bridge client for the given region
	 * @param region the AWS region
	 * @return the event bridge client in that region
	 */
	private AmazonEventBridge getCachedEventBridgeClient(String region) {
		try {
			return _regionalEventBridge.get(region);
		} catch (Exception e) {
			throw new ValidationException("region", region);
		}
	}

}

