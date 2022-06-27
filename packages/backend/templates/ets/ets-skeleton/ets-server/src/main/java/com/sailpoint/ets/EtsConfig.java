/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.eventbridge.AmazonEventBridge;
import com.amazonaws.services.eventbridge.AmazonEventBridgeClientBuilder;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sailpoint.atlas.boot.core.util.AwsEncryptionService;
import com.sailpoint.atlas.util.AwsEncryptionServiceUtil;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

/**
 * ETS Configuration.
 */
@Configuration
@CommonsLog
public class EtsConfig {

	@Bean
	CircuitBreakerConfig circuitBreakerConfig(EtsProperties etsProperties) {
		CircuitBreakerConfig.Builder builder = CircuitBreakerConfig.custom()
			.failureRateThreshold(etsProperties.getFailureRateThreshold())
			.waitDurationInOpenState(Duration.ofSeconds(etsProperties.getWaitDurationInOpenState()))
			.slidingWindow(etsProperties.getSlidingWindowSize(), etsProperties.getMinimumNumberOfCalls(), etsProperties.getSlidingWindowType())
			.permittedNumberOfCallsInHalfOpenState(etsProperties.getPermittedNumberOfCallsInHalfOpenState());

		if(etsProperties.isAutomaticTransitionFromOpenToHalfOpenEnabled()) {
			builder.enableAutomaticTransitionFromOpenToHalfOpen();
		}
		return builder.build();
	}

	@Bean
	public AWSLambda awsLambda(Regions currentRegion) {
		return AWSLambdaClientBuilder.standard()
			.withRegion(currentRegion)
			.build();
	}

	@Bean
	public LoadingCache<String, AmazonEventBridge> regionalEventBridge() {
		return CacheBuilder.newBuilder().build(new CacheLoader<String, AmazonEventBridge>() {
			@Override
			public AmazonEventBridge load(String region) {
				return AmazonEventBridgeClientBuilder.standard().withRegion(region).build();
			}
		});
	}

	@Bean
	public ModelMapper modelMapper() {
		return new ModelMapper();
	}

	@Bean
	public AwsEncryptionServiceUtil awsEncryptionServiceUtil(AwsEncryptionService awsEncryptionService,
															 EtsProperties etsProperties) {
		try {
			return awsEncryptionService.getAwsEncryptionServiceUtil(etsProperties.getKmsKeyArn());
		} catch (ExecutionException e) {
			log.error("Error create AwsEncryptionServiceUtil", e);
			throw new IllegalStateException(e);
		}
	}
}
