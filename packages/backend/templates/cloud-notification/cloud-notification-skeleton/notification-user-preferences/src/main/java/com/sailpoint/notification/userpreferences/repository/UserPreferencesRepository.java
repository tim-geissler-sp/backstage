/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.userpreferences.repository;

import com.sailpoint.notification.userpreferences.dto.UserPreferences;

import java.util.List;

/**
 * UserPreferences repository interface for CRUD operations.
 */
public interface UserPreferencesRepository {

	/**
	 * Retrieves an user preference by recipient id.
	 *
	 * @param recipientId The Recipient id.
	 * @return UserPreferences The user preference.
	 */
	UserPreferences findByRecipientId(String recipientId);

	/**
	 * Retrieves a list of user preferences by tenant.
	 *
	 * @param tenant The tenant.
	 * @return List<UserPreferences> List of user preferences.
	 */
	List<UserPreferences> findAllByTenant(String tenant);

	/**
	 * Creates a new user preference entry in the data store.
	 *
	 * @param userPreferences The user preference.
	 */
	void create(UserPreferences userPreferences);

	/**
	 * Deletes an user preference by recipiente id.
	 *
	 * @param recipientId The recipient id.
	 */
	void deleteByRecipientId(String recipientId);

	/**
	 * Deletes all user preferences entries by tenant.
	 *
	 * @param tenant The tenant.
	 */
	void deleteByTenant(String tenant);
}
