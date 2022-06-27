/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.util;

import com.sailpoint.ets.domain.event.InvocationCompletedEvent;
import com.sailpoint.ets.domain.event.InvocationCompletedTestEvent;
import com.sailpoint.ets.domain.event.InvocationFailedEvent;
import com.sailpoint.ets.domain.event.InvocationFailedTestEvent;
import com.sailpoint.ets.domain.event.TriggerInvokedEvent;
import com.sailpoint.ets.domain.status.CompleteInvocationInput;
import com.sailpoint.ets.domain.status.InvocationStatus;
import com.sailpoint.ets.domain.status.InvocationType;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.domain.trigger.TriggerId;
import com.sailpoint.ets.domain.trigger.TriggerType;
import com.sailpoint.ets.infrastructure.aws.HttpInvocationPayload;
import com.sailpoint.ets.infrastructure.aws.InvocationPayload;
import com.sailpoint.utilities.JsonUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class for creating sanitized JSON log for trigger events
 */
public class TriggerEventLogUtil {

	/**
	 * Get sanitized JSON log of {@link TriggerInvokedEvent}
	 *
	 * @param message             Log message
	 * @param triggerInvokedEvent TriggerInvokedEvent
	 * @return Sanitized JSON log
	 */
	public static String logTriggerInvokedEvent(String message, TriggerInvokedEvent triggerInvokedEvent) {
		Map<String, String> log = new LinkedHashMap<>();
		log.put("message", message);
		log.put("invocationId", triggerInvokedEvent.getInvocationId());
		log.put("invocationType", triggerInvokedEvent.getInvocationType().toString());
		log.put("triggerId", triggerInvokedEvent.getTriggerId());
		log.put("triggerType", CamelCaseUtil.toCamelCase(triggerInvokedEvent.getType()));
		log.put("tenantId", triggerInvokedEvent.getTenantId());
		log.put("subscriptionId", triggerInvokedEvent.getSubscriptionId());
		log.put("subscriptionType", CamelCaseUtil.toCamelCase(triggerInvokedEvent.getSubscriptionType()));

		return JsonUtil.toJson(log);
	}

	/**
	 * Get sanitized JSON log of {@link InvocationCompletedEvent}
	 *
	 * @param message                  Log message
	 * @param invocationCompletedEvent InvocationCompletedEvent
	 * @return Sanitized JSON log
	 */
	public static String logInvocationCompletedEvent(String message, InvocationCompletedEvent invocationCompletedEvent) {
		Map<String, String> log = new LinkedHashMap<>();
		log.put("message", message);
		log.put("invocationId", invocationCompletedEvent.getInvocationId());
		log.put("invocationType", invocationCompletedEvent instanceof InvocationCompletedTestEvent ? InvocationType.TEST.toString() :
			InvocationType.REAL_TIME.toString());
		log.put("triggerId", invocationCompletedEvent.getTriggerId());
		log.put("tenantId", invocationCompletedEvent.getTenantId());
		return JsonUtil.toJson(log);
	}

	/**
	 * Get sanitized JSON log of {@link InvocationFailedEvent} with default message
	 *
	 * @param invocationFailedEvent InvocationFailedEvent
	 * @return Sanitized JSON log
	 */
	public static String logInvocationFailedEvent(InvocationFailedEvent invocationFailedEvent) {
		return logInvocationFailedEvent("Trigger invocation completion failed.", invocationFailedEvent);
	}

	/**
	 * Get sanitized JSON log of {@link InvocationFailedEvent}
	 *
	 * @param message               Log message
	 * @param invocationFailedEvent InvocationFailedEvent
	 * @return Sanitized JSON log
	 */
	public static String logInvocationFailedEvent(String message, InvocationFailedEvent invocationFailedEvent) {
		Map<String, String> log = new LinkedHashMap<>();
		log.put("message", message);
		log.put("invocationId", invocationFailedEvent.getInvocationId());
		log.put("invocationType", invocationFailedEvent instanceof InvocationFailedTestEvent ? InvocationType.TEST.toString() :
			InvocationType.REAL_TIME.toString());
		log.put("triggerId", invocationFailedEvent.getTriggerId());
		log.put("tenantId", invocationFailedEvent.getTenantId());
		log.put("reason", invocationFailedEvent.getReason());

		return JsonUtil.toJson(log);
	}

	/**
	 * Get sanitized JSON log of {@link InvocationStatus} when trigger invocation starts.
	 *
	 * @param message          Log message
	 * @param invocationStatus InvocationStatus
	 * @return Sanitized JSON log
	 */
	public static String logCreateInvocationStatus(String message, InvocationStatus invocationStatus) {
		Map<String, String> log = new LinkedHashMap<>();
		log.put("message", message);
		log.put("invocationId", invocationStatus.getId().toString());
		log.put("invocationType", invocationStatus.getType().toString());
		log.put("triggerId", invocationStatus.getTriggerId().toString());
		log.put("tenantId", invocationStatus.getTenantId().toString());
		log.put("subscriptionId", invocationStatus.getSubscriptionId().toString());
		log.put("created", invocationStatus.getCreated().toString());

		return JsonUtil.toJson(log);
	}

	/**
	 * Get sanitized JSON log of {@link CompleteInvocationInput} with invocation ID when trigger invocation completes.
	 *
	 * @param message                 Log message
	 * @param invocationId            Invocation ID
	 * @param completeInvocationInput CompleteInvocationInput
	 * @return Sanitized JSON log
	 */
	public static String logCompleteInvocationStatus(String message, String invocationId, CompleteInvocationInput completeInvocationInput) {
		Map<String, String> log = new LinkedHashMap<>();
		log.put("message", message);
		log.put("invocationId", invocationId);
		log.put("error", completeInvocationInput.getError());

		return JsonUtil.toJson(log);
	}

	/**
	 * Get sanitized JSON log of specified exception with Invocation ID.
	 *
	 * @param message      Log message
	 * @param invocationId Invocation ID
	 * @param e            Exception
	 * @return Sanitized JSON log
	 */
	public static String logExceptionWithInvocationId(String message, String invocationId, Exception e) {
		Map<String, String> log = new LinkedHashMap<>();
		log.put("message", message);
		log.put("invocationId", invocationId);
		log.put("exception", e.toString());

		return JsonUtil.toJson(log);
	}

	/**
	 * Get sanitized JSON log of exception, triggerId, and tenantId.
	 *
	 * @param message      	Log message
	 * @param triggerId    	Id of trigger used
	 * @param tenantId		tenantId (org + pod)
	 * @param e            	Exception
	 * @return Sanitized JSON log
	 */
	public static String logExceptionWithTriggerAndTenantId(String message, TriggerId triggerId, String tenantId, Exception e) {
		Map<String, String> log = new LinkedHashMap<>();
		log.put("message", message);
		log.put("tenantId", tenantId);
		log.put("triggerId", triggerId.toString());
		log.put("exception", e.toString());
		return JsonUtil.toJson(log);
	}

	/**
	 * Get sanitized JSON log of {@link InvocationPayload} sent to Lambda.
	 *
	 * @param message Log message
	 * @param subscriptionId the subscription id of the invocation
	 * @param subscriptionType the subscription type of the invocation
	 * @param payload Invocation payload to lambda
	 * @return Log in JSON string format
	 */
	public static String dispatchInvocationLog(String message, String subscriptionId,
															SubscriptionType subscriptionType, InvocationPayload payload) {
		Map<String, Object> dispatchLog = new LinkedHashMap<>();
		dispatchLog.put("message", message);
		dispatchLog.put("invocationId", payload.getInvocationId());
		dispatchLog.put("triggerType", payload.getTriggerType());
		dispatchLog.put("subscriptionId", subscriptionId);
		dispatchLog.put("subscriptionType",CamelCaseUtil.toCamelCase(subscriptionType));

		if (CamelCaseUtil.toCamelCase(TriggerType.REQUEST_RESPONSE).equals(payload.getTriggerType())) {
			dispatchLog.put("callbackUrl", payload.getCallbackUrl());
			dispatchLog.put("responseMode", payload.getResponseMode());
		}

		if (payload instanceof HttpInvocationPayload) {
			dispatchLog.put("url", ((HttpInvocationPayload) payload).getUrl());
		}

		return JsonUtil.toJson(dispatchLog);
	}

}
