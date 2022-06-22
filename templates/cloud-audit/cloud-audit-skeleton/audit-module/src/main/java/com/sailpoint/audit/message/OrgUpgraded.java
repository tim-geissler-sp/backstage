/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.message;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.idn.IdnMessageScope;
import com.sailpoint.atlas.messaging.client.Job;
import com.sailpoint.atlas.messaging.client.JobSubmission;
import com.sailpoint.atlas.messaging.client.Payload;
import com.sailpoint.atlas.messaging.server.MessageHandler;
import com.sailpoint.atlas.messaging.server.MessageHandlerContext;
import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.atlas.service.MessageClientService;
import com.sailpoint.atlas.task.schedule.service.TaskScheduleImportService;
import com.sailpoint.audit.service.DataCatalogService;
import com.sailpoint.audit.service.FeatureFlags;
import com.sailpoint.audit.service.model.AddAthenaPartitionsDTO;
import com.sailpoint.audit.service.util.AuditUtil;
import com.sailpoint.mantis.core.service.CrudService;
import com.sailpoint.metrics.annotation.Timed;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sailpoint.object.Configuration;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

public class OrgUpgraded implements MessageHandler {

	static Log _log = LogFactory.getLog(OrgUpgraded.class);

	public enum PAYLOAD_TYPE {
		ORG_UPGRADED
	}

	@Inject
	FeatureFlagService _featureFlagService;

	@Inject
	CrudService _crudService;

	@Inject @Named("Athena")
	DataCatalogService _athenaService;

	@Inject
	AtlasConfig _atlasConfig;

	@Inject
	TaskScheduleImportService _taskScheduleImportService;

	@Inject
	MessageClientService _messageClientService;

	/**
	 *
	 * {@inheritDoc}
	 */
	@Override
	@Timed
	public void handleMessage(MessageHandlerContext context) throws Exception {
		if( _featureFlagService.getBoolean(FeatureFlags.CLEANUP_BULKUPLOAD_CONFIG) ) {
			Configuration config = _crudService
					.findByName(Configuration.class, "CloudConfiguration").get();
			_crudService.withTransactionLock(Configuration.class, config.getId(),
					cleanupBulkUploadEntries());
		}
		if( _featureFlagService.getBoolean(FeatureFlags.SP_AUDIT_PROVISION_ATHENA_TABLE, false) ) {
			String orgName = RequestContext.ensureGet().getOrg();
			String dbName = _atlasConfig.getString("AER_AUDIT_ATHENA_DATABASE");
			String s3Bucket = _atlasConfig.getString("AER_AUDIT_PARQUET_DATA_S3_BUCKET");

			_athenaService.createTable(dbName, AuditUtil.getOrgAuditAthenaTableName(orgName), s3Bucket,
					AuditUtil.PARQUET_DATA_S3_PREFIX);
			_taskScheduleImportService.importSchedulesFromResource("add_athena_partitions_schedule.json");
		}

		if( _featureFlagService.getBoolean(FeatureFlags.PLTDP_ONETIME_BULKSYNC_CUSTOM) ) {
			BulkSyncPayload bulkSyncPayload = new BulkSyncPayload();

			Map<String, Object> arguments = new HashMap<>();
			arguments.put("fromDate", "2020-06-01");
			arguments.put("toDate", "2021-07-01");
			arguments.put("onetimeSync", true);
			arguments.put("batchSize", 10000);
			arguments.put("recordLimit", 50000000);

			bulkSyncPayload.setArguments(arguments);

			_log.info("Starting bulk sync of audit with payload: "+JsonUtils.toJson(bulkSyncPayload));

			JobSubmission syncJob = new JobSubmission(
					new Payload(BulkSyncAuditEvents.PAYLOAD_TYPE.BULK_SYNCHRONIZE_AUDIT_EVENTS, bulkSyncPayload));
			Job job = _messageClientService.submitJob(IdnMessageScope.AUDIT, syncJob);
		}

		if( _featureFlagService.getBoolean(FeatureFlags.PLTDP_ADD_ATHENA_PARTIONS_CUSTOM, false) ) {
			LocalDate startDate = LocalDate.of(2021, 9, 15);
			LocalDate endDate = LocalDate.of(2022, 2, 2);
			for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1))
			{
				AddAthenaPartitionsDTO payload = new AddAthenaPartitionsDTO(date.toString());
				JobSubmission addAthenaPartitionsJob = new JobSubmission(
						new Payload(AddAthenaPartitions.PAYLOAD_TYPE.ADD_ATHENA_PARTITIONS, payload));
				_messageClientService.submitJob(IdnMessageScope.AUDIT, addAthenaPartitionsJob);
			}
			_log.info("Triggered add athena partition for custom date range " + startDate + " to " + endDate);
		}
	}

	private Consumer<Configuration> cleanupBulkUploadEntries() {
		return configuration -> {
			Iterator<String> iterator = configuration.getAttributes().keySet().iterator();
			while(iterator.hasNext()) {
				String key = iterator.next();
				if (key.contains("bulkUpload") || key.contains("bulkSync"))	{
					iterator.remove();
				}
			}
		};
	}
}
