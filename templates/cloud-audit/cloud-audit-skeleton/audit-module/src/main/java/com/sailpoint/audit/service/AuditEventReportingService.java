/*
 * Copyright (c) 2022. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.atlas.idn.RestClientProvider;
import com.sailpoint.atlas.idn.ServiceNames;
import com.sailpoint.audit.service.model.AuditReportRequest;
import com.sailpoint.audit.service.model.AuditReportResponse;
import com.sailpoint.audit.service.model.ReportDTO;
import com.sailpoint.audit.util.AuditEventSearchQueryUtil;
import com.sailpoint.audit.util.IndexNames;
import com.sailpoint.mantisclient.BaseRestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles Audit event reports
 */
@Singleton
public class AuditEventReportingService {
    public static final String SEARCH_EXPORT = "Search Export";
    public static final String REPORT_DEF_NAME = "reportDefName";
    public static final String NUM_DAYS = "numDays";
    public static final String INDICES = "indices";
    public static final String QUERY = "query";
    public static final String SORT = "sort";
    public static final String CREATED = "created";
    public static final String COLUMNS = "columns";
    public static final String CSV_OF_COLUMN_NAMES = "id,name,action,attributes.sourceName,actor.name,target.name,ipAddress,created,type";
    public static final String REPORTING_REPORTS_RUN = "reporting/reports/run";

    @Inject
    private RestClientProvider _restClientProvider;

    @Inject
    private AuditEventSearchQueryUtil auditEventSearchQueryUtil;

    /**
     * Make a call to RDE service to request 'search export' report
     *
     * @param auditReportRequest has userId and searchText to search for data
     * @return AuditReportResponse which has report_id (id) which can be used to download report
     */
    public AuditReportResponse generateReport(AuditReportRequest auditReportRequest) {
        String query = auditEventSearchQueryUtil.buildSearchQuery(auditReportRequest.getUserId(), auditReportRequest.getSearchText());

        ReportDTO reportConfig = new ReportDTO();
        reportConfig.setName(SEARCH_EXPORT);
        reportConfig.setReportName(SEARCH_EXPORT);
        reportConfig.setTaskDefName(SEARCH_EXPORT);
        reportConfig.setCompleted(false);
        Map<String, Object> args = new HashMap<>();
        args.put(REPORT_DEF_NAME, SEARCH_EXPORT);
        args.put(NUM_DAYS, auditReportRequest.getDays());
        args.put(INDICES, IndexNames.EVENTS.getIndexName());
        args.put(QUERY, query);
        args.put(SORT, CREATED);
        args.put(COLUMNS, CSV_OF_COLUMN_NAMES);
        reportConfig.setArguments(args);

        BaseRestClient rdeClient = _restClientProvider.getContextRestClient(ServiceNames.RDE);

        return rdeClient.postJson(AuditReportResponse.class, REPORTING_REPORTS_RUN, reportConfig);
    }
}
