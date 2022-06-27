/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.status;

import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.trigger.TriggerId;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * InvocationStatusRepo interface.
 */
public interface InvocationStatusRepo {

	/**
	 * Return all invocations for given tenant.
	 * @param tenantId  tenant Id.
	 * @return Invocation statuses.
	 */
	Stream<InvocationStatus> findByTenantId(TenantId tenantId);

	/**
	 * Return all invocations for given tenant and trigger id.
	 * @param tenantId tenant Id.
	 * @param triggerId trigger Id
	 * @return Invocation statuses.
	 */
	Stream<InvocationStatus> findByTenantIdAndTriggerId(TenantId tenantId, TriggerId triggerId);

	/**
	 * Return invocation for given tenant and invocation id.
	 * @param tenantId tenant Id.
	 * @param id invocation Id
	 * @return Invocation statuses.
	 */
	Optional<InvocationStatus> findByTenantIdAndId(TenantId tenantId, UUID id);

	/**
	 * Start Invocation.
	 * @param invocationStatus invocation status.
	 */
	void start(InvocationStatus invocationStatus);

	/**
	 * Complete Invocation
	 * @param tenantId tenant Id.
	 * @param id invocation Id.
	 * @param completeInvocation CompleteInvocationInpu
	 */
	void complete(TenantId tenantId, UUID id, CompleteInvocationInput completeInvocation);

	/**
	 * Delete invocation by Id.
	 * Implementation should provide automatic deletion, time to live for items.
	 * This method can be used just like an utility function.
	 * @param tenantId tenant Id.
	 * @param id invocation Id.
	 */
	void delete(TenantId tenantId, UUID id);

}
