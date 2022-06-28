/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.message;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.messaging.server.MessageHandler;
import com.sailpoint.atlas.messaging.server.MessageHandlerContext;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.atlas.task.schedule.service.TaskScheduleImportService;
import com.sailpoint.audit.service.DataCatalogService;
import com.sailpoint.audit.service.util.AuditUtil;
import com.sailpoint.metrics.annotation.Timed;

public class OrgCreated implements MessageHandler {

	public enum PAYLOAD_TYPE {
		ORG_CREATED
	}

	@Inject
	FeatureFlagService _featureFlagService;

	@Inject @Named("Athena")
	DataCatalogService _athenaService;

	@Inject
	AtlasConfig _atlasConfig;

	@Inject
	TaskScheduleImportService _taskScheduleImportService;

	/**
	 *
	 * {@inheritDoc}
	 */
	@Override
	@Timed
	public void handleMessage(MessageHandlerContext context) throws Exception {

		String orgName = RequestContext.ensureGet().getOrg();
		String dbName = _atlasConfig.getString("AER_AUDIT_ATHENA_DATABASE");
		String s3Bucket = _atlasConfig.getString("AER_AUDIT_PARQUET_DATA_S3_BUCKET");

		_athenaService.createTable(dbName, AuditUtil.getOrgAuditAthenaTableName(orgName), s3Bucket,
				AuditUtil.PARQUET_DATA_S3_PREFIX);
		_taskScheduleImportService.importSchedulesFromResource("add_athena_partitions_schedule.json");
	}

}
