/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sailpoint.atlas.OrgData;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.atlas.security.AdministratorSecurityContext;
import com.sailpoint.audit.persistence.S3AuditEventEnvelope;
import com.sailpoint.audit.persistence.S3PersistenceManager;
import com.sailpoint.audit.service.model.SyncJobStatistics;
import com.sailpoint.mantis.platform.db.SailPointContextProviderCache;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sailpoint.api.SailPointContext;
import sailpoint.object.AuditEvent;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNull;

/**
 * Worker thread pool that handles sync'ing a single AuditEvent record from an org's CIS DB to the S3 by-time bucket.
 * The activity of sync'ing a single AuditEvent from CIS->S3 is a highly parallel-ize-able activity.  This is an
 * internal ETL job with a separate verification task; this means we can decouple persistence from job processing
 * and improve performance by using a highly concurrent query worker and S3 persistence I/O worker approach.
 */
public class SyncCisToS3Worker implements Runnable {

	public static final Log log = LogFactory.getLog(SyncCisToS3Worker.class);

	// Thread pool for concurrent querying CIS for the full AuditEvent from a list of AuditEvent IDs.
	public static final ExecutorService execSvc = Executors.newFixedThreadPool(32,
			new ThreadFactoryBuilder()
					.setNameFormat("cis2s3-sync-%d")
					.setDaemon(true) // Allow process exit when long-running S3 purge job is active.
					.build()
	);

	// Thread pool for concurrent persisting to S3.  Takes a deserialized AuditEvent and stores it in S3.
	public static final ExecutorService s3ExecSvc = Executors.newFixedThreadPool(128,
			new ThreadFactoryBuilder()
					.setNameFormat("cis2s3-s3io-%d")
					.setDaemon(true) // Allow process exit when long-running S3 purge job is active.
					.build()
	);

	// Thread pool for concurrent querying of CIS.  CIS DB connections use ThreadLocal, so you can not have multiple
	// DB connections in a single processing thread.  For each AuditEvent.ID that we query out of CIS we need a separate
	// CIS DB connection to pull back that AuditEvent.  We use that as an excuse to decouple Iteration of IDs from
	// getting the de-serialized AuditEvents, and from persisting them in S3.
	public static final ExecutorService cisQuerySvc = Executors.newFixedThreadPool(4,
			new ThreadFactoryBuilder()
					.setNameFormat("cis2s3-dbio-%d")
					.setDaemon(true) // Allow process exit when long-running S3 purge job is active.
					.build()
	);

	// Latch to count down the number of items we are awaiting to have persisted in S3.
	// This tracks a count of which AuditEvent IDs are still pending completion of persistence in S3.  Once this count
	// goes to zero the worker thread can exit, knowing that the complete set of all the AuditEvent records it read
	// from CIS have been persisted in S3.
	CountDownLatch countDownLatch = null;

	// Environmental cache of SailPontContextProviders. Given an OrgData these give the worker a SailPointContext.
	SailPointContextProviderCache sailPointContextProviderCache;

	// Which org should we be syncing the AuditEvent for?  This helps us set logging MDC and get a SailPointContext.
	OrgData orgData;

	// Peg counters tracking job progress.
	SyncJobStatistics syncJobStatistics;

	// The time range of ETL operation to cover and the epected number o records to copy from that time range.
	SyncJobTimeRange syncJobTimeRange;

	/**
	 * Runnable inner class that concurrently handles the relatively long duration I/O calls up to S3.
	 */
	public class S3InputOutputWorker implements Runnable {

		SyncJobStatistics syncJobStatistics;
		AuditEvent auditEvent;
		RequestContext requestContext;

		public S3InputOutputWorker (SyncJobStatistics syncJobStatistics, AuditEvent auditEvent, RequestContext reqCon) {
			requireNonNull(syncJobStatistics, "A valid SyncJobStatistics reference is required");
			requireNonNull(auditEvent, "A valid AuditEvent reference is required");
			requireNonNull(reqCon, "A valid RequestContext reference is required");
			this.syncJobStatistics = syncJobStatistics;
			this.auditEvent = auditEvent;
			this.requestContext = reqCon;
		}

		// Persists the AuditEvent in S3.  This is effectively an UPSERT type of persist operation.
		@Override
		public void run() {

			RequestContext.set(requestContext);  // Log messages and the S3Persistence manager need OrgData.

			S3AuditEventEnvelope envelope;

			try {

				envelope = S3PersistenceManager.saveAuditEvent(auditEvent);
				log.debug("Resulting S3AuditEventEnvelope:" + JsonUtils.toJson(envelope));

				if (envelope.isAlreadyExistedInS3()) {
					syncJobStatistics.getAlreadyExistedInTarget().incrementAndGet();
					log.debug("AlreadyExisted in S3 AuditEvent.id:" + auditEvent.getId());
				} else {
					syncJobStatistics.getNewlyCreatedInTarget().incrementAndGet();
					log.debug("NewlyCreated in S3 AuditEvent.id:" + auditEvent.getId());
				}

			} catch (Exception ex) {
				syncJobStatistics.getExceptionCounter().incrementAndGet();
				log.error("Failure communicating with S3", ex);
				return;
			}  finally {
				countDownLatch.countDown();
			}

		}

	}

	/**
	 * Constructor for a worker thread that Syncs a SyncJobTimeRange set of events to S3.  After construction
	 * this should be given to the executor service for processing in a worker thread.
	 * @param spcpc - SailPointContextProviderCahce
	 * @param orgData - org for which we are sync'ing records
	 * @param syncJobStatistics - peg counters updated during processing
	 * @param syncJobTimeRange - start, end, num records
	 */
	private SyncCisToS3Worker(SailPointContextProviderCache spcpc, OrgData orgData,
							  SyncJobStatistics syncJobStatistics, SyncJobTimeRange syncJobTimeRange) {
		this.sailPointContextProviderCache = spcpc;
		this.orgData = orgData;
		this.syncJobStatistics = syncJobStatistics;
		this.syncJobTimeRange = syncJobTimeRange;
	}

	public static void syncAuditEventsCis2S3 (SailPointContextProviderCache spcpc, OrgData orgData,
											  SyncJobStatistics syncJobStats, SyncJobTimeRange syncJobTimeRange) {

		// Pre-flight sanity checks to pass context to the worker thread.  Checking now allows any
		// exceptions to be caught by the higher level Service before passing off to the worker thread.
		requireNonNull(spcpc, "A valid SailPointContextProviderCache reference is required.");

		requireNonNull(orgData, "A valid orgData reference is required.");
		requireNonNull(orgData.getOrg(), "A valid orgData.org String reference is required.");
		if (0 == orgData.getOrg().length()) {
			throw new RuntimeException("A valid orgData.org String with content must be provided.");
		}
		if (!orgData.getTenantId().isPresent()) {
			throw new RuntimeException("A valid orgData.tenantId String reference is required.");
		}
		if (0 == orgData.getTenantId().get().length()) {
			throw new RuntimeException("A valid orgData.tenantId String with content must be provided.");
		}

		requireNonNull(syncJobStats, "A valid syncJobStatistics reference is required.");
		requireNonNull(syncJobTimeRange, "A valid syncJobTimeRange reference is required.");

		// Construct the work request.  At this point Guice will inject the appropriate context provider.
		SyncCisToS3Worker workRequest = new SyncCisToS3Worker(spcpc, orgData, syncJobStats, syncJobTimeRange);

		// Pass the work off to the worker thread.
		execSvc.execute(workRequest);
		syncJobStats.getSubmittedToQueue().addAndGet(syncJobTimeRange.getExpectedRecordCount());

	}

	/**
	 * Process the items in the List of AuditEvent IDs to sync across. Update the job statistics as we iterate.
	 * @param idQueryContext - The org's SailPointContext to iterate the AuditEvent IDs from CIS.
	 */
	private void iterateEventsToSync(SailPointContext idQueryContext) {

		SyncCisToS3Queries cisIdQueries = new SyncCisToS3Queries(idQueryContext);

		countDownLatch = new CountDownLatch(Math.toIntExact(syncJobTimeRange.getExpectedRecordCount()));

		final AtomicLong readFromDbCount = new AtomicLong(0);

		cisIdQueries.getAuditEventIdCreatedByDateRange(
				Date.from(syncJobTimeRange.getStartTime()),
				Date.from(syncJobTimeRange.getUpToTime()),
				(auditEventId, createdDate) -> {

					readFromDbCount.incrementAndGet();
					syncJobStatistics.getAdoptedByWorkerThread().incrementAndGet();

					log.debug("AuditEvent.id: " + auditEventId + " createdDate:" + createdDate.toInstant().toString());

					cisQuerySvc.execute(() -> {

						// Set the RequestContext so our log messages present MDC data appropriately.
						RequestContext requestContext = new RequestContext();
						requestContext.setOrgData(orgData);
						requestContext.setSecurityContext(new AdministratorSecurityContext());
						RequestContext.set(requestContext);

						SailPointContext aeQueryContext = null;
						try {
							// Get secoond context + JDBC connection for this particular Org/tenant.  The other reads
							// the ResultSet of AuditEvent IDs, this second retrieves singular AuditEvent records.
							aeQueryContext = sailPointContextProviderCache.getContextProvider(orgData).create();

							SyncCisToS3Queries cisAeQueries = new SyncCisToS3Queries(aeQueryContext);

							// Get the audit event via a second Context to Query in MySQL. From MySQL's docs: "You must
							// read all of the rows in the result set (or close it) before you can issue any other
							// queries on the connection, or an exception will be thrown.", so we need 2 Connections.
							AuditEvent auditEvent = cisAeQueries.getAuditEvent(auditEventId);
							if (null == auditEvent) {
								log.error("Unable to find AuditEvent.id:" + auditEventId + " to sync!");
								syncJobStatistics.getMissingFromSource().incrementAndGet();
								countDownLatch.countDown(); // No record to persist in S3; decrement the counter.
								return;
							}

							log.debug("Retrieved AuditEvent.id:" + auditEvent.getId());

							// Dispatch the AuditEvent off to be persisted in S3 in another worker thread.
							// The countDownLatch is decremented in a finally{} block in the S3 I/O Worker.
							S3InputOutputWorker s3IoWorker = new S3InputOutputWorker(
									syncJobStatistics, auditEvent, RequestContext.ensureGet()
							);
							s3ExecSvc.execute(s3IoWorker);

						} finally {
							if (null != aeQueryContext) {
								sailPointContextProviderCache.getContextProvider(orgData).release(aeQueryContext);
							}
						}

					});

				}
		);

		// Inventory the difference between the count of AuditEvents returned from the DB and now.
		// Remove those from the countdown latch and report them back via the job stats.
		if (readFromDbCount.get() != syncJobTimeRange.getExpectedRecordCount()) {
			log.warn(String.format("CIS->S3 expected %d from DB, only retrieved %d",
					readFromDbCount.get(),
					syncJobTimeRange.getExpectedRecordCount())
			);
			long difference = syncJobTimeRange.getExpectedRecordCount() - readFromDbCount.get();
			syncJobStatistics.getAdoptedByWorkerThread().addAndGet(difference);
			syncJobStatistics.getMissingFromSource().addAndGet(difference);
			for (int i=0; i<difference; i++) countDownLatch.countDown();
		}

		// Wait for all the AuditEvent records to get persisted in S3 before returning to the caller.
		try {
			countDownLatch.await();
		} catch (InterruptedException e) {
			log.error("CIS->S3 interrupted while waiting for S3 I/O workers to complete", e);
		}

	}

	/**
	 * Work carried out in a worker thread. Establish context, get the AuditEvents from CIS, then persist it into S3.
	 */
	@Override
	public void run() {

		// Set the RequestContext so our log messages present MDC data appropriately.
		RequestContext requestContext = new RequestContext();
		requestContext.setOrgData(orgData);
		requestContext.setSecurityContext(new AdministratorSecurityContext());
		RequestContext.set(requestContext);

		SailPointContext idQueryContext = null;
		try {

			// Get two contexts + JDBC connection for this particular Org/tenant.  The first reads
			// the ResultSet of AuditEvent IDs, the second retrieves singular AuditEvent records.
			idQueryContext = sailPointContextProviderCache.getContextProvider(orgData).create();

			// When the context is safely constructed we can iterate the list of items to process.
			iterateEventsToSync(idQueryContext);

		} finally {
			if (null != idQueryContext) {
				sailPointContextProviderCache.getContextProvider(orgData).release(idQueryContext);
			}
		}

	}

}
