/*
  * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
  */
package com.sailpoint.audit.message;

import com.google.inject.Inject;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.messaging.server.MessageHandler;
import com.sailpoint.atlas.messaging.server.MessageHandlerContext;
import com.sailpoint.audit.service.BulkSyncS3AuditEventsService;
import com.sailpoint.audit.service.SyncJobManager;
import com.sailpoint.audit.service.model.JobTypes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.function.Supplier;

public class BulkSyncS3AuditEvents implements MessageHandler {
	private static final Log _log = LogFactory.getLog(BulkSyncS3AuditEvents.class);

	public enum PAYLOAD_TYPE { BULK_SYNCHRONIZE_S3_AUDIT_EVENTS }

	@Inject
	BulkSyncS3AuditEventsService _bulkSyncS3AuditEventsService;

	@Inject
	SyncJobManager _syncJobManager;

	@Override
	public void handleMessage(MessageHandlerContext context) {
		BulkSyncPayload bulkSyncPayload = context.getMessageContent(BulkSyncPayload.class);
		if (bulkSyncPayload.isReset()) {
			reset();
		} else if (bulkSyncPayload.isCountOnly()) {
			execute(JobTypes.sync_s3_count, bulkSyncPayload, context::isCancelled);
		} else {
			execute(JobTypes.sync_s3, bulkSyncPayload, context::isCancelled);
		}
	}

	private void reset() {
		try {
			String pod = RequestContext.ensureGet().getPod();
			String org = RequestContext.ensureGet().getOrg();
			_syncJobManager.resetStatusComplete(pod, org, JobTypes.sync_s3);
			_syncJobManager.resetStatusComplete(pod, org, JobTypes.sync_s3_count);
		} catch (Exception e) {
			_log.error("error running bulk "+JobTypes.sync_s3_reset, e);
			throw e;
		}
	}

	private void execute(JobTypes jobType, BulkSyncPayload bulkSyncPayload, Supplier<Boolean> cancelledFunction) {
		try {
			_bulkSyncS3AuditEventsService.sync(bulkSyncPayload, cancelledFunction);
		} catch (Exception e) {
			_log.error("error running bulk "+jobType.name(), e);
			throw e;
		}
	}
}
