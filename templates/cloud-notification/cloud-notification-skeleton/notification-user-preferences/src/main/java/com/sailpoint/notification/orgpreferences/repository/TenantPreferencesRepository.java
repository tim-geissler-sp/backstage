/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.orgpreferences.repository;


import com.sailpoint.notification.orgpreferences.repository.dto.PreferencesDto;

import java.util.List;

/**
 * Tenant Preferences repository interface for CRUD operations.
 */
public interface TenantPreferencesRepository extends BasePreferencesRepository<PreferencesDto> {

	/**
	 * Find all records for tenant.
	 * @param tenant tenant.
	 * @return list of preferences.
	 */
	List<PreferencesDto> findAllForTenant(String tenant);

	/**
	 * Find one record for tenant and key.
	 * @param tenant tenant.
	 * @param key notification key.
	 * @return preferences.
	 */
	PreferencesDto findOneForTenantAndKey(String tenant, String key);
}
