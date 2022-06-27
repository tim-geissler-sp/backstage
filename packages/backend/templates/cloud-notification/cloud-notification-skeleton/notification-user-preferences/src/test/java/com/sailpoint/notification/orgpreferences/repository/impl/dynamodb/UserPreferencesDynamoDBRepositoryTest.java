/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.orgpreferences.repository.impl.dynamodb;

import com.sailpoint.atlas.dynamodb.DynamoDBService;
import com.sailpoint.atlas.test.EnvironmentUtil;
import com.sailpoint.atlas.test.integration.dynamodb.DynamoDBServerRule;
import com.sailpoint.atlas.test.integration.dynamodb.EnableInMemoryDynamoDB;
import com.sailpoint.notification.api.event.dto.NotificationMedium;
import com.sailpoint.notification.orgpreferences.repository.TenantUserPreferencesRepository;
import com.sailpoint.notification.orgpreferences.repository.dto.UserPreferencesDto;
import com.sailpoint.notification.orgpreferences.repository.impl.dynamodb.UserPreferencesDynamoDBRepository;
import com.sailpoint.notification.orgpreferences.repository.impl.dynamodb.entity.TenantUserPreferencesEntity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;


/**
 * Test TenantPreferences repository implementation with DynamoDB.
 */
@EnableInMemoryDynamoDB
public class UserPreferencesDynamoDBRepositoryTest {

	private TenantUserPreferencesRepository _repository;

	@Rule
	public DynamoDBServerRule _dynamoDBServerRule = new DynamoDBServerRule(EnvironmentUtil.findFreePort());

	@Before
	public void setup() {
		_dynamoDBServerRule.getDynamoDBService().createTable(TenantUserPreferencesEntity.class, DynamoDBService.PROJECTION_ALL);
		_repository = new UserPreferencesDynamoDBRepository(_dynamoDBServerRule.getDynamoDBMapper());
	}

	@Test
	public void saveFindDeleteTest() {
		UserPreferencesDto entity = new UserPreferencesDto();
		entity.setKey("access_control");
		entity.setMediums(Arrays.asList(NotificationMedium.EMAIL));
		entity.setUserId("12345");
		_repository.save("acme-solar", entity);

		entity.setKey("manual_tasks");
		_repository.save("acme-solar", entity);

		List<UserPreferencesDto> result = _repository.findAllForTenantUser("acme-solar", "12345");
		Assert.assertEquals(2, result.size());

		UserPreferencesDto userPreferencesDto = _repository.findOneForKeyAndTenantUser("acme-solar",
				"access_control", "12345");

		Assert.assertNotNull(userPreferencesDto);
		Assert.assertEquals(userPreferencesDto.getMediums(), Arrays.asList(NotificationMedium.EMAIL));

		_repository.bulkDeleteForTenant("acme-solar");

		result = _repository.findAllForTenantUser("acme-solar", "12345");
		Assert.assertEquals(0, result.size());

		//test error conditions
		try {
			result = _repository.findAllForTenantUser(null, null);
			Assert.assertEquals(0, result.size());

			userPreferencesDto = _repository.findOneForKeyAndTenantUser(null,
					null, null);
			Assert.assertNull(userPreferencesDto);

			_repository.bulkDeleteForTenantUser(null, null);

		} catch (Exception ignore) {
			Assert.fail("Fail to handle user preferences if tenant or key or user null or empty");
		}
	}
}
