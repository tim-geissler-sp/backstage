/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.email.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.notification.sender.email.domain.TenantSenderEmail;
import lombok.extern.apachecommons.CommonsLog;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * DynamoDB Repository for {@link TenantSenderEmail}
 */
@Singleton
@CommonsLog
public class DynamoDBTenantSenderEmailRepository implements TenantSenderEmailRepository {

	private static final int BATCH_WRITE_MAX_RETRY = 3;

	private final DynamoDBMapper _mapper;

	@Inject
	public DynamoDBTenantSenderEmailRepository(final AmazonDynamoDB dynamoDB) {
		requireNonNull(dynamoDB, "Expected not null AmazonDynamoDB");

		DynamoDBMapperConfig mapperConfig = DynamoDBMapperConfig.builder()
				.withBatchWriteRetryStrategy(new DynamoDBMapperConfig.DefaultBatchWriteRetryStrategy(BATCH_WRITE_MAX_RETRY))
				.build();

		_mapper = new DynamoDBMapper(dynamoDB, mapperConfig);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<TenantSenderEmail> findAllByTenant(final String tenantId) {
		requireNonNull(tenantId, "Expected not null Tenant ID");

		Map<String, AttributeValue> eav = ImmutableMap.of(":tenantId", new AttributeValue().withS(tenantId));

		DynamoDBQueryExpression<TenantSenderEmail> queryExpression = new DynamoDBQueryExpression<TenantSenderEmail>()
				.withKeyConditionExpression("tenant = :tenantId")
				.withExpressionAttributeValues(eav);

		return _mapper.query(TenantSenderEmail.class, queryExpression);
	}

	@Override
	public Optional<TenantSenderEmail> findByTenantAndId(String tenantId, String id) {
		requireNonNull(tenantId, "Expected not null Tenant ID");
		requireNonNull(id, "Expected not null ID");

		Map<String, AttributeValue> eav = ImmutableMap.of(
				":tenantId", new AttributeValue().withS(tenantId),
				":id", new AttributeValue().withS(id)
		);

		DynamoDBQueryExpression<TenantSenderEmail> queryExpression = new DynamoDBQueryExpression<TenantSenderEmail>()
				.withKeyConditionExpression("tenant = :tenantId and id = :id")
				.withExpressionAttributeValues(eav);

		List<TenantSenderEmail> tenantSenderEmails = _mapper.query(TenantSenderEmail.class, queryExpression);


		if (tenantSenderEmails == null || tenantSenderEmails.size() == 0) {
			return Optional.empty();
		}

		return Optional.ofNullable(tenantSenderEmails.get(0));
	}

	@Override
	public List<TenantSenderEmail> findAllByEmail(String email) {
		requireNonNull(email, "Expected not null email");

		Map<String, AttributeValue> eav = ImmutableMap.of(":email", new AttributeValue().withS(email));

		DynamoDBQueryExpression<TenantSenderEmail> queryExpression = new DynamoDBQueryExpression<TenantSenderEmail>()
				.withIndexName("email-index")
				.withConsistentRead(false)
				.withKeyConditionExpression("email = :email")
				.withExpressionAttributeValues(eav);

		return _mapper.query(TenantSenderEmail.class, queryExpression);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void save(final TenantSenderEmail tenantSenderEmail) {
		_mapper.save(requireNonNull(tenantSenderEmail, "Expected not null TenantSenderEmail"));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void batchSave(final Collection<TenantSenderEmail> tenantSenderEmails) {
		List<DynamoDBMapper.FailedBatch> failedBatch = _mapper.batchSave(requireNonNull(tenantSenderEmails, "Expected not null TenantSenderEmails"));
		if (!failedBatch.isEmpty()) {
			log.error("Batch save failures " + (failedBatch.get(0) != null ? failedBatch.get(0).getUnprocessedItems() : "- unknown cause"));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void delete(String tenantId, String id) {
		requireNonNull(tenantId, "Expected not null Tenant ID");
		requireNonNull(id, "Expected not null ID");

		_mapper.delete(TenantSenderEmail
				.builder()
				.tenant(tenantId)
				.id(id)
				.build());
	}
}
