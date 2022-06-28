/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.message;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.messaging.server.MessageHandler;
import com.sailpoint.atlas.messaging.server.MessageHandlerContext;
import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.atlas.service.MessageClientService;
import com.sailpoint.audit.service.DataCatalogService;
import com.sailpoint.audit.service.FeatureFlags;
import com.sailpoint.audit.service.model.AddAthenaPartitionsDTO;
import com.sailpoint.audit.util.AthenaPartitionManager;
import com.sailpoint.metrics.annotation.Timed;

public class AddAthenaPartitions implements MessageHandler {

	public enum PAYLOAD_TYPE {
		ADD_ATHENA_PARTITIONS
	}

	@Inject
	FeatureFlagService _featureFlagService;

	@Inject
	AtlasConfig _atlasConfig;

	@Inject
	@Named("Athena")
	DataCatalogService _athenaService;

	@Inject
	MessageClientService _messageClientService;

	/**
	 *
	 * {@inheritDoc}
	 */
	@Override
 	@Timed
	public void handleMessage(MessageHandlerContext context) throws Exception {

		if( _featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, false) ) {
			AddAthenaPartitionsDTO addPartitionsPayload = JsonUtils.parse(AddAthenaPartitionsDTO.class,
					context.getMessage().getContentJson());
			AthenaPartitionManager.addPartition(addPartitionsPayload.getPartitionDate(), _atlasConfig,
					_athenaService, _messageClientService);
		}
	}

}
