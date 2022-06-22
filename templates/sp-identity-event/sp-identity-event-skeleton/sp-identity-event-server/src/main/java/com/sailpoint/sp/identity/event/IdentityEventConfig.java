/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.sailpoint.atlas.boot.core.beacon.BeaconIntegration;
import com.sailpoint.atlas.boot.event.EventRegistry;
import com.sailpoint.atlas.boot.event.EventService;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import com.sailpoint.sp.identity.event.domain.IdentityEventPublisher;
import com.sailpoint.sp.identity.event.domain.IdentityStateJpaRepository;
import com.sailpoint.sp.identity.event.domain.IdentityStateRepository;
import com.sailpoint.sp.identity.event.domain.service.IdentityEventDebugService;
import com.sailpoint.sp.identity.event.domain.service.IdentityEventPublishService;
import com.sailpoint.sp.identity.event.domain.service.IdentityEventService;
import com.sailpoint.sp.identity.event.infrastructure.DualWriterIdentityStateRepository;
import com.sailpoint.sp.identity.event.infrastructure.IdentityEventHandlers;
import com.sailpoint.sp.identity.event.infrastructure.InstrumentedIdentityStateRepository;
import com.sailpoint.sp.identity.event.infrastructure.KafkaIdentityEventPublisher;
import com.sailpoint.sp.identity.event.infrastructure.OrgLifecycleEventHandler;
import com.sailpoint.sp.identity.event.infrastructure.PostgresIdentityStateRepository;
import com.sailpoint.sp.identity.event.infrastructure.dynamos3.DynamoS3IdentityStateRepository;
import com.sailpoint.sp.identity.event.infrastructure.redis.debug.RedisIdentityEventPublisher;
import com.sailpoint.sp.identity.event.infrastructure.s3.S3Client;
import com.sailpoint.utilities.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;

/**
 * IdentityEventConfig
 */
@Configuration
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@CommonsLog
public class IdentityEventConfig {

	public static final String IDENTITY_CHANGED = "IDENTITY_CHANGED";
	public static final String IDENTITY_DELETED = "IDENTITY_DELETED";
	public static final String ORG_DELETED = "ORG_DELETED";

	private final EventRegistry _eventRegistry;
	private final String POSTGRES = "pg";
	private final String DYNAMOS3 = "dynamo-s3";

	@PostConstruct
	public void registerEventHandlers() {
		_eventRegistry.register(IdnTopic.IDENTITY, IDENTITY_CHANGED, IdentityEventHandlers.class);
		_eventRegistry.register(IdnTopic.IDENTITY, IDENTITY_DELETED, IdentityEventHandlers.class);

		_eventRegistry.register(IdnTopic.ORG_LIFECYCLE, ORG_DELETED, OrgLifecycleEventHandler.class);
	}

	@Bean
	public IdentityEventPublisher identityEventPublisher(EventService eventService, FeatureFlagService featureFlagService) {
		return new KafkaIdentityEventPublisher(eventService, featureFlagService);
	}

	@Bean
	public IdentityStateRepository identityStateRepository(IdentityEventProperties properties,
														   AmazonDynamoDB amazonDynamoDB,
														   IdentityStateJpaRepository identityStateJpaRepository,
														   FeatureFlagService featureFlagService,
														   TransferManager transferManager
														   ) {

		IdentityStateRepository pgRepository = new InstrumentedIdentityStateRepository(POSTGRES,
			new PostgresIdentityStateRepository(identityStateJpaRepository), featureFlagService);

		String bucketName = properties.getIdentityStateBucketName();
		String dynamoTableName = properties.getIdentityStateDynamoTable();

		log.info(String.format("bucket name: %s dynamo table name: %s", bucketName, dynamoTableName));

		if (StringUtil.isNullOrEmpty(bucketName) || StringUtil.isNullOrEmpty(dynamoTableName)) {
			log.warn("using postgres for identity state repository because either bucket name or dynamo table name are null/empty");
			return pgRepository;
		}

		try {
			log.info("using dual writer identity state repository");
			S3Client s3 = new S3Client(bucketName, transferManager);
			IdentityStateRepository dynamoRepository = new InstrumentedIdentityStateRepository(DYNAMOS3,
				new DynamoS3IdentityStateRepository(amazonDynamoDB, dynamoTableName, s3, 1200), featureFlagService);

			return new DualWriterIdentityStateRepository(pgRepository, dynamoRepository, featureFlagService);
		} catch (Exception e) {
			log.error("using postgres for identity state repository because dual writer failed to create", e);
			return pgRepository;
		}
	}

	@Bean
	public IdentityEventService identityEventService(IdentityStateRepository identityStateRepository,
													 IdentityEventPublishService identityEventPublishService) {
		return new IdentityEventService(identityStateRepository, identityEventPublishService);
	}

	@Bean
	public IdentityEventDebugService identityEventDebugService(IdentityStateRepository identityStateRepository,
															   RedisPool redisPool,
															   FeatureFlagService featureFlagService) {
		return new IdentityEventDebugService(identityStateRepository, new IdentityEventPublishService(new RedisIdentityEventPublisher(redisPool), featureFlagService));
	}

	@Bean
	public IdentityEventPublishService identityEventPublishService(IdentityEventPublisher identityEventPublisher,
																   FeatureFlagService featureFlagService) {
		return new IdentityEventPublishService(identityEventPublisher, featureFlagService);
	}

	@Bean
	@Profile("!production")
	public TransferManager devTransferManager(AWSCredentials awsCredentials,
										   AwsClientBuilder.EndpointConfiguration endpointConfiguration,
										   BeaconIntegration integration) {
		AmazonS3 amazonS3;
		if (!integration.isBeaconEnabled()) {
			amazonS3 = AmazonS3ClientBuilder
				.standard()
				.withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
				.withEndpointConfiguration(endpointConfiguration)
				.build();
		} else {
			amazonS3 = AmazonS3ClientBuilder
				.standard()
				.build();
		}
		return TransferManagerBuilder.standard().withS3Client(amazonS3).build();
	}

	@Bean
	@Profile("production")
	public TransferManager prodTransferManager() {
		return TransferManagerBuilder.standard().build();
	}

}
