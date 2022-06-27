/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.service;

import com.google.inject.AbstractModule;
import com.sailpoint.atlas.cache.ReplicatedCacheManager;
import com.sailpoint.atlas.cache.message.ReplicatedCacheChannel;

public class AuditFirehoseServiceModule extends AbstractModule {
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void configure() {
		binder().requireExplicitBindings();

		bind(ReplicatedCacheChannel.class);
		bind(ReplicatedCacheManager.class);
		bind(FirehoseService.class);
		bind(FirehoseCacheService.class);
		bind(FirehoseNotFoundCacheService.class);
	}
}
