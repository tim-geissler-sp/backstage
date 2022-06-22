/*
 *
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 *
 */

package com.sailpoint.audit.message;

import com.google.inject.Inject;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.messaging.server.MessageHandler;
import com.sailpoint.atlas.messaging.server.MessageHandlerContext;
import com.sailpoint.atlas.service.MessageClientService;
import com.sailpoint.audit.service.AuditReportService;
import com.sailpoint.audit.service.BulkUploadAuditEventsService;
import com.sailpoint.audit.service.SyncJobManager;
import com.sailpoint.audit.service.model.AuditTimeAndIdsRecord;
import com.sailpoint.audit.service.model.JobTypes;
import com.sailpoint.audit.util.BulkUploadUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sailpoint.object.AuditEvent;
import sailpoint.tools.GeneralException;

import java.util.Map;
import java.util.function.Supplier;

public class BulkUploadAuditEvents implements MessageHandler {
	public static Log _log = LogFactory.getLog(BulkUploadAuditEvents.class);

	public enum PAYLOAD_TYPE { BULK_UPLOAD_AUDIT_EVENTS }

	@Inject
	AtlasConfig _atlasConfig;

	@Inject
	AuditReportService _auditReportService;

	@Inject
	BulkUploadAuditEventsService _bulkUploadAuditEventsService;

	@Inject
	BulkUploadUtil _bulkUploadUtil;

	@Inject
	MessageClientService _messageClientService;

	@Inject
	SyncJobManager _syncJobManager;

	@Override
	public void handleMessage(MessageHandlerContext context) throws Exception {
		BulkUploadPayload bulkUploadPayload = context.getMessageContent(BulkUploadPayload.class);
		final Map<String, Object> arguments = bulkUploadPayload.getArguments();
		if (bulkUploadPayload.isReset()) {
			reset(bulkUploadPayload, context::isCancelled);
		} else if (bulkUploadPayload.isCountOnly()) {
			count(bulkUploadPayload);
		} else if (bulkUploadPayload.isCreateAuditArchiveIndex()) {
			createIndex();
		} else {
			upload(arguments, context::isCancelled);
		}
	}

	// This will go kick off a job, do 10k chunks up to 1M, then return to the sync job scheduler.  It will
	// keep chunking 1M at a time until it is done. There is a state/status saved here.  These event classes
	// are important; they are the caller of the bulk sync service.
	private void upload(Map<String, Object> arguments, Supplier<Boolean> cancelledFunction) throws Exception {
		try {
			int defaultBatchSize = (int)Math.round((double)arguments.getOrDefault("batchSize", (double)10000));
			int batchSize = _atlasConfig.getInt("BULK_UPLOAD_AUDIT_BATCH_SIZE", defaultBatchSize);

			int recordLimitDefault = (int)Math.round((double) arguments.getOrDefault("recordLimit", (double)1000000));
			int recordLimit = _atlasConfig.getInt("BULK_UPLOAD_AUDIT_RECORD_LIMIT", recordLimitDefault);

			boolean onetimeSync = (boolean)arguments.getOrDefault("onetimeSync", false);

			AuditTimeAndIdsRecord lastUploadStatus = _bulkUploadUtil.getLastUploadStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT);
			int sessionProcessed = 0;
			if (lastUploadStatus == null || !lastUploadStatus.isCompleted() || onetimeSync) {
				sessionProcessed = _bulkUploadAuditEventsService.bulkSyncAudit(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT,
						object -> _bulkUploadUtil.auditTransform((AuditEvent) object), cancelledFunction,
						recordLimit, batchSize, arguments);
			}

			int archiveSessionProcessed = 0;
			AuditTimeAndIdsRecord archiveLastUploadStatus = _bulkUploadUtil.getLastUploadStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT_ARCHIVE);
			if (((archiveLastUploadStatus == null || !archiveLastUploadStatus.isCompleted()) && sessionProcessed < recordLimit) || onetimeSync) {
				archiveSessionProcessed = _bulkUploadAuditEventsService.bulkSyncAudit(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT_ARCHIVE,
						object -> _bulkUploadUtil.auditTransform((AuditEvent)object), cancelledFunction,
						recordLimit - sessionProcessed, batchSize, arguments);
			}
			if ((archiveSessionProcessed == 0 && sessionProcessed == 0) && !onetimeSync) {
				String pod = RequestContext.ensureGet().getPod();
				String org = RequestContext.ensureGet().getOrg();
				_syncJobManager.setStatusComplete(pod, org, JobTypes.upload);
			}
		} catch (Exception e) {
			_log.error("error running bulk upload", e);
			throw e;
		}
	}

	private void reset(BulkUploadPayload payload, Supplier<Boolean> cancelledFunction) throws GeneralException {
		try {
			_bulkUploadAuditEventsService.purgeBulkSyncFiles(payload.getArguments(), cancelledFunction);
			_bulkUploadUtil.setCurrentUploadStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, null);
			_bulkUploadUtil.setCurrentUploadStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT_ARCHIVE, null);
			String pod = RequestContext.ensureGet().getPod();
			String org = RequestContext.ensureGet().getOrg();
			_syncJobManager.resetStatusComplete(pod, org, JobTypes.upload);
			_syncJobManager.resetStatusComplete(pod, org, JobTypes.upload_count);
		} catch (Exception e) {
			_log.error("error running bulk upload reset", e);
			throw e;
		}
	}

	private void count(BulkUploadPayload payload) {
		try {
			_bulkUploadAuditEventsService.countAuditRecords(payload.getArguments());
			_bulkUploadAuditEventsService.countAllAuditRecords(payload.getArguments());
		} catch (Exception e) {
			_log.error("error running bulk upload count", e);
			throw e;
		}
	}

	private void createIndex() {
		try {
			_bulkUploadAuditEventsService.createArchiveIndex();
		} catch (Exception e) {
			_log.error("error running bulk upload create index", e);
			throw e;
		}
	}
}
