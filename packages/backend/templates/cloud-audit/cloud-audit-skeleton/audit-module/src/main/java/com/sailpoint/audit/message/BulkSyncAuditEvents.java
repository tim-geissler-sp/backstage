/*
 *
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 *
 */

package com.sailpoint.audit.message;

import com.google.inject.Inject;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.messaging.server.MessageHandler;
import com.sailpoint.atlas.messaging.server.MessageHandlerContext;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.audit.service.BulkUploadAuditEventsService;
import com.sailpoint.audit.service.EventNormalizerService;
import com.sailpoint.audit.service.SyncJobManager;
import com.sailpoint.audit.service.model.AuditTimeAndIdsRecord;
import com.sailpoint.audit.service.model.JobTypes;
import com.sailpoint.audit.util.BulkUploadUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sailpoint.object.AuditEvent;

import java.util.Map;
import java.util.function.Supplier;

public class BulkSyncAuditEvents implements MessageHandler {
	private static final Log _log = LogFactory.getLog(BulkSyncAuditEvents.class);

	public enum PAYLOAD_TYPE { BULK_SYNCHRONIZE_AUDIT_EVENTS }

	@Inject
	AtlasConfig _atlasConfig;

	@Inject
	BulkUploadAuditEventsService _bulkUploadAuditEventsService;

	@Inject
	BulkUploadUtil _bulkUploadUtil;

	@Inject
	EventNormalizerService _eventNormalizerService;

	@Inject
	FeatureFlagService _featureFlagService;

	@Inject
	SyncJobManager _syncJobManager;

	@Override
	public void handleMessage(MessageHandlerContext context) throws Exception {
		BulkSyncPayload bulkSyncPayload = context.getMessageContent(BulkSyncPayload.class);
		final Map<String, Object> arguments = bulkSyncPayload.getArguments();
		if (bulkSyncPayload.isReset()) {
			resetSyncToSearch(bulkSyncPayload);
		} else if (bulkSyncPayload.isCountOnly()) {
			count(arguments);
		} else {
			syncToSearch(arguments, context::isCancelled);
		}
	}

	private void syncToSearch(Map<String, Object> arguments, Supplier<Boolean> cancelledFunction) throws Exception {
		try {
			arguments.put("syncToSearch", true);
			arguments.put("useCompression", true);

			int defaultBatchSize = Math.toIntExact((Long)arguments.getOrDefault("batchSize", 10000L));
			int batchSize = _atlasConfig.getInt("BULK_SYNC_AUDIT_BATCH_SIZE", defaultBatchSize);

			int recordLimitDefault = Math.toIntExact((Long)arguments.getOrDefault("recordLimit", 1000000L));
			int recordLimit = _atlasConfig.getInt("BULK_SYNC_AUDIT_RECORD_LIMIT", recordLimitDefault);

			boolean onetimeSync = (boolean)arguments.getOrDefault("onetimeSync", false);

			AuditTimeAndIdsRecord lastUploadStatus = _bulkUploadUtil.getLastSyncToSearchStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT);
			int sessionProcessed = 0;
			if (lastUploadStatus == null || !lastUploadStatus.isCompleted() || onetimeSync) {
				sessionProcessed = _bulkUploadAuditEventsService.bulkSyncAudit(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT,
						object -> _bulkUploadUtil.auditTransformWhitelisted((AuditEvent) object), cancelledFunction,
						recordLimit, batchSize, arguments);
			}

			int archiveSessionProcessed = 0;
			AuditTimeAndIdsRecord archiveLastUploadStatus = _bulkUploadUtil.getLastSyncToSearchStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT_ARCHIVE);
			if (((archiveLastUploadStatus == null || !archiveLastUploadStatus.isCompleted()) && sessionProcessed < recordLimit) || onetimeSync) {
				archiveSessionProcessed = _bulkUploadAuditEventsService.bulkSyncAudit(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT_ARCHIVE,
						object -> _bulkUploadUtil.auditTransformWhitelisted((AuditEvent)object), cancelledFunction,
						recordLimit - sessionProcessed, batchSize, arguments);
			}

			if ((archiveSessionProcessed == 0 && sessionProcessed == 0)) {
				String pod = RequestContext.ensureGet().getPod();
				String org = RequestContext.ensureGet().getOrg();
				_syncJobManager.setStatusComplete(pod, org, onetimeSync ? JobTypes.sync_to_search_onetime : JobTypes.sync_to_search);
			}
		} catch (Exception e) {
			_log.error("error running bulk sync", e);
			throw e;
		}
	}

	private void resetSyncToSearch(BulkSyncPayload payload) throws Exception {
		try {
			boolean purgeOrg = (boolean)payload.getArguments().getOrDefault("purgeOrg", false);
			if (purgeOrg) {
				_bulkUploadAuditEventsService.purgeSearchIndex(object -> _bulkUploadUtil.auditTransformWhitelisted((AuditEvent) object));
			}
			_bulkUploadUtil.setCurrentSyncToSearchStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, null);
			_bulkUploadUtil.setCurrentSyncToSearchStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT_ARCHIVE, null);
			_bulkUploadUtil.setSyncToSearchStart(null);
			String pod = RequestContext.ensureGet().getPod();
			String org = RequestContext.ensureGet().getOrg();
			_syncJobManager.resetStatusComplete(pod, org, JobTypes.sync_to_search_onetime);
			_syncJobManager.resetStatusComplete(pod, org, JobTypes.sync_to_search);
			_syncJobManager.resetStatusComplete(pod, org, JobTypes.sync_to_search_count);
		} catch (Exception e) {
			_log.error("error running bulk sync reset", e);
			throw e;
		}
	}

	private void count(Map<String, Object> arguments) {
		try {
			arguments.put("syncToSearch", true);
			_bulkUploadAuditEventsService.countAuditRecords(arguments);
		} catch (Exception e) {
			_log.error("error running bulk sync count", e);
			throw e;
		}
	}
}
