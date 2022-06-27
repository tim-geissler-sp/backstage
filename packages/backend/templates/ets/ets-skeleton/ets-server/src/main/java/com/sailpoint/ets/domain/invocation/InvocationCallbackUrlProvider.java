/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.domain.invocation;

import com.sailpoint.ets.domain.TenantId;

import java.util.UUID;

/**
 * InvocationCallbackUrlProvider
 */
public interface InvocationCallbackUrlProvider {

	/**
	 * Gets the callback URL for the specified invocation.
	 *
	 * @param tenantId The tenant ID.
	 * @param invocationId The invocation ID.
	 * @return The URL.
	 */
	String getCallbackUrl(TenantId tenantId, UUID invocationId);

}
