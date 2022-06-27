/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.email.repository;

import com.sailpoint.notification.sender.email.domain.TenantSenderEmail;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * {@link TenantSenderEmail} Repository Interface
 */
public interface TenantSenderEmailRepository {

	/**
	 * Retrieve all items for given tenant.
	 *
	 * @param tenantId Tenant ID
	 * @return List of {@link TenantSenderEmail}
	 */
	List<TenantSenderEmail> findAllByTenant(final String tenantId);

	/**
	 * Retrieve the item for given tenant and id
	 *
	 * @param tenantId Tenant ID
	 * @param id ID
	 * @return Optional of TenantSenderEmail
	 */
	Optional<TenantSenderEmail> findByTenantAndId(final String tenantId, final String id);

	/**
	 * Retrieve all Items for given email.
	 *
	 * @param email Email
	 * @return List of TenantSenderEmail
	 */
	List<TenantSenderEmail> findAllByEmail(final String email);

	/**
	 * Put {@link TenantSenderEmail} Item.
	 *
	 * @param tenantSenderEmail TenantSenderEmail item to save
	 */
	void save(final TenantSenderEmail tenantSenderEmail);

	/**
	 * Batch write collection of {@link TenantSenderEmail} Items.
	 *
	 * @param tenantSenderEmails Collection of TenantSenderEmail to save
	 */
	void batchSave(final Collection<TenantSenderEmail> tenantSenderEmails);

	/**
	 * Delete {@link TenantSenderEmail} Item.
	 *
	 * @param tenantId Tenant ID
	 * @param id ID
	 */
	void delete(final String tenantId, final String id);
}
