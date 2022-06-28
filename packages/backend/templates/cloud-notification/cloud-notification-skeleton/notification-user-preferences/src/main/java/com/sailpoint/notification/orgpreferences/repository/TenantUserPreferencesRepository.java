/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.orgpreferences.repository;


import com.sailpoint.notification.orgpreferences.repository.dto.UserPreferencesDto;

import java.util.List;

/**
 * User Preferences repository interface for CRUD operations.
 */
public interface TenantUserPreferencesRepository extends BasePreferencesRepository<UserPreferencesDto> {

	/**
	 * Delete all records for given userId in a tenant context.
	 * @param tenant tenant.
	 * @param userId userId.
	 */
	void bulkDeleteForTenantUser(String tenant, String userId);

	/**
	 * Find all records for user in a tenant context
	 * @param tenant tenant.
	 * @param userId userId.
	 * @return list of preferences.
	 */
	List<UserPreferencesDto> findAllForTenantUser(String tenant, String userId);

	/**
	 * Find one record for a given user and key in a tenant context.
	 * @param tenant tenant.
	 * @param key notification key.
	 * @return preferences.
	 */
	UserPreferencesDto findOneForKeyAndTenantUser(String tenant, String key, String userId);
}
