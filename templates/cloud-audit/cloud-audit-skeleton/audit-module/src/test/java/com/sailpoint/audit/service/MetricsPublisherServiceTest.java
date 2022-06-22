/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 *
 */

package com.sailpoint.audit.service;

import com.sailpoint.audit.service.util.AuditUtil;
import com.sailpoint.audit.util.AthenaPartitionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import sailpoint.object.QueryOptions;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class MetricsPublisherServiceTest {

    private static final String dbName = "MOCK_DB";
    private static final String orgName = "MOCK_ORG";
    private static final String s3Bucket = "MOCK_S3_BUCKET";
    private static final String pod = "MOCK_POD";

    MetricsPublisherService _metricsService;

    @Mock
    DataCatalogService _athenaService;

    @Mock
    BulkUploadAuditEventsService _bulkUploadService;

    @Before
    public void setup() {
        _metricsService = new MetricsPublisherService();
        _metricsService._athenaService = _athenaService;
        _metricsService._bulkUploadService = _bulkUploadService;

    }

    @Test
    public void testPublishAuditEventCounts() throws Exception {
        String date = DateTimeFormatter.ISO_DATE.format(OffsetDateTime.now().toLocalDate());

        long prevCmps = MetricsPublisherService.completionCounter.get();

        _metricsService.publishAuditEventCounts(dbName, orgName, date, s3Bucket, pod);
        int count = 0;
        do {
            Thread.sleep(100);
            assertTrue("Should complete in reasonable time, count:" + count, 100 > count++);
        } while (prevCmps == MetricsPublisherService.completionCounter.get());

        verify(_athenaService, times(1)).getAuditEventsCount(eq(dbName), eq(AuditUtil.getOrgAuditAthenaTableName(orgName)), eq(date), eq(s3Bucket));
        verify(_bulkUploadService, times(2)).countRecordsAudit(any(), anyLong(), anyLong(), any());

    }
}
