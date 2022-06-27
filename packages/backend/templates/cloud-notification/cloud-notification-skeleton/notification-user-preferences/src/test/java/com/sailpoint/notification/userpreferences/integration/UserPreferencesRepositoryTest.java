/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.userpreferences.integration;

import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.test.EnvironmentUtil;
import com.sailpoint.atlas.test.integration.dynamodb.DynamoDBServerRule;
import com.sailpoint.atlas.test.integration.dynamodb.EnableInMemoryDynamoDB;
import com.sailpoint.notification.api.event.RecipientBuilder;
import com.sailpoint.notification.sender.common.test.TestUtil;
import com.sailpoint.notification.userpreferences.dto.UserPreferences;
import com.sailpoint.notification.userpreferences.mapper.UserPreferencesMapper;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;
import com.sailpoint.notification.userpreferences.repository.impl.dynamodb.DynamoDBRepository;
import com.sailpoint.notification.userpreferences.repository.impl.dynamodb.entity.UserPreferencesEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@EnableInMemoryDynamoDB
public class UserPreferencesRepositoryTest {

	private static final Log _log = LogFactory.getLog(UserPreferencesRepositoryTest.class);

	UserPreferencesRepository _repository;

	UserPreferences _userPreferences;

	private final int USERPREFERENCES_ENTRIES = 3;

	@Rule
	public DynamoDBServerRule _dynamoDBServerRule = new DynamoDBServerRule(EnvironmentUtil.findFreePort());

	@Before
	public void setup() {
		RequestContext.set(TestUtil.setDummyRequestContext());
		_dynamoDBServerRule.getDynamoDBService().createTable(UserPreferencesEntity.class, null);
		_repository = new DynamoDBRepository(_dynamoDBServerRule.getDynamoDBMapper(), new UserPreferencesMapper());
	}

	@Test
	public void testFindByRecipientId() {
		givenUserPreferences();

		UserPreferences userPreferences = _repository.findByRecipientId(_userPreferences.getRecipient().getId());
		Assert.assertEquals(_userPreferences, userPreferences);
		userPreferences = _repository.findByRecipientId("invalidId");
		Assert.assertNull(userPreferences);
		userPreferences = _repository.findByRecipientId(null);
		Assert.assertNull(userPreferences);
		userPreferences = _repository.findByRecipientId("");
		Assert.assertNull(userPreferences);
	}

	@Test
	public void testDelete() {
		givenUserPreferences();

		String recipientId = _userPreferences.getRecipient().getId();
		_repository.deleteByRecipientId(recipientId);
		Assert.assertNull(_repository.findByRecipientId(recipientId));
		try {
			_repository.deleteByRecipientId(null);
			_repository.deleteByRecipientId("");
		} catch (Exception ignor) {
			Assert.fail("Fail to delete user by recipient for null or empty");
		}

	}

	@Test
	public void testFindAllByTenant() {

		for(int i = 0; i < USERPREFERENCES_ENTRIES; ++i) {
			givenUserPreferences();
		}

		// When find all by Tenant
		List<UserPreferences> userPreferencesEntities = _repository.findAllByTenant("dev__acme-solar");

		// Then verify list size
		Assert.assertEquals(USERPREFERENCES_ENTRIES, userPreferencesEntities.size());

		try {
			userPreferencesEntities = _repository.findAllByTenant(null);
			Assert.assertEquals(0, userPreferencesEntities.size());
			userPreferencesEntities = _repository.findAllByTenant("");
			Assert.assertEquals(0, userPreferencesEntities.size());
		} catch (Exception ignore) {
			Assert.fail("Fail to find tenant for null or empty");
		}
 	}

 	@Test
	public void deleteByTenant() {
		for(int i = 0; i < 30; ++i) {
			givenUserPreferences();
		}

		// When find all by Tenant
		List<UserPreferences> userPreferencesEntities = _repository.findAllByTenant("dev__acme-solar");

		// Then verify list size
		Assert.assertEquals(30, userPreferencesEntities.size());

		// When call delete
		_repository.deleteByTenant("dev__acme-solar");

		// Then find all by tenant should return empty list
		userPreferencesEntities = _repository.findAllByTenant("dev__acme-solar");
		Assert.assertTrue(userPreferencesEntities.isEmpty());
		try {
			_repository.deleteByTenant(null);
			_repository.deleteByTenant("");
		} catch (Exception ignore) {
			Assert.fail("Fail to delete tenant for null or empty");
		}
	}

 	private void givenUserPreferences() {
		_userPreferences = new UserPreferences.UserPreferencesBuilder()
				.withRecipient(new RecipientBuilder()
						.withName("Dante")
						.withEmail("count@montecristo.com")
						.withPhone(null)
						.withId(UUID.randomUUID().toString())
						.build())
				.withBrand(Optional.ofNullable("brand1"))
				.build();
		_repository.create(_userPreferences);
	}

	@After
	public void cleanup() {
		RequestContext.set(null);
	}
}
