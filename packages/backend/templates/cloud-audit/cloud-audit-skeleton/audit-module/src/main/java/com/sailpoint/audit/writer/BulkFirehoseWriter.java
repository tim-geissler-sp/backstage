/*
 * Copyright (C) 2021 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.writer;

import com.amazonaws.services.kinesisfirehose.model.Record;
import com.google.common.collect.Iterables;
import com.google.inject.Singleton;
import com.sailpoint.atlas.idn.IdnMessageScope;
import com.sailpoint.atlas.messaging.client.Job;
import com.sailpoint.atlas.messaging.client.JobSubmission;
import com.sailpoint.atlas.messaging.client.MessagePriority;
import com.sailpoint.atlas.messaging.client.Payload;
import com.sailpoint.atlas.messaging.client.SendMessageOptions;
import com.sailpoint.atlas.search.model.event.Event;
import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.atlas.service.MessageClientService;
import com.sailpoint.audit.message.AddAthenaPartitions;
import com.sailpoint.audit.service.BulkUploadAuditEventsService;
import com.sailpoint.audit.service.FirehoseService;
import com.sailpoint.audit.service.model.AddAthenaPartitionsDTO;
import com.sailpoint.audit.service.model.AuditUploadStatus;
import com.sailpoint.audit.util.BulkUploadUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sailpoint.object.AuditEvent;
import sailpoint.object.SailPointObject;
import sailpoint.tools.GeneralException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class BulkFirehoseWriter extends BulkWriter {

    public static Log _log = LogFactory.getLog(BulkFirehoseWriter.class);

    FirehoseService _firehoseService;

    MessageClientService _messageClientService;

    Map<String, ArrayList<Record>> _typeRecordsMap;

    public BulkFirehoseWriter(BulkUploadUtil bulkUploadUtil,
                              MessageClientService messageClientService,
                              FirehoseService firehoseService) {
        _bulkUploadUtil = bulkUploadUtil;
        _firehoseService = firehoseService;
        _messageClientService = messageClientService;
        _typeRecordsMap = new HashMap<>();

    }

    @Override
    public void writeLine(SailPointObject object,
                          String org,
                          BulkUploadAuditEventsService.AuditTableNames tableName,
                          boolean useCompression,
                          AuditUploadStatus status) throws GeneralException, IOException {
        Event event =  _bulkUploadUtil.convertToEvent((AuditEvent)object);

        //status.getLastCreatedTime() == 0 for the first record in the bulk upload
        if (!(status.getLastCreatedTime() == 0) && !areLocalDatesSame(status.getCreatedTime(), status.getLastCreatedTime())) {
            sendBatch(org, tableName, useCompression, status);
            addPartition(status);
        }

        try {
            if (event != null) {
                status.incrementUploaded();
                Record record = toRecord(JsonUtils.toJson(event));
                if (_typeRecordsMap.containsKey(event.getType())) {
                    _typeRecordsMap.get(event.getType()).add(record);
                } else {
                    ArrayList<Record> tempList = new ArrayList<>();
                    tempList.add(record);
                    _typeRecordsMap.put(event.getType(), tempList);
                }
            } else {
                status.incrementSessionSkipped();
            }
        } catch (Exception e) {
            _log.error("Error writing audit event to Map", e);
        }
    }

    @Override
    public void sendBatch(String org,
                          BulkUploadAuditEventsService.AuditTableNames tableName,
                          boolean useCompression, AuditUploadStatus status) throws GeneralException, IOException {

        try {
            for (Map.Entry<String, ArrayList<Record>> entry : _typeRecordsMap.entrySet()) {
                Iterables.partition(entry.getValue(), 500).forEach(batch -> {
                    _firehoseService.sendBatchToFirehose(batch);
                });
            };

            status.setBatchProcessed(0);
            status.setBatchUploaded(0);
            if (!status.isOnetimeSync()) {
                _bulkUploadUtil.setCurrentUploadStatus(tableName, _bulkUploadUtil.getStatus(status));
            }
            _bulkUploadUtil.sleepBetweenBatch();

            _typeRecordsMap = new HashMap<>();
        } catch (Exception e) {
            _log.error("Exception in sendBatch", e);
        }
    }

   private void addPartition(AuditUploadStatus status) {

        LocalDate createdLocalDate = Instant.ofEpochMilli(status.getLastCreatedTime())
                .atZone(ZoneId.of("GMT")).toLocalDate();

        _log.info("Adding partition for " + createdLocalDate);

       AddAthenaPartitionsDTO payload = new AddAthenaPartitionsDTO(createdLocalDate.toString());
       JobSubmission addAthenaPartitionsJob = new JobSubmission(
               new Payload(AddAthenaPartitions.PAYLOAD_TYPE.ADD_ATHENA_PARTITIONS, payload));

       //Enough delay for firehose-lambda to write to s3
       SendMessageOptions messageOptions = new SendMessageOptions(MessagePriority.LOW, 300);

       Job job = _messageClientService.submitJob(IdnMessageScope.AUDIT, addAthenaPartitionsJob, messageOptions);
    }

    private Record toRecord(String json) {
        Record record = new Record().withData(ByteBuffer.wrap(json.getBytes()));
        return record;

    }
}
