/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.writer;

import com.google.inject.Singleton;
import com.sailpoint.atlas.idn.IdnMessageScope;
import com.sailpoint.atlas.messaging.client.MessagePriority;
import com.sailpoint.atlas.messaging.client.Payload;
import com.sailpoint.atlas.messaging.client.SendMessageOptions;
import com.sailpoint.atlas.service.AtomicMessageService;
import com.sailpoint.audit.service.BulkUploadAuditEventsService;
import com.sailpoint.audit.service.model.AuditUploadStatus;
import com.sailpoint.audit.util.BulkUploadUtil;
import com.sailpoint.mantis.core.service.ConfigService;
import com.sailpoint.mantis.platform.service.model.LastSync;
import com.sailpoint.mantis.platform.service.search.BulkSynchronizationService;
import com.sailpoint.mantis.platform.service.search.SearchUtil;
import com.sailpoint.mantis.platform.service.search.SyncTransformer;
import sailpoint.object.SailPointObject;
import sailpoint.tools.GeneralException;
import sailpoint.tools.Util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.sailpoint.audit.service.BulkUploadAuditEventsService.EVENT_TYPE;

@Singleton
public class BulkSearchWriter extends BulkWriter {

	protected PrintWriter _printWriter = null;
	protected File _tempUploadFile = null;

	private AtomicMessageService _atomicMessageService = null;
	private final static String S3BUCKET = "orgDataS3Bucket";
	private final static String S3_SYNC_FOLDER = "sync";
	private String _s3Bucket;

	BulkSearchWriter(AtomicMessageService atomicMessageService, BulkUploadUtil bulkUploadUtil, ConfigService configService,
					 SearchUtil searchUtil, SyncTransformer syncTransformer) {
		_atomicMessageService = atomicMessageService;
		_bulkUploadUtil = bulkUploadUtil;
		_searchUtil = searchUtil;
		_syncTransformer = syncTransformer;
		_s3Bucket = configService.getString(S3BUCKET);
	}

	public void writeLine(SailPointObject object, String org, BulkUploadAuditEventsService.AuditTableNames tableName, boolean useCompression,
						  AuditUploadStatus status) throws GeneralException, IOException {
		String json = null;
		try {
			json = _syncTransformer.transform(object);
		} catch (Exception e) {
			status.incrementSessionErrors();
			_log.error(bulkSyncLogMessage(EVENT_TYPE, "error transforming " + EVENT_TYPE
					+ " to sync for object id " + object.getId()), e);
		}
		if (_printWriter==null || _tempUploadFile==null)  {
			if (_tempUploadFile!=null) {
				sendBatch(org, tableName, useCompression, status);
			}
			_tempUploadFile = File.createTempFile("bulk_event_upload", ".json");
			_printWriter = new PrintWriter(Files.newBufferedWriter(_tempUploadFile.toPath()));
		}
		if (json != null) {
			_printWriter.println(json);
			status.incrementUploaded();
		} else {
			status.incrementSessionSkipped();
		}
	}

	public void sendBatch(String org, BulkUploadAuditEventsService.AuditTableNames tableName, boolean useCompression,
						  AuditUploadStatus status) throws GeneralException {
		if (status.getBatchUploaded() > 0) {
			if (_printWriter!=null) {
				_printWriter.flush();
				_printWriter.close();
				_printWriter = null;
			}
			long lastBatchCreatedDate = status.getLastCreatedTime()!=0 ? status.getLastCreatedTime() : status.getCreatedTime();
			String s3Key = String.format("%s/%s/bulk_event_sync_%s.zip", org, S3_SYNC_FOLDER, Util.uuid());
			LastSync lastSync = new LastSync();
			lastSync.setTime(new Date(lastBatchCreatedDate));
			upload(useCompression, _tempUploadFile, _s3Bucket, s3Key, status, "sync to search");
			sendSyncPayload("events", "event", _s3Bucket, s3Key, Collections.emptyMap(), lastSync, false, !status.isOnetimeSync());
		}
		status.setBatchProcessed(0);
		status.setBatchUploaded(0);
		if (!status.isOnetimeSync()) {
			_bulkUploadUtil.setCurrentSyncToSearchStatus(tableName, _bulkUploadUtil.getStatus(status));
		}
		_bulkUploadUtil.sleepBetweenBatch();
	}

	public void purge(String org) {
		Map<String,Object> arguments = new HashMap<>();
		arguments.put("purgeOrg", true);
		File tempUploadFile = null;
		File uploadedFile = null;
		String s3Key = String.format("%s/%s/bulk_event_sync_%s.zip", org, S3_SYNC_FOLDER, Util.uuid());
		try {
			LastSync lastSync = new LastSync();
			tempUploadFile = File.createTempFile("bulk_event_upload", ".json");
			uploadedFile = _searchUtil.uploadSyncFile(EVENT_TYPE, tempUploadFile, _s3Bucket, s3Key);
			sendSyncPayload("events", "event", _s3Bucket, s3Key, arguments, lastSync, true, true);
		} catch (Exception e) {
			_log.error(bulkSyncLogMessage(EVENT_TYPE, "error sending sync purge message"));
		} finally {
			if (uploadedFile!=null) {
				uploadedFile.delete();
			}
			if (tempUploadFile!=null) {
				tempUploadFile.delete();
			}
		}
	}

	private void sendSyncPayload(String indexKey, String type,
								  String s3Bucket, String s3Key, Map<String, Object> arguments, LastSync lastSync,
								 boolean purgeOrg, boolean nextOnly) {
		Map<String, Object> syncData = new HashMap<>();
		syncData.putAll(arguments);
		syncData.put("lastSync", lastSync.getTime());
		syncData.put("indexKey", indexKey);
		syncData.put("type", type);
		syncData.put("s3Bucket", s3Bucket);
		syncData.put("s3Key", s3Key);
		syncData.put("purgeOrg", purgeOrg);
		syncData.put("nextOnly", nextOnly);

		_atomicMessageService.send(IdnMessageScope.SEARCH,
				new Payload(BulkSynchronizationService.PAYLOAD_TYPE.BULK_SYNCHRONIZE_DOCUMENTS, syncData),
				new SendMessageOptions(MessagePriority.HIGH));
		_log.info(bulkSyncLogMessage(type, "sync message sent"));
	}
}
