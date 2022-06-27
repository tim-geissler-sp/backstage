/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.message;

import com.google.inject.Inject;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.messaging.server.MessageHandler;
import com.sailpoint.atlas.messaging.server.MessageHandlerContext;
import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.audit.service.FeatureFlags;
import com.sailpoint.audit.service.MetricsPublisherService;
import com.sailpoint.audit.service.model.PublishAuditCountsDTO;
import com.sailpoint.metrics.annotation.Timed;

public class PublishAuditCounts implements MessageHandler {

	public enum PAYLOAD_TYPE {
		PUBLISH_AUDIT_COUNTS
	}

	@Inject
	FeatureFlagService _featureFlagService;

	@Inject
	AtlasConfig _atlasConfig;

	@Inject
	MetricsPublisherService _metricsPublisherService;

	/**
	 *
	 * {@inheritDoc}
	 */
	@Override
	@Timed
	public void handleMessage(MessageHandlerContext context) throws Exception {

		if( _featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, false) ) {
			RequestContext requestContext = RequestContext.ensureGet();
			String orgName = requestContext.getOrg();
			String dbName = _atlasConfig.getString("AER_AUDIT_ATHENA_DATABASE");
			String s3Bucket = _atlasConfig.getString("AER_AUDIT_PARQUET_DATA_S3_BUCKET");
			PublishAuditCountsDTO publishAuditCountsDTO = JsonUtils.parse(PublishAuditCountsDTO.class,
					context.getMessage().getContentJson());
			_metricsPublisherService.publishAuditEventCounts(dbName, orgName,
					publishAuditCountsDTO.getPublishCountsDate(), s3Bucket, requestContext.getPod());
		}
	}

}
