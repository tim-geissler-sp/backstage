/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.OrgDataProvider;
import com.sailpoint.atlas.search.model.event.Event;
import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.audit.service.BulkUploadAuditEventsService;
import com.sailpoint.audit.service.EventNormalizerService;
import com.sailpoint.audit.service.model.AuditTimeAndIdsRecord;
import com.sailpoint.audit.service.model.AuditUploadStatus;
import com.sailpoint.audit.service.util.AuditUtil;
import com.sailpoint.mantis.core.service.ConfigService;
import com.sailpoint.mantis.core.service.CrudService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sailpoint.object.AuditEvent;
import sailpoint.tools.GeneralException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static com.sailpoint.audit.event.AuditEventS3Handler.AUDITEVENT_PARQUET_TIMESTAMP;

public class BulkUploadUtil {

	private static Log _log = LogFactory.getLog(BulkUploadUtil.class);

	@Inject
	AtlasConfig _atlasConfig;

	@Inject
	ConfigService _configService;

	@Inject
	CrudService _crudService;

	@Inject
	EventNormalizerService _eventNormalizerService;

	@Inject
	FeatureFlagService _featureFlagService;

	@Inject
	OrgDataProvider _orgDataProvider;

	@Inject
	AuditUtil _auditUtil;

	@VisibleForTesting
	static final String S3_BUCKET_ATTRIBUTE_NAME = "auditS3Bucket";

	@VisibleForTesting
	static final String SEARCH_TO_SYNC_STATUS = "SEARCH_TO_SYNC_STATUS";

	private static final String SLEEP_BETWEEN_BATCHES = "SLEEP_BETWEEN_BATCHES_MILLISECONDS";

	/**
	 * Returns a JSON string of an audit event object
	 * @param auditEvent
	 * @return
	 */
	public String auditTransform(AuditEvent auditEvent) {
		Event event = _eventNormalizerService.normalize(auditEvent, true);
		return event != null ? JsonUtils.toJsonExcludeNull(event) : null;
	}

	/**
	 * Returns a JSON string of an audit event object
	 * @param auditEvent
	 * @return
	 */
	public String auditTransformWhitelisted(AuditEvent auditEvent) {
		final Event event = _eventNormalizerService.normalize(auditEvent, true);
		if (event == null) {
			return null;
		}
		if (_auditUtil.isAlwaysAllowAudit(auditEvent)) {
			return JsonUtils.toJsonExcludeNull(event);
		} else {
			return null;
		}
	}

	public Event convertToEvent(AuditEvent auditEvent) {
		return _eventNormalizerService.normalize(auditEvent, true);
	}

	public String getS3BucketAttributeName(String org) throws GeneralException {
		final String s3BucketAttribute = _orgDataProvider.find(org).get().getAttribute(S3_BUCKET_ATTRIBUTE_NAME);
		if (s3BucketAttribute == null) {
			//It's a deliberate decision to throw an exception
			throw new GeneralException("s3 bucket is not available in org table in dynamo");
		}
		//S3 bucket attribute is in the format arn:aws:s3:::s3bucket
		String s3Bucket = s3BucketAttribute.substring(13);
		return s3Bucket;
	}

	public AuditTimeAndIdsRecord getLastSyncToSearchStatus(BulkUploadAuditEventsService.AuditTableNames tableName) {
		AuditTimeAndIdsRecord auditTimeAndIdsRecord;
		auditTimeAndIdsRecord = getStatus(BulkUploadAuditEventsService.PhaseStatus.SEARCH_SYNC.getPhaseStatus(tableName));
		return auditTimeAndIdsRecord;
	}

	public void setCurrentSyncToSearchStatus(BulkUploadAuditEventsService.AuditTableNames tableName, AuditTimeAndIdsRecord auditTimeAndIdsRecord) throws GeneralException {
		_configService.put(BulkUploadAuditEventsService.PhaseStatus.SEARCH_SYNC.getPhaseStatus(tableName), JsonUtils.toJson(auditTimeAndIdsRecord));
		_crudService.getContext().commitTransaction();
	}

	public AuditTimeAndIdsRecord getLastUploadStatus(BulkUploadAuditEventsService.AuditTableNames tableName) {
		AuditTimeAndIdsRecord auditTimeAndIdsRecord =
				getStatus(BulkUploadAuditEventsService.PhaseStatus.BULK_UPLOAD_STATUS.getPhaseStatus(tableName));

		return auditTimeAndIdsRecord;
	}

	public void setCurrentUploadStatus(BulkUploadAuditEventsService.AuditTableNames tableName, AuditTimeAndIdsRecord auditTimeAndIdsRecord) throws GeneralException {
		_configService.put(BulkUploadAuditEventsService.PhaseStatus.BULK_UPLOAD_STATUS.getPhaseStatus(tableName), JsonUtils.toJson(auditTimeAndIdsRecord));

		_crudService.getContext().commitTransaction();
	}

	public AuditTimeAndIdsRecord getFirstAuditParquetCheckpoint() {
		return getStatus(AUDITEVENT_PARQUET_TIMESTAMP);

	}

	public AuditTimeAndIdsRecord getSyncToSearchStart() {
		return getStatus(SEARCH_TO_SYNC_STATUS);
	}

	public void setSyncToSearchStart(AuditTimeAndIdsRecord status) throws GeneralException {
		_configService.put(SEARCH_TO_SYNC_STATUS, JsonUtils.toJson(status));
		_crudService.getContext().commitTransaction();
	}

	private AuditTimeAndIdsRecord getStatus(String key) {
		String uploadStatus = _configService.getString(key);

		return uploadStatus != null ? JsonUtils.parse(AuditTimeAndIdsRecord.class, uploadStatus) : null;
	}

	public AuditTimeAndIdsRecord getStatus(AuditUploadStatus status) {
		AuditTimeAndIdsRecord lastUploadStatus = new AuditTimeAndIdsRecord();
		lastUploadStatus.setTotalProcessed(status.getTotalProcessed());
		lastUploadStatus.setTotalUploaded(status.getTotalUploaded());
		lastUploadStatus.setIds(new ArrayList<>(status.getLastIds()));
		lastUploadStatus.setTimestamp(status.getLastCreatedTime());
		return lastUploadStatus;
	}

	public String getString(String key) {
		String defaultValue = null;
		try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
			Properties prop = new Properties();
			prop.load(input);
			defaultValue = prop.getProperty(key);
		} catch (Exception e) {
			_log.error("error loading default value from application.properties", e);
		}
		return _atlasConfig.getString(key, defaultValue);
	}

	public String singleQuoteString(String value) {
		return "'"+value+"'";
	}

	public String getIdsCSV(Set<String> idSet) {
		return idSet.stream().map(this::singleQuoteString).collect(Collectors.joining(","));
	}

	public String getStatusString(String message, AuditUploadStatus status, Long duration) {
		long lastBatchCreatedDate = status.getLastCreatedTime()!=0 ? status.getLastCreatedTime() : status.getCreatedTime();
		StringBuilder statusStringBuilder = new StringBuilder(message)
				.append(", lastCreated ").append(lastBatchCreatedDate)
				.append(", lastIds ").append(getIdsCSV(status.getLastIds()))
				.append(", whitelisted/uploadToSearch: ").append(status.isUploadToSearch())
				.append(", total uploaded ").append(status.getTotalUploaded())
				.append(", total processed ").append(status.getTotalProcessed())
				.append(", batch uploaded ").append(status.getBatchUploaded())
				.append(", batch processed ").append(status.getBatchProcessed())
				.append(", session errors ").append(status.getSessionErrors())
				.append(", session skipped ").append(status.getSessionSkipped())
				.append(", session uploaded ").append(status.getSessionUploaded())
				.append(", session processed ").append(status.getSessionProcessed())
				.append(", session limit ").append(status.getSessionLimit());
		if (duration!=null) {
			statusStringBuilder.append(", duration ")
					.append(duration);
		}
		return statusStringBuilder.toString();
	}

	public String getStartMessage(AuditUploadStatus status, long toDate, int remainingRecords) {
		StringBuilder startMessage = new StringBuilder("starting bulk upload")
				.append(", lastCreated ")
				.append(status.getLastCreatedTime())
				.append(", lastIds ")
				.append(getIdsCSV(status.getLastIds()))
				.append(", toDate ")
				.append(toDate)
				.append(", total remaining records to be processed: ")
				.append(remainingRecords)
				.append(", remaining records to be processed this session: ")
				.append(status.getSessionLimit());
		return startMessage.toString();
	}

	public void sleepBetweenBatch() {
		try {
			Thread.sleep(_atlasConfig.getInt(SLEEP_BETWEEN_BATCHES, 1000));
		} catch (InterruptedException ie) {
		}
	}

}
