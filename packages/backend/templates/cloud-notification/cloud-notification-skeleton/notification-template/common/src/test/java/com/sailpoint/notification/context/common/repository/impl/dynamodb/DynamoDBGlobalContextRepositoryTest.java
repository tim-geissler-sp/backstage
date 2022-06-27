/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.context.common.repository.impl.dynamodb;

import com.google.common.collect.ImmutableList;
import com.sailpoint.atlas.dynamodb.DynamoDBService;
import com.sailpoint.atlas.test.EnvironmentUtil;
import com.sailpoint.atlas.test.integration.dynamodb.DynamoDBServerRule;
import com.sailpoint.atlas.test.integration.dynamodb.EnableInMemoryDynamoDB;
import com.sailpoint.notification.context.common.model.GlobalContext;
import com.sailpoint.notification.context.common.model.GlobalContextEntity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Unit tests for DynamoDB implementation of GlobalContextRepository.
 */
@EnableInMemoryDynamoDB
@RunWith(MockitoJUnitRunner.class)
public class DynamoDBGlobalContextRepositoryTest {

	private DynamoDBGlobalContextRepository _dynamoDBGlobalContextRepository;

	private final Map<String, Object> _attributes = new HashMap<>();

	private GlobalContext _globalContext;

	@Rule
	public DynamoDBServerRule _dynamoDBServerRule = new DynamoDBServerRule(EnvironmentUtil.findFreePort());

	@Before
	public void setup() {

		_dynamoDBGlobalContextRepository = new DynamoDBGlobalContextRepository(_dynamoDBServerRule.getDynamoDBMapper());

		_dynamoDBServerRule.getDynamoDBService().createTable(GlobalContextEntity.class,
				DynamoDBService.PROJECTION_ALL);
	}

	@Test
	public void verifySaveAndFindTest() {
		givenAttributes();
		givenGlobalContext();

		// when saved
		_dynamoDBGlobalContextRepository.save(_globalContext);

		// Then retrieved obj is the same
		Optional<GlobalContext> savedGlobalContext = _dynamoDBGlobalContextRepository.findOneByTenant("acme-solar");

		thenGlobalContextMatches(savedGlobalContext.get());
	}

	@Test
	public void verifyDeleteTest() {

		givenAttributes();
		givenGlobalContext();

		// when saved
		_dynamoDBGlobalContextRepository.save(_globalContext);

		// Then retrieved obj is the same
		Optional<GlobalContext> savedGlobalContext = _dynamoDBGlobalContextRepository.findOneByTenant("acme-solar");

		thenGlobalContextMatches(savedGlobalContext.get());

		// When deleted
		boolean deleted = _dynamoDBGlobalContextRepository.deleteByTenant("acme-solar");

		Assert.assertTrue(deleted);

		// Then retrieved obj is the same
		savedGlobalContext = _dynamoDBGlobalContextRepository.findOneByTenant("acme-solar");
		Assert.assertFalse(savedGlobalContext.isPresent());
	}

	@Test
	public void notFoundRecordTest() {
		// When deleted is called on a non-existing record
		boolean deleted = _dynamoDBGlobalContextRepository.deleteByTenant("acme-solar");

		// Then
		Assert.assertFalse(deleted);
	}

	private void thenGlobalContextMatches(GlobalContext globalContext) {
		Assert.assertNotNull(globalContext);
		Assert.assertEquals(_globalContext.getTenant(), globalContext.getTenant());
		Assert.assertEquals(_globalContext.getAttributes(), globalContext.getAttributes());
	}

	private void givenAttributes() {
		_attributes.put("productName", "IdentityNow");
		_attributes.put("version", 1234);
		_attributes.put("collection", ImmutableList.of("a", "b", "c"));
	}

	private void givenGlobalContext() {
		_globalContext = new GlobalContext("acme-solar");
		_globalContext.setAttributes(_attributes);
	}
}
