/*
 * Copyright (c) 2019.  SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.service;

import com.sailpoint.atlas.cache.ReplicatedCache;
import com.sailpoint.atlas.cache.ReplicatedCacheManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DeletedOrgsCacheServiceTest {

	@Mock
	ReplicatedCacheManager _replicatedCacheManager;

	@Mock
	ReplicatedCache _replicatedCache;

	DeletedOrgsCacheService _sut;

	@Before
	public void setUp() {
		when(_replicatedCacheManager.createLoadingCache(any(), anyString())).thenReturn(_replicatedCache);
		_sut = new DeletedOrgsCacheService(_replicatedCacheManager);
	}

	@Test
	public void testOrgDeleted() {
		_sut.cacheDeletedOrg("test-org");

		when(_replicatedCache.contains(eq("test-org"))).thenReturn(true);
		Assert.assertTrue(_sut.isOrgDeleted("test-org"));
	}
}