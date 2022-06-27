/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.trigger;

import com.sailpoint.ets.domain.invocation.Invocation;
import com.sailpoint.ets.domain.status.InvocationType;
import com.sailpoint.ets.domain.subscription.Subscription;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Trigger
 */
@Getter
@Builder
@CommonsLog
public class Trigger {
	@NonNull
	private final TriggerId _id;
	@NonNull
	private final TriggerName _name;

	@NonNull
	private final TriggerType _type;

	private final TriggerDescription _description;

	private final List<EventSource> _eventSources;

	@NonNull
	private final Schema _inputSchemaObject;

	@NonNull
	private final Map<String, Object> _inputSchema;

	private final Schema _outputSchemaObject;

	private final Map<String, Object> _outputSchema;

	@NonNull
	@Builder.Default
	private final Duration _timeout = Duration.ofMinutes(5);

	@NonNull
	private final Map<String, Object> _exampleInput;

	private final Map<String, Object> _exampleOutput;

	public Invocation invoke(Subscription subscription, Map<String, Object> input, Map<String, Object> context, int deadline, InvocationType type) {
		_inputSchemaObject.processData(input);
		return subscription.createInvocation(context, deadline, type);
	}

	public void validateOutput(Map<String, Object> output) {
		_outputSchemaObject.processData(output);
	}

	public void validateInput(Map<String, Object> input) {
		_inputSchemaObject.validateData(input);
	}


	/**
	 * Check if the trigger is enabled(visible) to the tenant
	 *
	 * @param etsFeatureStore feature flag service to check trigger enablement
	 * @return true if the trigger is enabled(visible) to the tenant. False otherwise.
	 */
	public boolean isEnabledForTenant(EtsFeatureStore etsFeatureStore) {
		return etsFeatureStore.isEnabledForTenant(_id);
	}

}
