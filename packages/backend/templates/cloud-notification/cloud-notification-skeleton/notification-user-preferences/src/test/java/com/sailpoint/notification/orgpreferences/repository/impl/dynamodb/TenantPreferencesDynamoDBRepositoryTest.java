/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.orgpreferences.repository.impl.dynamodb;

import com.sailpoint.atlas.dynamodb.DynamoDBService;
import com.sailpoint.atlas.test.EnvironmentUtil;
import com.sailpoint.atlas.test.integration.dynamodb.DynamoDBServerRule;
import com.sailpoint.atlas.test.integration.dynamodb.EnableInMemoryDynamoDB;
import com.sailpoint.notification.api.event.dto.NotificationMedium;
import com.sailpoint.notification.orgpreferences.repository.TenantPreferencesRepository;
import com.sailpoint.notification.orgpreferences.repository.dto.PreferencesDto;
import com.sailpoint.notification.orgpreferences.repository.impl.dynamodb.entity.TenantPreferencesEntity;
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
public class TenantPreferencesDynamoDBRepositoryTest {

	private TenantPreferencesRepository _repository;

	@Rule
	public DynamoDBServerRule _dynamoDBServerRule = new DynamoDBServerRule(EnvironmentUtil.findFreePort());

	@Before
	public void setup() {
		_dynamoDBServerRule.getDynamoDBService().createTable(TenantPreferencesEntity.class, DynamoDBService.PROJECTION_ALL);
		_repository = new TenantPreferencesDynamoDBRepository(_dynamoDBServerRule.getDynamoDBMapper());
	}

	@Test
	public void saveFindDeleteTest() {

		PreferencesDto entity = new PreferencesDto();
		entity.setKey("access_control");
		entity.setMediums(Arrays.asList(NotificationMedium.EMAIL));
		_repository.save("acme-solar", entity);

		entity.setKey("manual_tasks");
		entity.setMediums(Arrays.asList(NotificationMedium.EMAIL));
		_repository.save("acme-solar", entity);

		List<PreferencesDto> result = _repository.findAllForTenant("acme-solar");
		Assert.assertEquals(2, result.size());

		PreferencesDto preferencesDto = _repository.findOneForTenantAndKey("acme-solar",
				"access_control");

		Assert.assertNotNull(preferencesDto);
		Assert.assertEquals(preferencesDto.getMediums(), Arrays.asList(NotificationMedium.EMAIL));

		_repository.bulkDeleteForTenant("acme-solar");

		result = _repository.findAllForTenant("acme-solar");
		Assert.assertEquals(0, result.size());

		//test error conditions
		try {
			result = _repository.findAllForTenant(null);
			Assert.assertEquals(0, result.size());
			result = _repository.findAllForTenant("");
			Assert.assertEquals(0, result.size());

			preferencesDto = _repository.findOneForTenantAndKey(null, null);
			Assert.assertNull(preferencesDto);
			preferencesDto = _repository.findOneForTenantAndKey("acme-solar","");
			Assert.assertNull(preferencesDto);
			_repository.bulkDeleteForTenant("");
			_repository.bulkDeleteForTenant(null);
		} catch (Exception ignore) {
			Assert.fail("Fail to handle org preferences if tenant or key null or empty");
		}

	}
}
