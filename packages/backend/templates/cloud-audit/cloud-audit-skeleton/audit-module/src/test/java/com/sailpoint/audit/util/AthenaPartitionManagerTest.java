/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.util;

import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.idn.IdnMessageScope;
import com.sailpoint.atlas.messaging.client.JobSubmission;
import com.sailpoint.atlas.service.MessageClientService;
import com.sailpoint.audit.service.DataCatalogService;
import com.sailpoint.audit.utils.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AthenaPartitionManagerTest {

    private static final String ORG_NAME = "org_mock";
    private static final String POD_NAME = "pod_mock";

    @Mock
    DataCatalogService _athenaService;

    @Mock
    AtlasConfig _atlasConfig;

    @Mock
    MessageClientService _messageClientService;

    AthenaPartitionManager _sut;

    @Before
    public void setUp() {

        when(_atlasConfig.getString(eq("AER_AUDIT_ATHENA_DATABASE"))).thenReturn("mock-db");
        when(_atlasConfig.getString(eq("AER_AUDIT_PARQUET_DATA_S3_BUCKET"))).thenReturn("mock-bucket");

        RequestContext.set(TestUtils.setDummyRequestContext(ORG_NAME));

        _sut = new AthenaPartitionManager(ORG_NAME, POD_NAME, "2021-10-01",
                _atlasConfig, _athenaService, _messageClientService);
        _sut._athenaService = _athenaService;
        _sut._atlasConfig = _atlasConfig;
        _sut._messageClientService = _messageClientService;

    }

    @Test
    public void testHandleMessage() throws Exception {

        long prevSubs = AthenaPartitionManager.submissionCounter.get();
        long prevCmps = AthenaPartitionManager.completionCounter.get();

        _sut.submitWorkerThreadJob();
        int count = 0;
        do {
            Thread.sleep(100);
            assertTrue("Should complete in reasonable time, count:" + count, 100 > count++);
        } while (prevCmps == AthenaPartitionManager.completionCounter.get());

        verify(_athenaService).addPartitions(anyString(), anyString(), anyString(),anyString(), anyString());
        verify((_messageClientService), times(1))
                .submitJob(eq(IdnMessageScope.AUDIT), any(JobSubmission.class));

        assertTrue("submissionCounter expected 1+" + prevSubs +
                        ", got:" + AthenaPartitionManager.submissionCounter.get(),
                (1 + prevSubs) == AthenaPartitionManager.submissionCounter.get());

        assertTrue("completionCounter expected 1+" + prevCmps +
                        ", got:" + AthenaPartitionManager.completionCounter.get(),
                (1 + prevCmps) == AthenaPartitionManager.completionCounter.get());

    }

}
