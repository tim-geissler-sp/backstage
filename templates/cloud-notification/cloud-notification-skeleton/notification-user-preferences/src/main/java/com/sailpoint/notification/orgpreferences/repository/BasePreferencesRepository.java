/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.orgpreferences.repository;


import com.sailpoint.notification.orgpreferences.repository.dto.PreferencesDto;

/**
 * Base Preferences repository interface for CRUD operations.
 */
interface BasePreferencesRepository<T extends PreferencesDto> {

	/**
	 * Save preferences in tenant repo.
	 * @param tenant tenant.
	 * @param preferences preferences.
	 */
	void save(String tenant, T preferences);

	/**
	 * Delete all records for given tenant.
	 * @param tenant tenant.
	 */
	void bulkDeleteForTenant(String tenant);
}
