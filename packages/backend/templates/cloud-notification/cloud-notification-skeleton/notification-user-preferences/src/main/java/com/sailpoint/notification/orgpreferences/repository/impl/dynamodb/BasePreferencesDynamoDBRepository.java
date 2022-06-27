/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.orgpreferences.repository.impl.dynamodb;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.sailpoint.atlas.util.StringUtil;
import com.sailpoint.notification.orgpreferences.repository.impl.dynamodb.entity.BasePreferencesEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Base class for PreferencesDynamoDBRepository.
 */
abstract class BasePreferencesDynamoDBRepository<T extends BasePreferencesEntity> {

	private final static String USER_ATTRIBUTE = ":i";

	final static int MAX_DELETE_LIMIT = 250;

	private static final Log _log = LogFactory.getLog(TenantPreferencesDynamoDBRepository.class);

	private Class<T> _clazz;

	DynamoDBMapper _dynamoDBMapper;

	BasePreferencesDynamoDBRepository(DynamoDBMapper dynamoDBMapper, Class<T> clazz) {
		_dynamoDBMapper = dynamoDBMapper;
		_clazz = clazz;
	}

	/**
	 * Delete all records for given tenant.
	 * @param tenant tenant.
	 */
	public void bulkDeleteForTenant(String tenant) {
		if(StringUtil.isNullOrEmpty(tenant)) {
			_log.warn("Tenant null or empty");
			return;
		}

		boolean deleting = true;
		final DynamoDBQueryExpression<T> query = getQueryExpression(tenant);
		query.setLimit(MAX_DELETE_LIMIT);
		do {
			List<T> result = _dynamoDBMapper.query(_clazz, query);
			if (result != null && result.size() > 0) {
				_dynamoDBMapper.batchDelete(result);
			} else {
				deleting = false;
			}
		} while (deleting);
		_log.info("Bulk delete tenant preferences for " + tenant);
	}

	protected DynamoDBQueryExpression<T> getQueryExpression(String tenant) {
		try {
			final T orgPref = _clazz.newInstance();
			orgPref.setNotificationKey(null);
			orgPref.setTenant(tenant);

			final DynamoDBQueryExpression<T> queryExpression = new DynamoDBQueryExpression<>();
			return queryExpression.withHashKeyValues(orgPref);
		} catch (Exception e) {
			throw  new IllegalArgumentException("Error get query for type " + _clazz.getCanonicalName(), e);
		}
	}

	protected T oneForTenantAndKey(String tenant, String key) {
		return _dynamoDBMapper.load(_clazz, tenant, key);
	}


	protected void save(T tenantPreferencesEntity) {
		_dynamoDBMapper.save(tenantPreferencesEntity);
	}

	protected List<T> allForTenant(String tenant) {
		final DynamoDBQueryExpression<T> query = getQueryExpression(tenant);
		return _dynamoDBMapper.query(_clazz, query);
	}

}
