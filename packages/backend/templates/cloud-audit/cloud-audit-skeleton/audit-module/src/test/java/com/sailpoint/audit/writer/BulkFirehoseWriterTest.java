/*
 * Copyright (C) 2021 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.writer;

import com.sailpoint.atlas.search.model.event.Event;
import com.sailpoint.atlas.service.MessageClientService;
import com.sailpoint.audit.service.BulkUploadAuditEventsService;
import com.sailpoint.audit.service.FirehoseService;
import com.sailpoint.audit.service.model.AuditUploadStatus;
import com.sailpoint.audit.util.BulkUploadUtil;
import com.sailpoint.audit.utils.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import sailpoint.object.AuditEvent;

import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BulkFirehoseWriterTest {

    private static final String ORG_NAME = "acme-solar";

    @Mock
    FirehoseService _firehoseService;

    @Mock
    MessageClientService _messageClientService;

    @Mock
    BulkUploadUtil _bulkUploadUtil;

    @Mock
    AuditUploadStatus _auditUploadStatus;

    @Mock
    AuditEvent _auditEvent;

    BulkFirehoseWriter _sut;

    @Before
    public void setup() throws Exception {
        _sut = new BulkFirehoseWriter(_bulkUploadUtil, _messageClientService, _firehoseService);

        Event event = TestUtils.getTestEvent();
        when(_bulkUploadUtil.convertToEvent(any(AuditEvent.class))).thenReturn(event);
    }

    @Test
    public void testWriteLine() throws Exception {
        _sut.writeLine(_auditEvent, ORG_NAME, BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT,
                false, _auditUploadStatus);

        Assert.assertEquals(1, _sut._typeRecordsMap.size());

        _sut.writeLine(_auditEvent, ORG_NAME, BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT,
                false, _auditUploadStatus);

        Assert.assertEquals(1, _sut._typeRecordsMap.size());
        Assert.assertEquals(2, _sut._typeRecordsMap.get("SOURCE_MANAGEMENT").size());
    }

    @Test
    public void testSendBatch() throws Exception {
        _sut.writeLine(_auditEvent, ORG_NAME, BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT,
                false, _auditUploadStatus);
        _sut.writeLine(_auditEvent, ORG_NAME, BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT,
                false, _auditUploadStatus);

        _sut.sendBatch(ORG_NAME, BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, false, _auditUploadStatus);

        verify(_firehoseService, times(1)).sendBatchToFirehose(any(List.class));
        verify(_bulkUploadUtil, times(1)).setCurrentUploadStatus(any(), any());

        Assert.assertEquals(0, _sut._typeRecordsMap.size());
    }
}
