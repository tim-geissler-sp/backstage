/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.writer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.atlas.service.AtomicMessageService;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.atlas.service.MessageClientService;
import com.sailpoint.audit.service.FirehoseService;
import com.sailpoint.audit.util.BulkUploadUtil;
import com.sailpoint.mantis.core.service.ConfigService;
import com.sailpoint.mantis.platform.service.search.SearchUtil;
import com.sailpoint.mantis.platform.service.search.SyncTransformer;

import java.io.IOException;

@Singleton
public class BulkWriterFactory {
	@Inject
	AtomicMessageService _atomicMessageService;

	@Inject
	BulkUploadUtil _bulkUploadUtil;

	@Inject
	ConfigService _configService;

	@Inject
	FeatureFlagService _featureFlagService;

	@Inject
	FirehoseService _firehoseService;

	@Inject
	SearchUtil _searchUtil;

	@Inject
	MessageClientService _messageClientService;

	public BulkWriter getWriter(SyncTransformer transformer, boolean syncToSearch) throws IOException {
		if (syncToSearch) {
			return new BulkSearchWriter(_atomicMessageService, _bulkUploadUtil, _configService, _searchUtil, transformer);
		} else {
			return new BulkFirehoseWriter(_bulkUploadUtil, _messageClientService, _firehoseService);
		}
	}
}
