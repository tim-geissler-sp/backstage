/*
 * Copyright (c) 2019.  SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.atlas.cache.ReplicatedCache;
import com.sailpoint.atlas.cache.ReplicatedCacheManager;

import java.util.concurrent.TimeUnit;

@Singleton
public class DeletedOrgsCacheService {

	ReplicatedCache<String> _deletedOrgsCache;

	@Inject
	public DeletedOrgsCacheService(ReplicatedCacheManager _replicatedCacheManager) {
		LoadingCache<String, String> _loadingCache = CacheBuilder.newBuilder()
				.maximumSize(100)
				.expireAfterWrite(15, TimeUnit.MINUTES)
				.build(
						new CacheLoader<String, String>() {
							@Override
							public String load(String index)  {
								return index;
							}
						}
				);

		_deletedOrgsCache = _replicatedCacheManager
				.createLoadingCache(_loadingCache, "deletedOrgsCache");
	}

	public void cacheDeletedOrg(String org) {
		_deletedOrgsCache.put(org, org);
	}

	public boolean isOrgDeleted(String org) {
		return  _deletedOrgsCache.contains(org);
	}
}
