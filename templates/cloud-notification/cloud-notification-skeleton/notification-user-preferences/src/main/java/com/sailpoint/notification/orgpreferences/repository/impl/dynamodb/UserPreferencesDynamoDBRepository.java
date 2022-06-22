/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.orgpreferences.repository.impl.dynamodb;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.atlas.util.StringUtil;
import com.sailpoint.notification.orgpreferences.repository.TenantUserPreferencesRepository;
import com.sailpoint.notification.orgpreferences.repository.dto.UserPreferencesDto;
import com.sailpoint.notification.orgpreferences.repository.impl.dynamodb.entity.PreferencesMapper;
import com.sailpoint.notification.orgpreferences.repository.impl.dynamodb.entity.TenantUserPreferencesEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.sailpoint.notification.orgpreferences.repository.impl.dynamodb.entity.TenantUserPreferencesEntity.USER_NAME;


/**
 * UserPreferences repository implementation with DynamoDB.
 */
@Singleton
public class UserPreferencesDynamoDBRepository extends BasePreferencesDynamoDBRepository<TenantUserPreferencesEntity>
		implements TenantUserPreferencesRepository {

	private static final Log _log = LogFactory.getLog(UserPreferencesDynamoDBRepository.class);

	@VisibleForTesting
	@Inject
	UserPreferencesDynamoDBRepository(DynamoDBMapper dynamoDBMapper) {
		super(dynamoDBMapper, TenantUserPreferencesEntity.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void save(String tenant, UserPreferencesDto preferences) {
		save(PreferencesMapper.userPreferencesDtoToTenantUserPreferencesEntity(tenant, preferences));
		_log.info("Save tenant user preferences for " + tenant + " " + preferences.toString());
	}

	/**
	 * {@inheritDoc}
	 */
	public void bulkDeleteForTenantUser(String tenant, String userId) {
		if(StringUtil.isNullOrEmpty(tenant) || StringUtil.isNullOrEmpty(userId)) {
			_log.warn("Tenant or userId null or empty");
			return;
		}

		boolean deleting = true;
		final DynamoDBQueryExpression<TenantUserPreferencesEntity> query = getQueryExpression(tenant, null, userId);
		query.setLimit(MAX_DELETE_LIMIT);
		do {
			List<TenantUserPreferencesEntity> result = _dynamoDBMapper.query(TenantUserPreferencesEntity.class, query);
			if (result != null && result.size() > 0) {
				_dynamoDBMapper.batchDelete(result);
			} else {
				deleting = false;
			}
		} while (deleting);
		_log.info("Bulk delete tenant user preferences for  " + tenant + " userId " + userId);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<UserPreferencesDto> findAllForTenantUser(String tenant, String userId) {
		if(StringUtil.isNullOrEmpty(tenant) || StringUtil.isNullOrEmpty(userId)) {
			_log.warn("Tenant or userId null or empty");
			return Collections.emptyList();
		}

		return _dynamoDBMapper.query(TenantUserPreferencesEntity.class, getQueryExpression(tenant, null, userId))
				.stream()
				.map(PreferencesMapper::tenantUserPreferencesEntityToUserPreferencesDto)
				.collect(Collectors.toList());
	}

	@Override
	public UserPreferencesDto findOneForKeyAndTenantUser(String tenant, String key, String userId) {
		if(StringUtil.isNullOrEmpty(tenant) || StringUtil.isNullOrEmpty(key) || StringUtil.isNullOrEmpty(userId)) {
			_log.warn("Tenant, key, or userId null or empty");
			return null;
		}

		return _dynamoDBMapper.query(TenantUserPreferencesEntity.class, getQueryExpression(tenant, key, userId))
				.stream()
				.map(PreferencesMapper::tenantUserPreferencesEntityToUserPreferencesDto)
				.findFirst()
				.orElse(null);
	}

	private DynamoDBQueryExpression<TenantUserPreferencesEntity> getQueryExpression(String tenant, String key, String userId) {
		final TenantUserPreferencesEntity orgPref = new TenantUserPreferencesEntity();
		orgPref.setNotificationKey(key);
		orgPref.setTenant(tenant);

		Condition rangeKeyCondition = new Condition();
		rangeKeyCondition.withComparisonOperator(ComparisonOperator.EQ)
				.withAttributeValueList(new AttributeValue().withS(userId));

		final DynamoDBQueryExpression<TenantUserPreferencesEntity> query = new DynamoDBQueryExpression<>();

		query.withHashKeyValues(orgPref)
				.withRangeKeyCondition(USER_NAME, rangeKeyCondition)
				.setConsistentRead(false);

		return query;
	}
}
