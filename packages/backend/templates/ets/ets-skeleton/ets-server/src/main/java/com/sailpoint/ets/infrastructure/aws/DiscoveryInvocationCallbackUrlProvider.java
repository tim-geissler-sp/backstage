/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.aws;

import com.sailpoint.atlas.boot.core.web.TenantIdentifier;
import com.sailpoint.atlas.boot.discovery.DiscoveryProperties;
import com.sailpoint.ets.domain.invocation.InvocationCallbackUrlProvider;
import com.sailpoint.ets.domain.TenantId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

/**
 * InvocationCallbackUrlProvider that uses atlas discovery to build the API url.
 */
@Component
public class DiscoveryInvocationCallbackUrlProvider implements InvocationCallbackUrlProvider {

	private final static String INVOCATION_COMPLETED_URL = "beta/trigger-invocations/%s/complete";
	private final String _baseUrlPattern;

	@Autowired
	public DiscoveryInvocationCallbackUrlProvider(DiscoveryProperties discoveryProperties) {
		_baseUrlPattern = discoveryProperties.getBaseUrlPattern();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getCallbackUrl(TenantId tenantId, UUID invocationId) {
		TenantIdentifier identifier = TenantIdentifier.parse(tenantId.getValue());
		try {
			URL baseUrl = new URL(String.format(_baseUrlPattern, identifier.getOrg()));
			return new URL(baseUrl, String.format(INVOCATION_COMPLETED_URL, invocationId)).toString();
		} catch (MalformedURLException ex) {
			throw new IllegalStateException("error generating callback url", ex);
		}
	}

}
