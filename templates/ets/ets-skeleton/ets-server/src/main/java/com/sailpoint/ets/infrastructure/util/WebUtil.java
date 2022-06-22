/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.util;

import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.boot.core.web.TenantIdentifier;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.iris.client.EventHeaders;

import java.util.HashMap;
import java.util.Map;

/**
 * WebUtil
 */
public class WebUtil {

	/**
	 * Gets the current tenant ID from the RequestContext.
	 *
	 * @return The tenant ID.
	 */
	public static TenantId getCurrentTenantId() {
		RequestContext rc = RequestContext.ensureGet();
		TenantIdentifier identifier = new TenantIdentifier(rc.ensurePod(), rc.ensureOrg());
		return new TenantId(identifier.toString());
	}

	/**
	 * Gets the headers from the RequestContext.
	 * Since ETS uses the event bus for intra-service messaging, we use the Event Header names.
	 *
	 * @return The headers.
	 */
	public static Map<String, String> getHeaders() {
		RequestContext rc = RequestContext.ensureGet();

		Map<String, String> headers = new HashMap<>();
		headers.put(EventHeaders.REQUEST_ID, rc.getId());
		headers.put(EventHeaders.ORG, rc.getOrg());
		headers.put(EventHeaders.POD, rc.getPod());
		headers.put(EventHeaders.TENANT_ID, rc.getTenantId().orElse(null));

		return headers;
	}
}
