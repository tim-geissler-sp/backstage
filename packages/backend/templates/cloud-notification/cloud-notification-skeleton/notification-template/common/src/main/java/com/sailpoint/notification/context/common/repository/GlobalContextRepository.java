/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.context.common.repository;

import com.sailpoint.notification.sender.common.exception.persistence.StaleElementException;
import com.sailpoint.notification.context.common.model.GlobalContext;

import java.util.List;
import java.util.Optional;

/**
 * GlobalContext repository interface.
 */
public interface GlobalContextRepository {

	/**
	 * Retrieves GlobalContext by tenant.
	 *
	 * @param tenant GlobalContext tenant.
	 * @return GlobalContext.
	 */
	Optional<GlobalContext> findOneByTenant(String tenant);

	/**
	 * Retrieves all {@link GlobalContext} by tenant.
	 * @param tenant {@link GlobalContext} tenant.
	 * @return {@link GlobalContext}
	 */
	List<GlobalContext> findAllByTenant(String tenant);

	/**
	 * Saves GlobalContext.
	 *
	 * @param globalContext The GlobalContext.
	 */
	void save(GlobalContext globalContext) throws StaleElementException;

	/**
	 * Deletes GlobalContext by tenant.
	 *
	 * @param tenant GlobalContext tenant.
	 * @return boolean Whether or not the GlobalContext was deleted.
	 */
	boolean deleteByTenant(String tenant) throws StaleElementException;

}
