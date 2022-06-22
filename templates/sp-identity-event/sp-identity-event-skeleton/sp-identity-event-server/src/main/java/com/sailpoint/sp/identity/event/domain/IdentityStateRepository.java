/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain;


import java.util.Optional;

/**
 * Repository for persistence of IdentityState.
 */
public interface IdentityStateRepository {

	/**
	 * Gets the specified IdentityState by tenant/id.
	 *
	 * @param tenantId The tenant ID.
	 * @param identityId The identity ID.
	 * @return The optional IdentityState.
	 */
	Optional<IdentityState> findById(TenantId tenantId, IdentityId identityId);

	/**
	 * Saves the specified IdentityState.
	 *
	 * @param tenantId The tenant ID.
	 * @param identityState The IdentityState to persist.
	 */
	void save(TenantId tenantId, IdentityState identityState);

	/**
	 * Deletes all Identity state data for the specified tenant.
	 *
	 * @param tenantId The tenant ID.
	 */
	void deleteAllByTenant(TenantId tenantId);

	/**
	 * Deletes the specified IdentityState by id.
	 *
	 * @param tenantId The tenant ID.
	 * @param identityId The identity ID.
	 */
	default void deleteById(TenantId tenantId, IdentityId identityId) {
		throw new UnsupportedOperationException("not implemented");
	}

}
