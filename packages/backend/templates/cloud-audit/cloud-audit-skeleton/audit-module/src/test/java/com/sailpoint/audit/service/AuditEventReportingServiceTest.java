/*
 * Copyright (c) 2022 SailPoint Technologies, Inc.  All rights reserved
 */

package com.sailpoint.audit.service;

import com.sailpoint.atlas.idn.RestClientProvider;
import com.sailpoint.atlas.idn.ServiceNames;
import com.sailpoint.audit.event.util.ResourceUtils;
import com.sailpoint.audit.service.model.AuditReportRequest;
import com.sailpoint.audit.service.model.AuditReportResponse;
import com.sailpoint.audit.util.AuditEventSearchQueryUtil;
import com.sailpoint.mantisclient.BaseRestClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuditEventReportingServiceTest {
    @InjectMocks
    private AuditEventReportingService auditEventReportingService;

    @Mock
    private RestClientProvider _restClientProvider;

    @Mock
    private BaseRestClient _baseRestClient;

    @Mock
    AuditEventSearchQueryUtil auditEventSearchQueryUtil;

    @Test
    public void testGenerateReport() {
        AuditReportRequest reportRequest = new AuditReportRequest();
        reportRequest.setUserId("testUserId");
        reportRequest.setSearchText("export");
        reportRequest.setDays(7);

        ResourceUtils resourceUtils = new ResourceUtils();
        AuditReportResponse auditReportResponseSample = resourceUtils.loadResource(AuditReportResponse.class, "reports/rde-audit-activity-report-response.json", "audit event activity");

        when(auditEventSearchQueryUtil.buildSearchQuery(anyString(), anyString())).thenReturn("query");

        when(_restClientProvider.getContextRestClient(eq(ServiceNames.RDE))).thenReturn(_baseRestClient);
        when(_baseRestClient.postJson(any(), anyString(), any())).thenReturn(auditReportResponseSample);

        AuditReportResponse auditReportResponse = auditEventReportingService.generateReport(reportRequest);
        assertNotNull(auditReportResponse);
    }
}
