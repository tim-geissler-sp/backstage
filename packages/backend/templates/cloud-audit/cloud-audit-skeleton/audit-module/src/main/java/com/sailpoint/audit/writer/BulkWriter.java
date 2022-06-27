/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.writer;

import com.google.inject.Singleton;
import com.sailpoint.audit.service.BulkUploadAuditEventsService;
import com.sailpoint.audit.service.model.AuditUploadStatus;
import com.sailpoint.audit.util.BulkUploadUtil;
import com.sailpoint.mantis.platform.service.search.SearchUtil;
import com.sailpoint.mantis.platform.service.search.SyncTransformer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sailpoint.object.SailPointObject;
import sailpoint.tools.GeneralException;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static com.sailpoint.audit.service.BulkUploadAuditEventsService.EVENT_TYPE;

@Singleton
public abstract class BulkWriter {

	public static Log _log = LogFactory.getLog(BulkWriter.class);

	static final int MAX_UPLOAD_RETRIES = 3;

	BulkUploadUtil _bulkUploadUtil;

	SearchUtil _searchUtil;

	SyncTransformer _syncTransformer;

	public BulkWriter() {
		_bulkUploadUtil = null;
		_syncTransformer = null;
		_searchUtil = null;
	}

	public abstract void writeLine(SailPointObject object, String org, BulkUploadAuditEventsService.AuditTableNames tableName, boolean useCompression,
								   AuditUploadStatus status) throws GeneralException, IOException;

	public abstract void sendBatch(String org, BulkUploadAuditEventsService.AuditTableNames tableName, boolean useCompression,
						  AuditUploadStatus status) throws GeneralException, IOException;

	boolean areLocalDatesSame(long createdTime, long lastCreatedTime) {
		LocalDate createdLocalDate = Instant.ofEpochMilli(createdTime)
				.atZone(ZoneId.of("UTC")).toLocalDate();
		LocalDate lastCreatedLocalDate = Instant.ofEpochMilli(lastCreatedTime)
				.atZone(ZoneId.of("UTC")).toLocalDate();

		return createdLocalDate.getYear() == lastCreatedLocalDate.getYear() &&
				createdLocalDate.getMonth() == lastCreatedLocalDate.getMonth() &&
				createdLocalDate.getDayOfMonth() == lastCreatedLocalDate.getDayOfMonth();
	}

	static String bulkSyncLogMessage(String type, String message) {
		return SearchUtil.searchServiceLogMessage(SearchUtil.LogType.BULK_SYNC, type, message);
	}

	boolean upload(boolean useCompression, File tempFile, String s3Bucket, String s3Key, AuditUploadStatus status, String uploadType) {
		int retryCount = 0;
		boolean success = false;
		File uploadedFile = null;
		while (!success && retryCount < MAX_UPLOAD_RETRIES) {
			try {
				if (useCompression) {
					uploadedFile = _searchUtil.uploadSyncFile(EVENT_TYPE, tempFile, s3Bucket, s3Key);
				} else {
					uploadedFile = _searchUtil.uploadUncompressedFile(EVENT_TYPE, tempFile, s3Bucket, s3Key);
				}
				success = true;
			} catch (Exception e) {
				if (retryCount == MAX_UPLOAD_RETRIES) {
					_log.error(bulkSyncLogMessage(EVENT_TYPE, "max upload retries exceeded, terminating bulk upload"));
				}
				_log.error(bulkSyncLogMessage(EVENT_TYPE, "error uploading file, retry " + retryCount));
				retryCount++;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ignored) {
				}
			} finally {
				if (uploadedFile != null) {
					uploadedFile.delete();
				}
			}
		}
		if (success) {
			_log.debug(bulkSyncLogMessage(EVENT_TYPE, _bulkUploadUtil.getStatusString(uploadType+" file generated, uploading file", status, null)));
		} else {
			_log.error(bulkSyncLogMessage(EVENT_TYPE, _bulkUploadUtil.getStatusString(uploadType+" upload failed after "+MAX_UPLOAD_RETRIES+" retry attempts", status, null)));
		}
		if (tempFile!=null) {
			try {
				tempFile.delete();
			} catch (Exception e) {
				_log.error(bulkSyncLogMessage(EVENT_TYPE, "error deleting temp upload file"), e);
			}
		}
		return success;
	}
}
