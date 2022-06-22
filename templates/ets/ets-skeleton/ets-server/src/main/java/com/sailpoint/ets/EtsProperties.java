/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_FAILURE_RATE_THRESHOLD;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_SLIDING_WINDOW_SIZE;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_MINIMUM_NUMBER_OF_CALLS;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_SLIDING_WINDOW_TYPE;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_WAIT_DURATION_IN_OPEN_STATE;


/**
 * ETS Service Properties
 */
@Data
@ConfigurationProperties(prefix="ets")
public class EtsProperties {

	/**
	 * ETS invocation deadline, in minutes, default value 1 hour.
	 */
	private int _deadlineMinutes = 60;
	/**
	 * ETS subscriptions limit for trigger types that support multiple subscriptions
	 */
	private int _subscriptionLimit = 50;
	/**
	 * ETS subscriptions limit for SCRIPT subscriptions
	 */
	private int _scriptSubscriptionLimit = 10;
	/**
	 * ETS script byte size limit for SCRIPT subscriptions
	 */
	private int _scriptByteSizeLimit = 1000000;
	/**
	 * ETS invocations lambda function name
	 */
	private String _lambdaNamePrefix;

	/**
	 * File path for json triggers repo using json schemas.
	 * By default we are using repo from resources but can be set as external file.
	 */
	private String _jsonTriggersRepoFilePathJsonSchema;

	/**
	 * ETS ARN Key in KMS.
	 */
	private String _kmsKeyArn;
	/**
	 * Domain portion of the Partner Event Source URL
	 */
	private String _eventBridgePartnerEventSourcePrefix;

	/**
	 * Custom configuration for Circuit Breakers.
	 * A {@link CircuitBreakerConfig} configures a {@link CircuitBreaker}
	 */
	private float _failureRateThreshold = DEFAULT_FAILURE_RATE_THRESHOLD;
	private int _slidingWindowSize = DEFAULT_SLIDING_WINDOW_SIZE;
	private int _minimumNumberOfCalls = DEFAULT_MINIMUM_NUMBER_OF_CALLS;
	private CircuitBreakerConfig.SlidingWindowType _slidingWindowType = DEFAULT_SLIDING_WINDOW_TYPE;
	private int _permittedNumberOfCallsInHalfOpenState = DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE;
	private int _waitDurationInOpenState = DEFAULT_WAIT_DURATION_IN_OPEN_STATE;
	private boolean _automaticTransitionFromOpenToHalfOpenEnabled = false;
}
