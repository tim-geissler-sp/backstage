/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.orgpreferences.repository.impl.dynamodb;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.atlas.util.StringUtil;
import com.sailpoint.notification.orgpreferences.repository.TenantPreferencesRepository;
import com.sailpoint.notification.orgpreferences.repository.dto.PreferencesDto;
import com.sailpoint.notification.orgpreferences.repository.impl.dynamodb.entity.PreferencesMapper;
import com.sailpoint.notification.orgpreferences.repository.impl.dynamodb.entity.TenantPreferencesEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TenantPreferences repository implementation with DynamoDB.
 */
@Singleton
public class TenantPreferencesDynamoDBRepository extends BasePreferencesDynamoDBRepository<TenantPreferencesEntity>
		implements TenantPreferencesRepository {

	private static final Log _log = LogFactory.getLog(TenantPreferencesDynamoDBRepository.class);

	@VisibleForTesting
	@Inject
	TenantPreferencesDynamoDBRepository(DynamoDBMapper dynamoDBMapper) {
		super(dynamoDBMapper, TenantPreferencesEntity.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void save(String tenant, PreferencesDto preferences) {
		save(PreferencesMapper.preferencesDtoToTenantPreferencesEntity(tenant, preferences));
		_log.info("Save tenant preferences for " + tenant + "  " + preferences.toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PreferencesDto findOneForTenantAndKey(String tenant, String key) {
		if(StringUtil.isNullOrEmpty(tenant) || StringUtil.isNullOrEmpty(key)) {
			_log.warn("Tenant or key null or empty");
			return null;
		}
		return PreferencesMapper.tenantPreferencesEntityToPreferencesDto(oneForTenantAndKey(tenant, key));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<PreferencesDto> findAllForTenant(String tenant) {
		if(StringUtil.isNullOrEmpty(tenant)) {
			_log.warn("Tenant null or empty");
			return Collections.emptyList();
		}
		return allForTenant(tenant)
				.stream()
				.map(PreferencesMapper::tenantPreferencesEntityToPreferencesDto)
				.collect(Collectors.toList());
	}

}
