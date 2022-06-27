/*
 * Copyright (C) 2022 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.infrastructure.dynamos3;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.BatchWriteItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.TableWriteItems;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.sailpoint.sp.identity.event.domain.IdentityId;
import com.sailpoint.sp.identity.event.domain.IdentityState;
import com.sailpoint.sp.identity.event.domain.IdentityStateRepository;
import com.sailpoint.sp.identity.event.domain.TenantId;
import com.sailpoint.sp.identity.event.infrastructure.s3.S3Client;
import com.sailpoint.utilities.CompressionUtil;
import com.sailpoint.utilities.JsonUtil;
import io.prometheus.client.Summary;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * DynamoDB implementation of IdentityStateRepository
 */
@CommonsLog
public class DynamoS3IdentityStateRepository implements IdentityStateRepository {

	public static final int DELETE_BATCH_SIZE = 25;
	public static final int BATCH_PROCESS_MAX_ATTEMPTS = 3;
	private static final int DYNAMO_DEFAULT_SIZE_LIMIT = 32000;
	public static final String HASH_KEY_NAME = "identity_id";
	public static final String URL_KEY = "s3_url";

	private final DynamoDB _dynamoDB;
	private final String _tableName;
	private final Table _dynamoTable;
	private final S3Client _s3Client;

	private int _dynamoMaxSizeBytes;

	private static final Summary _compressedStateBytes = Summary.build()
		.name("identity_state_file_size_bytes_dynamo")
		.quantile(0.5, 0.05)
		.quantile(0.75, 0.05)
		.quantile(0.95, 0.01)
		.help("the number of bytes stored in dynamo for identity state")
		.register();

	public DynamoS3IdentityStateRepository(AmazonDynamoDB amazonDynamoDB,
										   String dynamoTable,
										   S3Client s3Client,
										   int dynamoSizeLimitBytes) {
		requireNonNull(amazonDynamoDB, "amazonDynamoDB is required");
		requireNonNull(dynamoTable, "dynamo table name is required");
		requireNonNull(s3Client, "s3Client is required");

		// initialize Dynamo-related variables
		_dynamoDB = new DynamoDB(amazonDynamoDB);
		_tableName = dynamoTable;
		_dynamoTable = _dynamoDB.getTable(_tableName);

		_s3Client = s3Client;
		_dynamoMaxSizeBytes = (dynamoSizeLimitBytes <= 0 || dynamoSizeLimitBytes > 400000) ? DYNAMO_DEFAULT_SIZE_LIMIT : dynamoSizeLimitBytes;
	}

	public DynamoS3IdentityStateRepository(AmazonDynamoDB amazonDynamoDB,
										   String dynamoTable,
										   S3Client s3Client) {
		requireNonNull(amazonDynamoDB, "amazonDynamoDB is required");
		requireNonNull(dynamoTable, "dynamo table name is required");
		requireNonNull(s3Client, "s3Client is required");

		// initialize Dynamo-related variables
		_dynamoDB = new DynamoDB(amazonDynamoDB);
		_tableName = dynamoTable;
		_dynamoTable = _dynamoDB.getTable(_tableName);

		_s3Client = s3Client;
		_dynamoMaxSizeBytes = DYNAMO_DEFAULT_SIZE_LIMIT;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<IdentityState> findById(TenantId tenantId, IdentityId identityId) {
		requireNonNull(tenantId);
		requireNonNull(identityId);

		// retrieve dynamo object. If it is not in dynamo, then it is also not in s3.
		Item dynamoItem;

		dynamoItem = _dynamoTable.getItem(HASH_KEY_NAME, identityId.toString());
		if (dynamoItem == null) {
			return Optional.ofNullable(null);
		}

		// if there is an s3_url, retrieve the object in s3
		String s3Url = dynamoItem.getString(URL_KEY);
		if (s3Url != null) {
			return Optional.of(decodeIdentityState(_s3Client.findByURL(s3Url)));
		}
		// no s3_url, it's implied that the identity state is in the row
		return Optional.ofNullable(dynamoItem)
			.map(DynamoS3IdentityStateRepository::fromItem);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void save(TenantId tenantId, IdentityState identityState) {
		requireNonNull(tenantId);
		requireNonNull(identityState);

		final byte[] compressedIdentityState = encodeIdentityState(identityState);

		int sizeOfCompressedBytes = compressedIdentityState.length;

		long lastEventTime = identityState.getLastEventTime().toEpochSecond();

		UpdateItemSpec updateItemSpec = new UpdateItemSpec()
			.withPrimaryKey(HASH_KEY_NAME, identityState.getIdentity().getId().toString());

		log.info("size -- " + sizeOfCompressedBytes);
		// DYNAMO_SIZE_LIMIT is to protect against inserting a record in dynamo larger than 400k KB
		if (sizeOfCompressedBytes >= _dynamoMaxSizeBytes) {
			String url = tenantId.getId() + "/" + UUID.randomUUID().toString();
			updateItemSpec
				.withUpdateExpression("REMOVE compressed_identity_state SET s3_url=:url, last_event_time=:time, tenant_id = :v_tenantID")
				.withValueMap(
					new ValueMap()
						.withString(":url", url)
						.with(":time", lastEventTime)
						.withString(":v_tenantID", tenantId.getId()));
			// save into s3
			this._s3Client.saveCompressedBytes(compressedIdentityState, url, identityState.getExpiration());

			// attempt to save into dynamo
			this._dynamoTable.updateItem(updateItemSpec);
			_compressedStateBytes.observe(sizeOfCompressedBytes);
			return;
		}
		updateItemSpec
			.withUpdateExpression("REMOVE s3_url SET compressed_identity_state=:identity_state, last_event_time=:time, tenant_id = :v_tenantID")
			.withValueMap(
				new ValueMap()
					.withBinary(":identity_state", compressedIdentityState)
					.with(":time", lastEventTime)
					.withString(":v_tenantID", tenantId.getId()))
			.withReturnValues(ReturnValue.ALL_OLD);

		// save the new dynamo entry. If there is an old s3 entry, let's go ahead and clean that up.
		UpdateItemOutcome oldItem =  this._dynamoTable.updateItem(updateItemSpec);
		// check to see if the oldItem is not null. If it is, it is assumed we just created a new identity.
		if (oldItem != null && oldItem.getItem() != null) {
			String url = oldItem.getItem().getString(URL_KEY);
			if (url != null) {
				this._s3Client.deleteByURL(url);
			}
		}
		_compressedStateBytes.observe(sizeOfCompressedBytes);
	}

	// need to also delete from S3 potentially, or again we run the risk of orphaned s3 objects
	public void deleteById(TenantId tenantId, IdentityState identityState) {
		_dynamoTable.deleteItem(HASH_KEY_NAME, identityState.getIdentity().getId().toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deleteAllByTenant(TenantId tenantId) {

		List<PrimaryKey> keysToDelete = new ArrayList<>();
		Index index = _dynamoTable.getIndex("tenant_id-index");
		QuerySpec querySpec = new QuerySpec()
			.withKeyConditionExpression("tenant_id = :v_tenantID")
			.withValueMap(new ValueMap().withString(":v_tenantID", tenantId.getId()));
		index.query(querySpec)
			.forEach(
				i -> keysToDelete.add(new PrimaryKey(HASH_KEY_NAME, i.get(HASH_KEY_NAME)))
			);

		PrimaryKey[] batch = new PrimaryKey[DELETE_BATCH_SIZE];
		int i = 0;
		do {
			int fromIndex = i * DELETE_BATCH_SIZE;
			int toIndex = (i + 1) * DELETE_BATCH_SIZE;

			if (fromIndex >= keysToDelete.size()) {
				return;
			}

			if (toIndex < keysToDelete.size()) {
				batch = keysToDelete.subList(fromIndex, toIndex).toArray(batch);
				batchDelete(batch);
			} else {
				PrimaryKey[] endBatch = new PrimaryKey[0];
				batchDelete(keysToDelete.subList(fromIndex, keysToDelete.size()).toArray(endBatch));
			}
		} while (i++ < keysToDelete.size() / DELETE_BATCH_SIZE);

		// also wipe s3 data by tenantID. The correct way to do this however is to grab all
		// s3_urls from each dynamo row by tenantID, and delete them, as the s3 client is bleeding
		// details of us saving identity states
		this._s3Client.deleteAllByTenant(tenantId);
	}

	/**
	 * Batch deletes the given Primary Keys from this table.
	 *
	 * @param keysToDelete The PrimaryKeys.
	 */
	private void batchDelete(PrimaryKey... keysToDelete) {
		TableWriteItems tableWriteItems = new TableWriteItems(_tableName)
			.withPrimaryKeysToDelete(keysToDelete);

		BatchWriteItemOutcome outcome = _dynamoDB.batchWriteItem(tableWriteItems);

		int attempt = 0;
		do {
			Map<String, List<WriteRequest>> unprocessedItems = outcome.getUnprocessedItems();

			if (outcome.getUnprocessedItems().size() > 0) {
				outcome = _dynamoDB.batchWriteItemUnprocessed(unprocessedItems);
			}
		} while (outcome.getUnprocessedItems().size() > 0 && ++attempt < BATCH_PROCESS_MAX_ATTEMPTS);

		if (outcome.getUnprocessedItems().size() > 0) {
			log.error("Bulk deletion returned unprocessed items after " + attempt + " attempts.");
		}
	}

	/**
	 * Converts a raw DynamoDB item to an IdentityState.
	 *
	 * @param item The raw DynamoDB item.
	 * @return The IdentityState.
	 */
	public static IdentityState fromItem(Item item) {

		byte[] compressedIdentityState = item.getBinary("compressed_identity_state");
		return decodeIdentityState(compressedIdentityState);

	}

	private static byte[] encodeIdentityState(IdentityState identityState) {
		final String stateJson = JsonUtil.toJson(identityState);
		final byte[] jsonBytes = stateJson.getBytes(StandardCharsets.UTF_8);

		return CompressionUtil.compress(jsonBytes);
	}

	public static IdentityState decodeIdentityState(byte[] encoded) {
		final byte[] jsonBytes = CompressionUtil.decompress(encoded);
		final String stateJson = new String(jsonBytes, StandardCharsets.UTF_8);

		return JsonUtil.parse(IdentityState.class, stateJson);
	}
}
