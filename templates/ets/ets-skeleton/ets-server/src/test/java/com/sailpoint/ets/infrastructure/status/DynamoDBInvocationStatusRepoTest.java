/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.status;

import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.google.common.collect.ImmutableMap;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.boot.AtlasBootIntegrationTest;
import com.sailpoint.atlas.boot.core.AtlasBootCoreConfiguration;
import com.sailpoint.ets.TestEventPublisher;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.status.CompleteInvocationInput;
import com.sailpoint.ets.domain.status.InvocationStatus;
import com.sailpoint.ets.domain.status.InvocationType;
import com.sailpoint.ets.domain.status.StartInvocationInput;
import com.sailpoint.ets.domain.trigger.TriggerId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.boot.autoconfigure.web.ServerProperties;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.sailpoint.ets.infrastructure.status.DynamoDBInvocationStatusRepo.TABLE_NAME;
import static com.sailpoint.ets.infrastructure.status.DynamoDBInvocationStatusRepo.TENANT_INDEX_NAME;
import static com.sailpoint.ets.infrastructure.status.DynamoDBInvocationStatusRepo.TRIGGER_TENANT_INDEX_NAME;


/**
 * Integration tests for  DynamoDBInvocationStatusRepo.
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {AtlasBootCoreConfiguration.class, ServerProperties.class},
	initializers = AtlasBootIntegrationTest.RandomPropertiesInitializer.class)
@SpringBootTest(classes = TestEventPublisher.class)
@ActiveProfiles("test")
public class DynamoDBInvocationStatusRepoTest extends AtlasBootIntegrationTest {

	DynamoDBInvocationStatusRepo _dynamoDBInvocationStatusRepo;

	@Before
	public void setup() {
		RequestContext requestContext = new RequestContext();
		requestContext.setOrg("acme-solar");
		requestContext.setPod("dev");
		RequestContext.set(requestContext);

		GlobalSecondaryIndex tenantIndex = new GlobalSecondaryIndex()
			.withIndexName(TENANT_INDEX_NAME)
			.withKeySchema(new KeySchemaElement("tenant_id", KeyType.HASH))
			.withProvisionedThroughput(new ProvisionedThroughput(5L, 5L))
			.withProjection(new Projection().withProjectionType(ProjectionType.ALL));

		GlobalSecondaryIndex ownerTenantIndex = new GlobalSecondaryIndex()
			.withIndexName(TRIGGER_TENANT_INDEX_NAME)
			.withKeySchema(new KeySchemaElement("trigger_id", KeyType.HASH),
				new KeySchemaElement("tenant_id", KeyType.RANGE))
			.withProvisionedThroughput(new ProvisionedThroughput(5L, 5L))
			.withProjection(new Projection().withProjectionType(ProjectionType.ALL));

		final CreateTableRequest tableRequest = new CreateTableRequest()
			.withTableName(TABLE_NAME)
			.withKeySchema(new KeySchemaElement("id", KeyType.HASH),
				new KeySchemaElement("tenant_id", KeyType.RANGE))
			.withAttributeDefinitions(
				new AttributeDefinition("id", "S"),
				new AttributeDefinition("tenant_id", "S"),
				new AttributeDefinition("trigger_id", "S")
			)
			.withProvisionedThroughput(new ProvisionedThroughput(5L, 5L))
			.withGlobalSecondaryIndexes(ownerTenantIndex, tenantIndex);

		_amazonDynamoDB.createTable(tableRequest);
		_dynamoDBInvocationStatusRepo = new DynamoDBInvocationStatusRepo(_amazonDynamoDB);
	}

	@After
	public void cleanup() {
		RequestContext.set(null);
	}

	@Test
	public void saveCompleteStatusTest() {
		//when initial invocation
		InvocationStatus invocationStatus = getInitialInvocationStatus();
		_dynamoDBInvocationStatusRepo.start(invocationStatus);
		Optional<InvocationStatus> result = _dynamoDBInvocationStatusRepo.findByTenantIdAndId(new TenantId("dev#acme-solar"),
			invocationStatus.getId());
		Assert.assertTrue(result.isPresent());
		verifyEquals(invocationStatus, result.get());

		//complete invocation.
		CompleteInvocationInput completeInvocation = getCompleteInvocationInput("Test error 1");
		invocationStatus.setCompleteInvocationInput(completeInvocation);
		invocationStatus.setCompleted(OffsetDateTime.now());

		_dynamoDBInvocationStatusRepo.complete(invocationStatus.getTenantId(), invocationStatus.getId(), completeInvocation);

		result = _dynamoDBInvocationStatusRepo.findByTenantIdAndId(new TenantId("dev#acme-solar"),
			invocationStatus.getId());

		Assert.assertTrue(result.isPresent());
		verifyEquals(invocationStatus, result.get());
		_dynamoDBInvocationStatusRepo.delete(invocationStatus.getTenantId(), invocationStatus.getId());

		long count = _dynamoDBInvocationStatusRepo.findByTenantId(invocationStatus.getTenantId()).count();
		Assert.assertEquals(0, count);
	}

	@Test
	public void completeStatusTest() {
		//complete invocation.
		CompleteInvocationInput completeInvocation = getCompleteInvocationInput("Test error 1");
		UUID id = UUID.randomUUID();
		TenantId tenantId = new TenantId("dev#acme-ocean");

		_dynamoDBInvocationStatusRepo.complete(tenantId, id, completeInvocation);

		Optional<InvocationStatus> result = _dynamoDBInvocationStatusRepo.findByTenantIdAndId(tenantId, id);

		Assert.assertFalse(result.isPresent());
	}

	@Test
	public void limitsTest() {
		//generate MAX_QUERY_SIZE + 20 invocations
		List<UUID> ids = new ArrayList<>();
		for(int i = 0; i < (DynamoDBInvocationStatusRepo.MAX_QUERY_SIZE + 20); i++) {
			//start invocation
			InvocationStatus invocationStatus = getInitialInvocationStatus();
			_dynamoDBInvocationStatusRepo.start(invocationStatus);
			//complete invocation.
			CompleteInvocationInput completeInvocation = getCompleteInvocationInput("Test error " + i);
			invocationStatus.setCompleteInvocationInput(completeInvocation);
			invocationStatus.setCompleted(OffsetDateTime.now());
			ids.add(invocationStatus.getId());
		}
		//insure we take only 2000
		TenantId testTenant = new TenantId("dev#acme-solar");
		long count = _dynamoDBInvocationStatusRepo.findByTenantId(testTenant).count();
		Assert.assertEquals(DynamoDBInvocationStatusRepo.MAX_QUERY_SIZE, count);

		//delete all
		ids.forEach( id->_dynamoDBInvocationStatusRepo.delete(testTenant, id));
		count = _dynamoDBInvocationStatusRepo.findByTenantId(testTenant).count();

		//verify deleted
		Assert.assertEquals(0, count);
	}

	private void verifyEquals(InvocationStatus invocationStatus, InvocationStatus result ) {
		Assert.assertEquals(invocationStatus.getId(), result.getId());
		Assert.assertEquals(invocationStatus.getTenantId(), result.getTenantId());
		Assert.assertEquals(invocationStatus.getTriggerId(), result.getTriggerId());
		Assert.assertEquals(invocationStatus.getSubscriptionId(), result.getSubscriptionId());
		Assert.assertEquals(invocationStatus.getType(), result.getType());
		Assert.assertEquals(invocationStatus.getCreated(), result.getCreated());
		Assert.assertEquals(invocationStatus.getSubscriptionName(), result.getSubscriptionName());
		if(invocationStatus.getCompleted() != null) {
			Assert.assertNotNull(result.getCompleted());
			Assert.assertEquals(invocationStatus.getCompleted().toEpochSecond(), result.getCompleted().toEpochSecond(), 5);
		}
		Assert.assertEquals(invocationStatus.getStartInvocationInput(), result.getStartInvocationInput());
		Assert.assertEquals(invocationStatus.getCompleteInvocationInput(), result.getCompleteInvocationInput());
	}

	private InvocationStatus getInitialInvocationStatus() {
		return InvocationStatus.builder()
			.id(UUID.randomUUID())
			.tenantId(new TenantId("dev#acme-solar"))
			.triggerId(new TriggerId("idn:test-trigger"))
			.subscriptionId(UUID.randomUUID())
			.subscriptionName("testSub")
			.type(InvocationType.REAL_TIME)
			.created(OffsetDateTime.now())
			.startInvocationInput(StartInvocationInput.builder()
				.input(ImmutableMap.of("inputKey1", "inputValue1"))
				.triggerId(new TriggerId("idn:test-trigger"))
				.contentJson(ImmutableMap.of("contentKey1", "contentValue1"))
				.build())
			.build();
	}

	private CompleteInvocationInput getCompleteInvocationInput(String error) {
		return CompleteInvocationInput.builder()
			.output(ImmutableMap.of("outKey1", "outValue1"))
			.error(error)
			.build();
	}
}
