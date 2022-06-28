/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.service;

import com.sailpoint.atlas.OrgData;
import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.audit.service.model.SyncJobStatistics;
import com.sailpoint.mantis.platform.db.SailPointContextProvider;
import com.sailpoint.mantis.platform.db.SailPointContextProviderCache;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sailpoint.api.SailPointContext;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.function.Consumer;

import static com.sailpoint.audit.persistence.S3PersistenceManager.UTC_ZONE;
import static com.sailpoint.audit.service.SyncCisToS3Service.REDIS_SYNC_SET_KEY;

/**
 * Job class that oversees the synchronization of AuditEvent record data from CIS databases to S3 storage.
 *
 * This takes 2 items as input: (1) The Org name (OrgData) to run the sync against and (2) the
 * SailPointContextProviderCache that is used to create CIS connections to that Org's CIS database.
 *
 * When invoked this class iterates from a STARTING_DATE of 2014-01-01 UTC.  It then counts the number
 * of AuditEvents in CIS (both the main table and the archive table) day by day.  If the count is
 * non-zero then the job iterates hours in the day, getting the List<AuditEventId> for each Hour and synchronizing
 * them over to the CIS in concurrent worker threads.  This job waits for each day's data to complete before advancing
 * to the next day's data.
 *
 */
public class SyncCisToS3Job {

	public static final Log log = LogFactory.getLog(SyncCisToS3Job.class);

	// After this date all org/tenant's were writing to the S3 by-time bucket in addition to CIS.
	// We do not need to sync any records written after this date because they are already in S3.
	public static final Instant FINISHING_DATE = LocalDateTime.of(
			2021, 10, 1, 0,0,0,0
	).atZone(UTC_ZONE).toInstant();

	OrgData orgData;

	SailPointContextProviderCache spcpCache;

	RedisPool redisPool;

	/**
	 * Poll the SyncJobStatistics in a busy-waiting poller until a timeout or given sum of outputs arrives.
	 *
	 * @param desiredCount
	 * @param maxMsecDuration
	 * @param stats
	 */
	public static boolean waitForStatsCount(long desiredCount, long maxMsecDuration, SyncJobStatistics stats) {

		long startTime = System.currentTimeMillis();
		long duration = 0;
		long statsResult = 0;
		boolean desiredCountMet = false;
		boolean maxDurationMet = false;
		do {
			try {
				Thread.sleep(500);
				duration = System.currentTimeMillis() - startTime;
				// _log.info("Polling after duration:" + duration);
				desiredCountMet = desiredCount <= (statsResult = stats.getSumResultsCount());
				maxDurationMet = duration > maxMsecDuration;
			} catch (Exception ex) { /* swallow errors */ }
		} while ( (!desiredCountMet && !maxDurationMet) && (0 == stats.getExceptionCounter().get()) );

		log.info("CIS->S3 Poll Ended, desiredCount:" + desiredCount + " statsResult:" + statsResult +
				" duration:" + duration);

		// Inform the caller that we timed out.
		if (duration > maxMsecDuration) {
			log.warn("CIS->S3 Timed out waiting for " + desiredCount + " records, SyncJobStatistics:" +
					JsonUtils.toJson(stats));
			return false;
		}

		return true;
	}

	/**
	 * Performs an operation using the SailPointContext for the given Org.  This was flagrantly stolen from the
	 * <pre>com.sailpoint.mantis.test.integration.MantisIntegrationTest</pre> implementation of a method with the
	 * same name. This implementation does not return any value; the Consumer must interact with its surrounding
	 * environment to return data.
	 * @param contextConsumer
	 */
	public void runWithContext(Consumer<SailPointContext> contextConsumer) {
		SailPointContext context = null;
		SailPointContextProvider contextProvider = spcpCache.getContextProvider(orgData);
		try {
			context = contextProvider.create();
			contextConsumer.accept(context);
		} catch (Exception e) {
			throw new RuntimeException("Failed runWithContext()", e);
		} finally {
			contextProvider.release(context);
		}
	}

	/**
	 * Syncs the 24 hours of the day, breaking the hours of the day out into 24 different worker requests.
	 * @param startOfDay
	 */
	public SyncJobStatistics syncOrgCisToS3HoursOfDay(Instant startOfDay) {

		SyncJobStatistics dayStats = new SyncJobStatistics();

		long recordsExpected = 0;
		long startTimeStamp = System.currentTimeMillis();

		Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS).minus(1, ChronoUnit.MILLIS);

		Instant startTime = startOfDay;
		Instant upToTime = startOfDay;
		do {

			upToTime = startTime.plus(1, ChronoUnit.HOURS);

			// Items passed to the Lambda for CIS Querying must be final.
			final Date startDate = new Date(startTime.toEpochMilli());
			final Date upToDate = new Date(upToTime.toEpochMilli());
			final SyncJobTimeRange syncJobTimeRange = new SyncJobTimeRange();

			syncJobTimeRange.startTime = Instant.ofEpochMilli(startTime.toEpochMilli());
			syncJobTimeRange.upToTime  = Instant.ofEpochMilli(upToTime.toEpochMilli());

			// Count the number of records in the given time range.
			runWithContext(context -> {
				SyncCisToS3Queries cisQueries = new SyncCisToS3Queries(context);
				long numRecords = cisQueries.getAuditEventCountsByDateRange(startDate, upToDate);
				syncJobTimeRange.setExpectedRecordCount(numRecords);
			});

			// Pass any work to do off to the worker thread.
			if (0 != syncJobTimeRange.getExpectedRecordCount()) {
				log.info(String.format("CIS->S3 Dispatching %d AuditEvents for %s in hour %s -> %s",
						syncJobTimeRange.getExpectedRecordCount(), orgData.getOrg(), startTime, upToTime
				));
				recordsExpected += syncJobTimeRange.getExpectedRecordCount();
				SyncCisToS3Worker.syncAuditEventsCis2S3(spcpCache, orgData, dayStats, syncJobTimeRange);
			}

			startTime = startTime.plus(1, ChronoUnit.HOURS);

		} while (upToTime.isBefore(endOfDay) || upToTime.equals(endOfDay));

		// Sync'ing over a full day's worth of audit events should take less than a day!
		long waitTime = 1000 * 60 * 60 * 24; // Wait a day, max.
		boolean syncOk = waitForStatsCount(recordsExpected, waitTime, dayStats);
		if (!syncOk) {
			// Throw exception?  Scream and jump overboard? Email the government a complaint?
			// There's really nothing to do here except scream in the logs or peg a metric. Let's go logs
			// because CIS->S3 bulk synchronization is a sort-of "one time" thing; we don't plan to use forever.
			log.error(String.format("Failed to CIS->S3 sync %s, stats: %s", startOfDay, JsonUtils.toJson(dayStats)));
		}

		long durationTime = System.currentTimeMillis() - startTimeStamp;
		log.info(String.format("Completed to CIS->S3 day %s sync of %d records in %d msecs. Stats: %s",
				startOfDay,	recordsExpected,  durationTime, JsonUtils.toJson(dayStats)));

		return dayStats;
	}

	/**
	 * Counts the records that need to be sync'ed from CIS to S3 for the given time range.  The time range is expected
	 * to be a 24-hour block (from midnight to 11:59:59.999) for a given day.  If the count is zero then by-hour syncs
	 * are optimized out and skipped over.
	 *
	 * @param startStamp - Date at midnight UTC
	 * @param upToStamp  - Date for 11:59:59.999 of the same day.
	 * @return - a statistics POJO with the counts for the number of objects synchronized.
	 */
	public SyncJobStatistics syncOrgCisToS3OneDay(Instant startStamp, Instant upToStamp) {

		// Use a List<> to pass a final variable to pull a return from the runWithContext() lambda.
		final ArrayList<SyncJobStatistics> statsList = new ArrayList<>();

		runWithContext(context -> {

			SyncCisToS3Queries cisQueries = new SyncCisToS3Queries(context);

			long cisRecordCount = cisQueries.getAuditEventCountsByDateRange(
					new Date(startStamp.toEpochMilli()), new Date(upToStamp.toEpochMilli())
			);

			log.info(String.format("CIS->S3 Sync'ing %d AuditEvents for %s day %s -> %s",
					cisRecordCount, orgData.getOrg(), startStamp, upToStamp
			));

			if (0 != cisRecordCount) { statsList.add(syncOrgCisToS3HoursOfDay(startStamp)); }

		});

		if (0 == statsList.size()) statsList.add(new SyncJobStatistics());
		return statsList.get(0);
	}

	/**
	 * Synchronizes the given Org's data over S3 from CIS.  Processes day-by-dy, and hour-by-hour.  Does no
	 * data set reconciliation ahead of time; performs a straight "oldest first" linear copy of the data
	 * broken into "by hour" ID chucks which are distributed to worker threads.  Dynamically retrieves the earliest
	 * audit event's created date from CIS to set the start of the date range to iterate.
	 *
	 * TODO: Longer term - Do we need to do "by minute" chunks? Let's hope not for now, save that in needed.
	 *
	 * @param orgData
	 * @param spcpCache
	 */
	public void syncOrgCisToS3 (OrgData orgData, SailPointContextProviderCache spcpCache, RedisPool redisPool) {

		this.orgData = orgData;
		this.spcpCache = spcpCache;
		this.redisPool = redisPool;

		final Instant [] earliestDateArr = new Instant[1]; // Allow the lambda to un-unwrap here.
		runWithContext(context -> {
			SyncCisToS3Queries cisQueries = new SyncCisToS3Queries(context);
			Instant earliestDate = cisQueries.getEarliestAuditEventCreatedDate();
			earliestDateArr[0] = earliestDate;
		});
		log.info("CIS->S3 using earliestDate of: " + earliestDateArr[0] + " for org:" + orgData.getOrg());

		Instant startStamp = Instant.ofEpochMilli(earliestDateArr[0].toEpochMilli());
		SyncJobStatistics sumTotalStats = new SyncJobStatistics();
		long jobStartTime = System.currentTimeMillis();

		do {

			Instant upToStamp = startStamp.plus(1, ChronoUnit.DAYS).minus(1, ChronoUnit.MILLIS);

			SyncJobStatistics dayStats = syncOrgCisToS3OneDay(startStamp, upToStamp);
			sumTotalStats = sumTotalStats.mergeResults(dayStats);

			startStamp = startStamp.plus(1, ChronoUnit.DAYS);

		} while (startStamp.isBefore(FINISHING_DATE));

		long jobFinishTime = System.currentTimeMillis() - jobStartTime;
		log.info(String.format(
				"Completed CIS->S3 Sync for %s in %d msecs, stats: %s",
				orgData.getOrg(),
				jobFinishTime,
				JsonUtils.toJson(sumTotalStats)
		));

		// If the sync was not total and complete then re-queue the job to run it again. This is a simple, if
		// inefficient, form of error correction for bulk sync jobs. If one org fails, run it again!  If we
		// find ad-infinite-um loops happening we can roll AER to cancel the job.
		if (!sumTotalStats.isCompleteSuccess()) {

			log.warn(String.format(
					"Unsuccessful CIS->S3 Sync for %s, re-adding org to queue to re-process.",
					orgData.getOrg()
			));

			if (null != redisPool) {
				redisPool.exec(jedis -> jedis.sadd(REDIS_SYNC_SET_KEY, orgData.getOrg()));
			} else {
				log.error(String.format(
						"Unable to re-queue Org %s to re-process CIS->S3; no redisPool available!",
						orgData.getOrg()
				));
			}

		}

	}

}
