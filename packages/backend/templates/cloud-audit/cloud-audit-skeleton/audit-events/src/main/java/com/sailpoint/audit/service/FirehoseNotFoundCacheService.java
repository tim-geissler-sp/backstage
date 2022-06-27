/*
 * Copyright (c) 2020.  SailPoint Technologies, Inc. All rights reserved.
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
public class FirehoseNotFoundCacheService {

	ReplicatedCache<String> _firehoseNotFoundCache;

	@Inject
	public FirehoseNotFoundCacheService(ReplicatedCacheManager _replicatedCacheManager) {
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

		_firehoseNotFoundCache = _replicatedCacheManager
				.createLoadingCache(_loadingCache, "firehoseNotFoundCache");
	}

	public void cacheFirehoseNotFound(String firehoseDataStreamName) {
		_firehoseNotFoundCache.put(firehoseDataStreamName, firehoseDataStreamName);
	}

	public boolean isFirehoseNotFound(String firehoseDataStreamName) {
		return  _firehoseNotFoundCache.contains(firehoseDataStreamName);
	}
}
