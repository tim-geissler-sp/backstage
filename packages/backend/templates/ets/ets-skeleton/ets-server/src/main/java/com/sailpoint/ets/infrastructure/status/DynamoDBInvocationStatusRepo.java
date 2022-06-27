/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.status;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.google.common.collect.ImmutableMap;
import com.sailpoint.atlas.util.StringUtil;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.status.CompleteInvocationInput;
import com.sailpoint.ets.domain.status.InvocationStatus;
import com.sailpoint.ets.domain.status.InvocationStatusRepo;
import com.sailpoint.ets.domain.status.InvocationType;
import com.sailpoint.ets.domain.status.StartInvocationInput;
import com.sailpoint.ets.domain.trigger.TriggerId;
import lombok.AllArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * DynamoDB implementation for InvocationStatusRepo.
 */
@Repository
@AllArgsConstructor
@CommonsLog
public class DynamoDBInvocationStatusRepo implements InvocationStatusRepo {

	static final String TABLE_NAME = "invocation_status";

	static final String TENANT_INDEX_NAME = "tenant_id-index";

	static final String TRIGGER_TENANT_INDEX_NAME = "trigger_id-tenant_id-index";

	static final int MAX_QUERY_SIZE = 2000;
	static final int ERROR_EXPIRATION_MINUTES = 2880; //48*60
	static final int INFO_EXPIRATION_MINUTES = 1400; //24*60

	static final String ID_FIELD = "id";
	static final String TENANT_ID_FIELD = "tenant_id";
	static final String TRIGGER_ID_FIELD = "trigger_id";
	static final String SUBSCRIPTION_ID_FIELD = "subscription_id";
	static final String SUBSCRIPTION_NAME_FIELD = "subscription_name";
	static final String TYPE_FIELD = "type";
	static final String CREATED_FIELD = "created";
	static final String COMPLETED_FIELD = "completed";

	static final String START_INVOCATION_INPUT_FIELD = "start_invocation_input";
	static final String INPUT_FIELD = "input";
	static final String CONTENT_JSON_FIELD = "content_json";

	static final String COMPLETE_INVOCATION_INPUT_FIELD = "complete_invocation_input";
	static final String ERROR_FIELD = "error";
	static final String OUTPUT_FIELD = "output";

	static final String UPDATE_QUERY = "set complete_invocation_input = :input," +
		"completed = :completed,expiration = :ttl";

	static final String UPDATE_CONDITIONAL_EXPRESSION = "attribute_exists(created)";

	static final String EXPIRATION_FIELD = "expiration";

	static final String INPUT_VALUE = ":input";
	static final String COMPLETED_VALUE = ":completed";
	static final String EXPIRATION_VALUE = ":ttl";

	private final Table _table;

	private final Index _tenantIndex;

	private final Index _triggerTenantIndex;

	@Autowired
	DynamoDBInvocationStatusRepo(AmazonDynamoDB amazonDynamoDB) {
		requireNonNull(amazonDynamoDB, "amazonDynamoDB is required");
		DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
		_table = dynamoDB.getTable(TABLE_NAME);
		_tenantIndex = _table.getIndex(TENANT_INDEX_NAME);
		_triggerTenantIndex = _table.getIndex(TRIGGER_TENANT_INDEX_NAME);
	}

	@Override
	public Stream<InvocationStatus> findByTenantId(TenantId tenantId) {
		requireNonNull(tenantId);

		List<Item> items = new ArrayList<>();
		QuerySpec querySpec = new QuerySpec()
			.withHashKey(TENANT_ID_FIELD, tenantId.toString())
			.withMaxResultSize(MAX_QUERY_SIZE);

		_tenantIndex.query(querySpec)
			.forEach(items::add);

		return items.stream()
			.map(DynamoDBInvocationStatusRepo::fromItem);
	}

	@Override
	public Stream<InvocationStatus> findByTenantIdAndTriggerId(TenantId tenantId, TriggerId triggerId) {
		requireNonNull(tenantId);
		requireNonNull(triggerId);

		List<Item> items = new ArrayList<>();
		QuerySpec querySpec = new QuerySpec()
			.withHashKey(TRIGGER_ID_FIELD, triggerId.toString())
			.withRangeKeyCondition(new RangeKeyCondition(TENANT_ID_FIELD).eq(tenantId.toString()))
			.withMaxResultSize(MAX_QUERY_SIZE);

		_triggerTenantIndex.query(querySpec)
			.forEach(items::add);

		return items.stream()
			.map(DynamoDBInvocationStatusRepo::fromItem);
	}

	@Override
	public Optional<InvocationStatus> findByTenantIdAndId(TenantId tenantId, UUID id) {
		requireNonNull(tenantId);
		requireNonNull(id);

		return Optional.ofNullable(_table.getItem(ID_FIELD, id.toString(), TENANT_ID_FIELD, tenantId.toString()))
			.map(DynamoDBInvocationStatusRepo::fromItem);
	}

	@Override
	public void start(InvocationStatus invocationStatus) {
		requireNonNull(invocationStatus);
		_table.putItem(toItem(invocationStatus));
	}

	@Override
	public void complete(TenantId tenantId, UUID id, CompleteInvocationInput completeInvocation) {
		requireNonNull(completeInvocation);

		long expiration =  INFO_EXPIRATION_MINUTES;
		try {
			Map<String, Object> newValues = new HashMap<>();
			Map<String, Object> completeInput = completeInvocationToMap(completeInvocation);

			if (!StringUtil.isNullOrEmpty(completeInvocation.getError())) {
				expiration = ERROR_EXPIRATION_MINUTES;
			}

			newValues.put(INPUT_VALUE, completeInput);
			newValues.put(COMPLETED_VALUE, OffsetDateTime.now().toString());
			newValues.put(EXPIRATION_VALUE, getExpiration(expiration));

			UpdateItemSpec updateItemSpec = new UpdateItemSpec()
				.withPrimaryKey(ID_FIELD, id.toString(), TENANT_ID_FIELD, tenantId.toString())
				.withUpdateExpression(UPDATE_QUERY)
				.withValueMap(newValues)
				.withConditionExpression(UPDATE_CONDITIONAL_EXPRESSION)
				.withReturnValues(ReturnValue.NONE);

			_table.updateItem(updateItemSpec);
		} catch (Exception e) {
			log.error("Error complete invocation", e);
		}
	}

	@Override
	public void delete(TenantId tenantId, UUID id) {
		requireNonNull(id);
		_table.deleteItem(ID_FIELD, id.toString(), TENANT_ID_FIELD, tenantId.toString());
	}

	/**
	 * Get InvocationStatus from DynamoDB item.
	 * @param item DynamoDB Item.
	 * @return InvocationStatus.
	 */
	private static InvocationStatus fromItem(Item item) {
		OffsetDateTime completed = null;
		if(!StringUtil.isNullOrEmpty(item.getString(COMPLETED_FIELD))) {
			completed = OffsetDateTime.parse(item.getString(COMPLETED_FIELD));
		}

		StartInvocationInput startInput = null;
		if (item.getRawMap(START_INVOCATION_INPUT_FIELD) != null) {
			Map<String, Object> startInputItem = item.getRawMap(START_INVOCATION_INPUT_FIELD);
			startInput = StartInvocationInput.builder()
				.triggerId(new TriggerId((String)startInputItem.get(TRIGGER_ID_FIELD)))
				.input((Map<String, Object>)startInputItem.get(INPUT_FIELD))
				.contentJson((Map<String, Object>)startInputItem.get(CONTENT_JSON_FIELD))
				.build();
		}

		CompleteInvocationInput completeInput = null;
		if (item.getRawMap(COMPLETE_INVOCATION_INPUT_FIELD) != null) {
			Map<String, Object> completeInputItem = item.getRawMap(COMPLETE_INVOCATION_INPUT_FIELD);
			completeInput = CompleteInvocationInput.builder()
				.error((String)completeInputItem.get(ERROR_FIELD))
				.output((Map<String, Object>)completeInputItem.get(OUTPUT_FIELD))
				.build();
		}

		return InvocationStatus.builder()
			.id(UUID.fromString(item.getString(ID_FIELD)))
			.tenantId(new TenantId(item.getString(TENANT_ID_FIELD)))
			.triggerId(new TriggerId(item.getString(TRIGGER_ID_FIELD)))
			.subscriptionId(UUID.fromString(item.getString(SUBSCRIPTION_ID_FIELD)))
			.subscriptionName(item.getString(SUBSCRIPTION_NAME_FIELD))
			.type(InvocationType.valueOf(item.getString(TYPE_FIELD)))
			.created(OffsetDateTime.parse(item.getString(CREATED_FIELD)))
			.completed(completed)
			.startInvocationInput(startInput)
			.completeInvocationInput(completeInput)
			.build();
	}

	/**
	 * Get DynamoDB Item form InvocationStatus.
	 * @param invocationStatus  Invocation Status.
	 * @return DynamoDB Item.
	 */
	private static Item toItem(InvocationStatus invocationStatus) {
		long expiration =  INFO_EXPIRATION_MINUTES;

		Map<String, Object> startInput = null;
		if (invocationStatus.getStartInvocationInput() != null) {
			startInput = ImmutableMap.of(
				TRIGGER_ID_FIELD, invocationStatus.getStartInvocationInput().getTriggerId().toString(),
				INPUT_FIELD, invocationStatus.getStartInvocationInput().getInput(),
				CONTENT_JSON_FIELD, invocationStatus.getStartInvocationInput().getContentJson());
		}

		Map<String, Object> completeInput = null;
		if (invocationStatus.getCompleteInvocationInput() != null) {
			completeInput = completeInvocationToMap(invocationStatus.getCompleteInvocationInput());

			if(!StringUtil.isNullOrEmpty(invocationStatus.getCompleteInvocationInput().getError())) {
				expiration = ERROR_EXPIRATION_MINUTES;
			}
		}

		Item result = new Item()
			.withString(ID_FIELD, invocationStatus.getId().toString())
			.withString(TENANT_ID_FIELD, invocationStatus.getTenantId().toString())
			.withString(TRIGGER_ID_FIELD, invocationStatus.getTriggerId().toString())
			.withString(SUBSCRIPTION_ID_FIELD, invocationStatus.getSubscriptionId().toString())
			.withString(SUBSCRIPTION_NAME_FIELD, invocationStatus.getSubscriptionName())
			.withString(TYPE_FIELD, invocationStatus.getType().toString())
			.withString(CREATED_FIELD, invocationStatus.getCreated().toString());

		if(invocationStatus.getCompleted() != null) {
			result.withString(COMPLETED_FIELD,
				invocationStatus.getCompleted().toString());
		}

		if(startInput != null) {
			result.withMap(START_INVOCATION_INPUT_FIELD, startInput);
		}

		if(completeInput != null) {
			result.withMap(COMPLETE_INVOCATION_INPUT_FIELD, completeInput);
		}

		result.withLong(EXPIRATION_FIELD, getExpiration(expiration));
		return result;
	}

	/**
	 * Convert complete invocation to Map.
	 * @param completeInvocation complete invocation.
	 * @return map represent complete invocations.
	 */
	private static Map<String, Object> completeInvocationToMap(CompleteInvocationInput completeInvocation) {
		Map<String, Object> completeInput = null;
		if (completeInvocation != null) {
			completeInput = new HashMap<>();
			if(!StringUtil.isNullOrEmpty(completeInvocation.getError())) {
				completeInput.put(ERROR_FIELD, completeInvocation.getError());
			}
			if(completeInvocation.getOutput() != null) {
				completeInput.put(OUTPUT_FIELD, completeInvocation.getOutput());
			};
		}
		return completeInput;
	}

	/**
	 * Get Item Expiration.
	 * @param duration duration in minutes.
	 * @return expiration time.
	 */
	private static long getExpiration(long duration) {
		long epochMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(duration);
		return TimeUnit.MILLISECONDS.toSeconds(epochMillis);
	}
}
