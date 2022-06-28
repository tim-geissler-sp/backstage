/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.sailpoint.atlas.OrgDataProvider;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.service.AtomicMessageService;
import com.sailpoint.atlas.service.RemoteFileService;
import com.sailpoint.audit.message.BulkSyncPayload;
import com.sailpoint.audit.persistence.S3AuditEventEnvelope;
import com.sailpoint.audit.persistence.S3PersistenceManager;
import com.sailpoint.audit.service.model.AuditBulkSyncStatus;
import com.sailpoint.audit.util.AuditMetricUtil;
import com.sailpoint.audit.util.BulkUploadUtil;
import com.sailpoint.mantis.core.service.ConfigService;
import com.sailpoint.mantis.platform.service.search.SearchUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sailpoint.object.AuditEvent;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * This has not _yet_ been used actively in production. This class was intended to support
 * re-sync form S3/Athena back into Elastic Search; when we treated Athena as authoritiative.
 * This was wirtten even before we decided on the partition format we have landed on; this
 * might be from the JSON version.
 */
@Singleton
public class BulkSyncS3AuditEventsService {
	private static final Log _log = LogFactory.getLog(BulkSyncS3AuditEventsService.class);

	private final static String METRICS_BASE_PATH = "com.sailpoint.audit.bulk.sync.s3";

	@Inject
	Provider<AtomicMessageService> _atomicMessageService;

	@Inject
	AuditEventService _auditEventService;

	@Inject
	BulkUploadUtil _bulkUploadUtil;

	@Inject
	OrgDataProvider _orgDataProvider;

	@Inject
	ConfigService _configService;

	@Inject
	EventNormalizerService _eventNormalizerService;

	@Inject
	RemoteFileService _remoteFileService;

	@Inject
	SearchUtil _searchUtil;

	private AuditMetricUtil _auditMetricUtil = new AuditMetricUtil(METRICS_BASE_PATH);

	@VisibleForTesting
	final static String S3BUCKET = "orgDataS3Bucket";

	public void sync(BulkSyncPayload bulkSyncPayload, Supplier<Boolean> cancelledFunction) {
		AuditBulkSyncStatus status = new AuditBulkSyncStatus();
		boolean countOnly = bulkSyncPayload.isCountOnly();

		try {
			final String org = RequestContext.ensureGet().getOrg();
			final String tenantId = RequestContext.ensureGet().getTenantId().orElse(_orgDataProvider.ensureFind(org).getTenantId()
					.orElseThrow(()->new RuntimeException("No tenantId available for org: "+org)));

			LocalDateTime fromDate = LocalDateTime.parse(_bulkUploadUtil.getString("audit.start.date"));
			if (bulkSyncPayload.getArguments().containsKey("fromDate")) {
				String fromDateString = (String)bulkSyncPayload.getArguments().get("fromDate");
				fromDate = LocalDateTime.parse(fromDateString);
			}

			LocalDateTime toDate = LocalDateTime.now(ZoneId.of("UTC"));
			if (bulkSyncPayload.getArguments().containsKey("toDate")) {
				String toDateString = (String) bulkSyncPayload.getArguments().get("toDate");
				toDate = LocalDateTime.parse(toDateString);
			}

			status.setStartTime(System.nanoTime());

			_log.info(bulkSyncMessage(countOnly,"started"));

			S3PersistenceManager.iterateByDateRange(tenantId,
					Date.from(fromDate.toInstant(ZoneOffset.UTC)),
					Date.from(toDate.toInstant(ZoneOffset.UTC)), (objKey) -> {
				if (!isCancelled(cancelledFunction, "canceled")) {
					S3AuditEventEnvelope s3AuditEventEnvelope = S3PersistenceManager.getAuditEvent(objKey);
					sendAuditEvent(s3AuditEventEnvelope.getAuditEvent(), status);
				}
			});
		} catch (Exception e) {
			_log.error(bulkSyncMessage(countOnly, "unknown error."), e);
		}
		generateMetrics(status, countOnly);
	}

	private void sendAuditEvent(AuditEvent auditEvent, AuditBulkSyncStatus status) {
		_auditEventService.normalizeAndEmit(auditEvent);
		status.incrementTotalProcessed();
	}

	private void generateMetrics(AuditBulkSyncStatus status, boolean countOnly) {
		long endTime = System.nanoTime();
		long durationInMilli = (long) Math.ceil((TimeUnit.MILLISECONDS
				.convert(endTime - status.getStartTime(), TimeUnit.NANOSECONDS)));
		if (countOnly) {
			_auditMetricUtil.writeGauge(AuditMetricUtil.AUDIT_RECORD_COUNT, status.getTotalProcessed(), null);
			_auditMetricUtil.writeGauge(AuditMetricUtil.NON_WHITELISTED + "." + AuditMetricUtil.AUDIT_RECORD_COUNT, status.getTotalFiltered(), null);
		}

		_log.info(bulkSyncMessage(countOnly,"completed, total files processed: "+status.getTotalFiles()+
				", total records processed "+status.getTotalProcessed()+
				", total records filtered (non-whitelisted): "+status.getTotalFiltered()+
				", total duration "+durationInMilli));
	}

	private String bulkSyncMessage(boolean countOnly, String message) {
		return "bulk sync "+(countOnly?"count":"")+" for s3 to search "+message;
	}

	private boolean isCancelled(Supplier<Boolean> cancelledFunction, String message) {
		boolean isJobCancelled = cancelledFunction.get();
		if (isJobCancelled) {
			_log.info(message);
			return true;
		}
		return false;
	}

}
