package com.sailpoint.sp.identity.event.infrastructure.dynamos3;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.sp.identity.event.DualWriterIdentityStateRepositoryTest;
import com.sailpoint.sp.identity.event.domain.IdentityId;
import com.sailpoint.sp.identity.event.domain.IdentityState;
import com.sailpoint.sp.identity.event.domain.IdentityStateRepository;
import com.sailpoint.sp.identity.event.domain.TenantId;
import com.sailpoint.sp.identity.event.infrastructure.DualWriterIdentityStateRepository;
import com.sailpoint.sp.identity.event.infrastructure.s3.S3Client;
import com.sailpoint.utilities.CompressionUtil;
import com.sailpoint.utilities.JsonUtil;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Optional;

import static com.sailpoint.sp.identity.event.infrastructure.dynamos3.DynamoS3IdentityStateRepository.HASH_KEY_NAME;
import static com.sailpoint.sp.identity.event.infrastructure.dynamos3.DynamoS3IdentityStateRepository.URL_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DynamoS3IdentityStateRepositoryTest {
	private final AmazonDynamoDB mockAmazonDynamoDB = mock(AmazonDynamoDB.class);
	private S3Client mockS3Client = mock(S3Client.class);
	private IdentityState identityState = IdentityState.builder()
		.identity(DualWriterIdentityStateRepositoryTest.buildDummyIdentity())
		.lastEventTime(OffsetDateTime.now())
		.build();
	TenantId tenantId = new TenantId("tenantId");

	DynamoS3IdentityStateRepository repo = new DynamoS3IdentityStateRepository(mockAmazonDynamoDB, "table", mockS3Client);

	@Test
	public void findByIdInDynamoTest() {
		final String stateJson = JsonUtil.toJson(identityState);
		final byte[] jsonBytes = stateJson.getBytes(StandardCharsets.UTF_8);
		ByteBuffer buffer = ByteBuffer.wrap(CompressionUtil.compress(jsonBytes));
		IdentityId identityId = new IdentityId("identityId");
		HashMap<String, AttributeValue> itemMap = new HashMap<String, AttributeValue>();
		itemMap.put("compressed_identity_state", new AttributeValue().withB(buffer));

		GetItemResult itemResult = new GetItemResult().withItem(itemMap);
		when(mockAmazonDynamoDB.getItem(any(GetItemRequest.class))).thenReturn(itemResult);

		Optional<IdentityState> retrievedIdentityState = repo.findById(tenantId, identityId);

		verify(mockAmazonDynamoDB, times(1)).getItem(any(GetItemRequest.class));
		assertEquals(identityState, retrievedIdentityState.get());
	}

	@Test
	public void findByIdInS3Test() {
		final String stateJson = JsonUtil.toJson(identityState);
		final byte[] jsonBytes = stateJson.getBytes(StandardCharsets.UTF_8);
		final String s3Url = "www.s3url.com";

		IdentityId identityId = new IdentityId("identityId");
		HashMap<String, AttributeValue> itemMap = new HashMap<String, AttributeValue>();
		itemMap.put(URL_KEY, new AttributeValue().withS(s3Url));

		GetItemResult itemResult = new GetItemResult().withItem(itemMap);
		when(mockAmazonDynamoDB.getItem(any(GetItemRequest.class))).thenReturn(itemResult);
		when(mockS3Client.findByURL(s3Url)).thenReturn(CompressionUtil.compress(jsonBytes));
		Optional<IdentityState> retrievedIdentityState = repo.findById(tenantId, identityId);

		verify(mockAmazonDynamoDB, times(1)).getItem(any(GetItemRequest.class));
		verify(mockS3Client, times(1)).findByURL(s3Url);

		assertEquals(identityState, retrievedIdentityState.get());
	}

	@Test
	public void saveDynamoNoS3() {
		UpdateItemResult itemResult = new UpdateItemResult();
		when(mockAmazonDynamoDB.updateItem(any(UpdateItemRequest.class))).thenReturn(itemResult);
		assertDoesNotThrow(() -> repo.save(tenantId, identityState));

		verify(mockAmazonDynamoDB, times(1)).updateItem(any(UpdateItemRequest.class));

	}


}
