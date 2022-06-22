/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.aws;

import com.sailpoint.atlas.boot.discovery.DiscoveryProperties;
import com.sailpoint.ets.domain.TenantId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for  DiscoveryInvocationCallbackUrlProvider
 */
@RunWith(MockitoJUnitRunner.class)
public class DiscoveryInvocationCallbackUrlProviderTest {

	private TenantId _tenantId;
	private UUID _invocationId;
	private DiscoveryInvocationCallbackUrlProvider _provider;
	private String _url;

	@Test
	public void simpleTenant() {
		givenBaseUrlPattern("https://%s.api.cloud.sailpoint.com");
		givenTenantId("dev#acme-solar");
		givenInvocationId("0612a993-a2f8-4365-9dcc-4b5d620a64f0");

		whenTheUrlIsGenerated();

		thenTheUrlEquals("https://acme-solar.api.cloud.sailpoint.com/beta/trigger-invocations/0612a993-a2f8-4365-9dcc-4b5d620a64f0/complete");
	}

	@Test
	public void baseUrlContainsSlash() {
		givenBaseUrlPattern("https://%s.api.cloud.sailpoint.com/");
		givenTenantId("dev#acme-solar");
		givenInvocationId("0612a993-a2f8-4365-9dcc-4b5d620a64f0");

		whenTheUrlIsGenerated();

		thenTheUrlEquals("https://acme-solar.api.cloud.sailpoint.com/beta/trigger-invocations/0612a993-a2f8-4365-9dcc-4b5d620a64f0/complete");
	}

	private void givenBaseUrlPattern(String pattern) {
		DiscoveryProperties discoveryProperties = new DiscoveryProperties();
		discoveryProperties.setBaseUrlPattern(pattern);

		_provider = new DiscoveryInvocationCallbackUrlProvider(discoveryProperties);
	}

	private void givenTenantId(String id) {
		_tenantId = new TenantId(id);
	}

	private void givenInvocationId(String id) {
		_invocationId = UUID.fromString(id);
	}

	private void whenTheUrlIsGenerated() {
		_url = _provider.getCallbackUrl(_tenantId, _invocationId);
	}

	private void thenTheUrlEquals(String url) {
		assertEquals(url, _url);
	}





}
