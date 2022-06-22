/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 *
 */

package com.sailpoint.audit.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sailpoint.audit.service.util.AuditUtil;
import com.sailpoint.metrics.annotation.Timed;
import io.prometheus.client.Gauge;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sailpoint.tools.GeneralException;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
public class MetricsPublisherService {

    private static Log log = LogFactory.getLog(MetricsPublisherService.class);

    private static final Gauge AUDIT_PUBLISH_COUNTS = Gauge.build()
            .name("audit_event_counts")
            .help("Number of audit events currently available for given date.")
            .labelNames("pod", "org", "type", "source", "date")
            .register();

    public static final ExecutorService execSvc = Executors.newFixedThreadPool( 32,
            new ThreadFactoryBuilder()
                    .setNameFormat("athena-metrics-%d")
                    .setDaemon(true) // Allow process exit when long-running S3 purge job is active.
                    .build()
    );

    // Counter incremented when a request is submitted for processing.
    @VisibleForTesting
    public static final AtomicLong submissionCounter = new AtomicLong(0L);

    // Counter incremented after a partition is successfully added.
    @VisibleForTesting
    public static final AtomicLong completionCounter = new AtomicLong(0L);

    @Inject
    @Named("Athena")
    DataCatalogService _athenaService;

    @Inject
    BulkUploadAuditEventsService _bulkUploadService;

    private void publishAuditEventsCount(String pod, String org, String type, String source, long count, String date) {
        AUDIT_PUBLISH_COUNTS.labels(pod, org, type, source.toLowerCase(), date).set(count);
    }

    public void publishAuditEventCounts(String dbName, String orgName, String date, String s3Bucket, String pod) {
        MetricsPublisherWorker mpWorker = new MetricsPublisherWorker(dbName, orgName, date, s3Bucket, pod);
        submissionCounter.incrementAndGet();
        execSvc.execute(mpWorker);
    }

    @Timed
    public long getAuditEventsCountFromAthena(String dbName, String orgName, String date, String s3Bucket) throws Exception {
        return _athenaService.getAuditEventsCount(dbName, AuditUtil.getOrgAuditAthenaTableName(orgName),
                date, s3Bucket);
    }

    @Timed
    public long getAuditEventsCountFromCis(String date) throws GeneralException, ParseException {
        ZonedDateTime fromDate = LocalDate.parse(date).atStartOfDay(ZoneId.of("GMT"));
        ZonedDateTime toDate = fromDate.plusDays(1);

        int cisCount = _bulkUploadService.countRecordsAudit(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT,
                fromDate.toEpochSecond() * 1000,
                toDate.toEpochSecond() * 1000,
                null);
        int cisArchiveCount = _bulkUploadService.countRecordsAudit(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT_ARCHIVE,
                fromDate.toEpochSecond() * 1000,
                toDate.toEpochSecond() * 1000,
                null);
        return cisCount + cisArchiveCount;
    }

    public class MetricsPublisherWorker implements Runnable {

        private String _dbName;
        private String _orgName;
        private String _date;
        private String _s3Bucket;
        private String _pod;

        public MetricsPublisherWorker(String dbName, String orgName, String date, String s3Bucket, String pod) {
            this._dbName = dbName;
            this._orgName = orgName;
            this._date = date;
            this._s3Bucket = s3Bucket;
            this._pod = pod;
        }

        @Override
        public void run() {

            try {

                long athenaEventsCount = getAuditEventsCountFromAthena(_dbName, _orgName, _date, _s3Bucket);
                long cisEventsCount = getAuditEventsCountFromCis(_date);

                log.debug(String.format(
                        "Audit event counts on %s, in athena: %s, in cis: %s ",
                        _date,
                        athenaEventsCount,
                        cisEventsCount
                ));

                if (athenaEventsCount != cisEventsCount) {
                    //TODO: Self healing code. Trigger a service which finds the missing id(s) and selectively sync them
                    log.info(String.format("Audit event counts are not equal on %s, in athena: %s, in cis: %s ",
                            _date, athenaEventsCount, cisEventsCount));
                }

                publishAuditEventsCount(_pod, _orgName, "AuditEventsCount", "Athena", athenaEventsCount, _date);
                publishAuditEventsCount(_pod, _orgName, "AuditEventsCount", "Cis", cisEventsCount, _date);

            } catch (Exception ex) {
                log.error("Failure getting AuditEvents Count", ex);
            } finally {
                completionCounter.incrementAndGet();
            }
        }
    }

}
