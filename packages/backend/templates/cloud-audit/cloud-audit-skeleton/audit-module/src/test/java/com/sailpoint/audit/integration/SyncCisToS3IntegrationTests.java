/*
 * Copyright (c) 2021.  SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.integration;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.event.AtlasEventPlugin;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.health.AtlasHealthPlugin;
import com.sailpoint.atlas.idn.DevOrgDataProvider;
import com.sailpoint.atlas.logging.AtlasDynamicLoggingPlugin;
import com.sailpoint.atlas.metrics.AtlasMetricsPlugin;
import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.atlas.service.AtomicMessageService;
import com.sailpoint.atlas.service.ServiceFactory;
import com.sailpoint.atlas.task.schedule.service.TaskScheduleClientModule;
import com.sailpoint.audit.AuditEventPlugin;
import com.sailpoint.audit.AuditModulePlugin;
import com.sailpoint.audit.event.DomainEventPlugin;
import com.sailpoint.audit.persistence.S3PersistenceManager;
import com.sailpoint.audit.service.BulkUploadAuditEventsService;
import com.sailpoint.audit.service.DeletedOrgsCacheService;
import com.sailpoint.audit.service.SyncCisToS3Job;
import com.sailpoint.audit.service.SyncCisToS3Queries;
import com.sailpoint.audit.service.SyncCisToS3Service;
import com.sailpoint.audit.service.SyncCisToS3Worker;
import com.sailpoint.audit.service.SyncJobTimeRange;
import com.sailpoint.audit.service.model.SyncJobStatistics;
import com.sailpoint.audit.utils.TestUtils;
import com.sailpoint.featureflag.impl.MockFeatureFlagClient;
import com.sailpoint.mantis.core.service.CrudService;
import com.sailpoint.mantis.core.service.model.AuditEventActions;
import com.sailpoint.mantis.event.MantisEventHandlerModule;
import com.sailpoint.mantis.platform.MantisApplication;
import com.sailpoint.mantis.platform.db.SailPointContextProviderCache;
import com.sailpoint.mantis.test.integration.IntegrationTestApplication;
import com.sailpoint.mantis.test.integration.MantisIntegrationTest;
import com.sailpoint.mantisclient.IdnAtlasClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sailpoint.object.AuditEvent;
import sailpoint.tools.GeneralException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.sailpoint.audit.persistence.S3PersistenceManager.UTC_ZONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests that exercise the worker thread layer of S3 to CIS bulk synchronization. This carries out various permutations
 * of the CIS->S3 sync process.
 */
public class SyncCisToS3IntegrationTests extends MantisIntegrationTest {

	public static final Log _log = LogFactory.getLog(S3PersistenceIntegrationTests.class);

	public static final String _tenantId = UUID.randomUUID().toString();

	// How far back to generate test data and look for audit events to sync over.
	public static final long NUM_DAYS_BACK = 730;
	public static final long MSECS_PER_DAY = 1000 * 60 * 60 * 24;

	Random random = new Random(System.currentTimeMillis());

	AtomicMessageService _atomicMessageService;

	EventService _eventService;

	IdnAtlasClient _idnAtlasClient;

	DeletedOrgsCacheService _deletedOrgsCache;

	BulkUploadAuditEventsService _bulkUploadAuditEventsService;

	CrudService _crudService;

	SyncCisToS3Service _syncCisToS3Service;

	@Override
	protected MantisApplication createMantisApplication() {
		return new IntegrationTestApplication() {{
			registerPlugin(new AtlasEventPlugin());
			registerPlugin(new AtlasHealthPlugin());
			registerPlugin(new AtlasMetricsPlugin());

			registerPlugin(new AuditEventPlugin());
			registerPlugin(new AuditModulePlugin());

			registerPlugin(new DomainEventPlugin());
			registerPlugin(new AtlasDynamicLoggingPlugin());

			addServiceModule(new MantisEventHandlerModule());
			addServiceModule(new TaskScheduleClientModule());
		}};
	}

	@Before
	public void initializeApplication() throws Exception {

		System.setProperty("sessionFactory.hibernateProperties.hibernate.show_sql", "true");

		super.initializeApplication();

		TestUtils.setDummyRequestContext(_tenantId, "acme-solar", "dev");

		SyncCisToS3Queries.setIsMySqlDb(false); // Tell the integration test we're using H2.

		_application.getRestPort().ifPresent(restPort -> {
			_idnAtlasClient = new IdnAtlasClient(
					String.format("http://localhost:%d", restPort),
					"dev",
					"acme-solar",
					DevOrgDataProvider.DEV_API_KEY,
					null,
					getHttpClient());
		});

		_restClient = _idnAtlasClient;

		_eventService = ServiceFactory.getService(EventService.class);

		_atomicMessageService = ServiceFactory.getService(AtomicMessageService.class);

		_deletedOrgsCache = ServiceFactory.getService(DeletedOrgsCacheService.class);

		_bulkUploadAuditEventsService = ServiceFactory.getService(BulkUploadAuditEventsService.class);

		_syncCisToS3Service = ServiceFactory.getService(SyncCisToS3Service.class);

		MockFeatureFlagClient featureFlagClient = getFeatureFlagClient();

		featureFlagClient.setBoolean("WRITE_AUDIT_DATA_IN_PARQUET", true);

		_crudService = ServiceFactory.getService(CrudService.class);

		// Squelch some loud log messages in test output.

		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		config.getLoggerConfig("mantis.platform.db.SailPointContextProvider").setLevel(Level.ERROR);
		ctx.updateLoggers();  // This causes all Loggers to refetch information from their LoggerConfig.
		_log.info("Applied customer Hibernate logging.");

	}

	@AfterClass
	public static void cleanupS3Artifiacts() {
		S3PersistenceManager.deleteAuditEventsForTenant(_tenantId);
	}

	/**
	 * Poll the SyncJobStatistics in a busy-waiting poller until a timeout or given sum of outputs arrives.
	 * @param testName
	 * @param desiredCount
	 * @param maxMsecDuration
	 * @param stats
	 */
	private void waitForStatsCount(String testName, long desiredCount, long maxMsecDuration, SyncJobStatistics stats) {

		long startTime = System.currentTimeMillis();
		long duration = 0;
		long statsResult = 0;
		boolean desiredCountMet = false;
		boolean maxDurationMet = false;
		do {
			try {
				Thread.sleep(250);
				duration = System.currentTimeMillis() - startTime;
				// _log.info("Polling after duration:" + duration);
				desiredCountMet = desiredCount <= (statsResult = stats.getSumResultsCount());
				maxDurationMet = duration > maxMsecDuration;
			} catch (Exception ex) { /* swallow errors */ }
		} while ( !desiredCountMet && !maxDurationMet );

		_log.info("Poll Ended, desiredCount:" + desiredCount + " statsResult:" + statsResult +" duration:" + duration);

		if (duration > maxMsecDuration) {
			Assert.fail("Timed out waiting for: " + testName + " SyncJobStatistics:" + JsonUtils.toJson(stats));
		}

	}

	@Test
	public void testSyncCisToS3Worker() {

		_log.info("Testing SyncCisToS3Worker class logic.");

		SailPointContextProviderCache spcpc = ((MantisApplication)_application).getContextProviderCache();
		_log.info("spcpc: " + spcpc);

		RequestContext reqCtxt = TestUtils.setDummyRequestContext(_tenantId, "acme-solar", "dev");

		// Test case: Try to sync an AuditEvent that doesn't exist.
		String testName = "Sync an AuditEvent that doesn't exist";
		String auditEventNotExists = UUID.randomUUID().toString().replaceAll("-", "");
		SyncJobStatistics notExistsStats = new SyncJobStatistics();

		SyncJobTimeRange notExistsTimeRange  = new SyncJobTimeRange();
		notExistsTimeRange.setStartTime(Instant.now());
		notExistsTimeRange.setUpToTime(Instant.now());
		notExistsTimeRange.setExpectedRecordCount(1L);

		// Submit the request to the worker thread and wait for its completion.
		SyncCisToS3Worker.syncAuditEventsCis2S3(spcpc, reqCtxt.getOrgData(), notExistsStats, notExistsTimeRange);
		waitForStatsCount(testName, 1, 30000, notExistsStats);
		assertEquals(1L, notExistsStats.getSubmittedToQueue().get());
		assertEquals(1L, notExistsStats.getAdoptedByWorkerThread().get());
		assertEquals("Expect an non-existent peg counter, got:" + JsonUtils.toJson(notExistsStats),
				1L, notExistsStats.getMissingFromSource().get());
		assertEquals(0L, notExistsStats.getAlreadyExistedInTarget().get());
		assertEquals(0L, notExistsStats.getNewlyCreatedInTarget().get());
		_log.info("PASS: Non-existent AuditEvent correctly accounted.");

		// Test case: Try to sync exactly one single AuditEvent.
		ensureAuditArchiveTableExists(true);
		purgeAuditEvents();
		ensureAuditArchiveTableExists(false);
		testName = "Sync exactly one single AuditEvent.";
		AuditEvent auditEventSingle = getStubAuditEvent(null);
		enrichAuditEvent(auditEventSingle);
		saveAuditEventH2Safe(Collections.singletonList(auditEventSingle));
		SyncJobStatistics singleStats = new SyncJobStatistics();

		SyncJobTimeRange singleEventTimeRange  = new SyncJobTimeRange();
		singleEventTimeRange.setStartTime(auditEventSingle.getCreated().toInstant());
		singleEventTimeRange.setUpToTime(Instant.now());
		singleEventTimeRange.setExpectedRecordCount(1L);

		SyncCisToS3Worker.syncAuditEventsCis2S3(spcpc, reqCtxt.getOrgData(), singleStats, singleEventTimeRange);
		waitForStatsCount(testName, 1, 10000, singleStats);

		assertEquals(1L, singleStats.getSubmittedToQueue().get());
		assertEquals(1L, singleStats.getAdoptedByWorkerThread().get());
		assertEquals(0L, singleStats.getMissingFromSource().get());
		assertEquals(0L, singleStats.getAlreadyExistedInTarget().get());
		assertEquals("Expect a single newly created AuditEvent, got:" + JsonUtils.toJson(singleStats),
				1L, singleStats.getNewlyCreatedInTarget().get());
		_log.info("PASS: Single AuditEvent sync correctly accounted.");

		// Test case: Try to sync exactly one Archived AuditEvent that we have already sync-ed.
		testName = "Sync exactly one Archived AuditEvent that we have already sync-ed.";
		ensureAuditArchiveTableExists(true);
		moveAuditsIntoArchiveTable();
		SyncJobStatistics arhcSyncStats = new SyncJobStatistics();

		SyncJobTimeRange singleArchivedTimeRange  = new SyncJobTimeRange();
		singleArchivedTimeRange.setStartTime(auditEventSingle.getCreated().toInstant());
		singleArchivedTimeRange.setUpToTime(Instant.now());
		singleArchivedTimeRange.setExpectedRecordCount(1L);

		SyncCisToS3Worker.syncAuditEventsCis2S3(spcpc, reqCtxt.getOrgData(), arhcSyncStats, singleArchivedTimeRange);
		waitForStatsCount(testName, 1, 10000, arhcSyncStats);
		assertEquals(1L, arhcSyncStats.getSubmittedToQueue().get());
		assertEquals(1L, arhcSyncStats.getAdoptedByWorkerThread().get());
		assertEquals(0L, arhcSyncStats.getMissingFromSource().get());
		assertEquals("Expect archive already created created, got:" + JsonUtils.toJson(arhcSyncStats),
				1L, arhcSyncStats.getAlreadyExistedInTarget().get());
		assertEquals(0L, arhcSyncStats.getNewlyCreatedInTarget().get());
		_log.info("PASS: Single archived AuditEvent already sync-ed correctly accounted.");


		// Test case: Try to sync exactly one both archived and non archived AuditEvents.
		testName = "Sync exactly one single AuditEvent and one Archived AuditEvent.";
		purgeAuditEvents();

		AuditEvent ae1 = getStubAuditEvent(null);
		enrichAuditEvent(ae1);
		saveAuditEventH2Safe(Collections.singletonList(ae1));

		moveAuditsIntoArchiveTable();

		AuditEvent ae2 = getStubAuditEvent(null);
		enrichAuditEvent(ae2);
		saveAuditEventH2Safe(Collections.singletonList(ae2));

		SyncJobStatistics oneAndOneStats = new SyncJobStatistics();

		SyncJobTimeRange oneAndOneTimeRange  = new SyncJobTimeRange();
		oneAndOneTimeRange.setStartTime(Instant.ofEpochMilli(
				Math.min(ae1.getCreated().getTime(), ae2.getCreated().getTime()))
		);
		oneAndOneTimeRange.setUpToTime(Instant.now());
		oneAndOneTimeRange.setExpectedRecordCount(2L);

		SyncCisToS3Worker.syncAuditEventsCis2S3(spcpc, reqCtxt.getOrgData(), oneAndOneStats, oneAndOneTimeRange);

		waitForStatsCount(testName, 2, 10000, oneAndOneStats);

		assertEquals(2L, oneAndOneStats.getSubmittedToQueue().get());
		assertEquals(2L, oneAndOneStats.getAdoptedByWorkerThread().get());
		assertEquals(0L, oneAndOneStats.getMissingFromSource().get());
		assertEquals(0L, oneAndOneStats.getAlreadyExistedInTarget().get());
		assertEquals("Expect a single newly created AuditEvent, got:" + JsonUtils.toJson(oneAndOneStats),
				2L, oneAndOneStats.getNewlyCreatedInTarget().get());
		_log.info("PASS: " + testName);

		// Test case: Try to sync a large random grouping of sync'd and non-sync'ed events, concurrently.
		purgeAuditEvents();
		long numToTest = 4096;
		testName = "Multi-Threaded Concurrent Production and Sync";

		SyncJobStatistics multiThreadStats = new SyncJobStatistics();

		SyncJobTimeRange multiThreadTimeRange  = new SyncJobTimeRange();
		multiThreadTimeRange.setStartTime(
			LocalDateTime.of(2014, 1, 1, 0,0,0,0)
					.atZone(UTC_ZONE).toInstant()
		);
		multiThreadTimeRange.setUpToTime(Instant.now());
		multiThreadTimeRange.setExpectedRecordCount(numToTest);

		ExecutorService execSvc = Executors.newFixedThreadPool(32,
				new ThreadFactoryBuilder()
						.setNameFormat("cis2s3-stuffer-%d")
						.setDaemon(true) // Allow process exit when long-running S3 purge job is active.
						.build()
		);

		// Concurrently add an AuditEvent to the DB (H2) and then request it get sync'ed to S3.
		for (long i=0; i<numToTest; i++) {
			execSvc.submit(() -> {
				AuditEvent ae = enrichAuditEvent(getStubAuditEvent(null));
				saveAuditEventH2Safe(Collections.singletonList(ae));
			});
		}
		execSvc.shutdown();
		try {
			execSvc.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		SyncCisToS3Worker.syncAuditEventsCis2S3(spcpc, reqCtxt.getOrgData(), multiThreadStats, multiThreadTimeRange);

		waitForStatsCount(testName, numToTest, 120000, multiThreadStats);

		assertEquals(numToTest, multiThreadStats.getSubmittedToQueue().get());
		assertEquals(numToTest, multiThreadStats.getAdoptedByWorkerThread().get());
		assertEquals("Expected zero missing from source, got:" + multiThreadStats.getMissingFromSource().get(),
				0L, multiThreadStats.getMissingFromSource().get());
		assertEquals("Expected zero already existed, got:" + multiThreadStats.getAlreadyExistedInTarget().get(),
				0L, multiThreadStats.getAlreadyExistedInTarget().get());
		assertEquals("Expect all newly created AuditEvents, got:" + JsonUtils.toJson(multiThreadStats),
				numToTest, multiThreadStats.getNewlyCreatedInTarget().get());
		_log.info("PASS: " + testName);

		/* - This test case no longer applies:

		// Test the List<AuditEventId> interface to store collections of audit events.
		testName = "Test the List<AuditEventId> interface";
		SyncJobStatistics listTestStats = new SyncJobStatistics();
		ConcurrentArrayList<String> auditEventIdList = new ConcurrentArrayList<>();

		// Concurrently generate a bunch of Audit Events and save them in H2.
		long listSizeToTest = 128;
		ExecutorService listTestExecSvc = Executors.newFixedThreadPool(32,
				new ThreadFactoryBuilder()
						.setNameFormat("cis2s3-stuffer-%d")
						.setDaemon(true) // Allow process exit when long-running S3 purge job is active.
						.build()
		);
		for (long i=0; i<listSizeToTest; i++) {
			listTestExecSvc.execute(() -> {
				AuditEvent ae = enrichAuditEvent(getStubAuditEvent(null));
				saveAuditEventH2Safe(Collections.singletonList(ae));
				auditEventIdList.add(ae.getId());
			});
		}
		listTestExecSvc.shutdown();
		try {
			listTestExecSvc.awaitTermination(120, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			_log.error("Failure waiting for H2 AuditEvent population.");
		}

		// Convert to a regular flat List instead of the concurrent kind.
		ArrayList<String> listToPass = new ArrayList<>();
		auditEventIdList.iterator().forEachRemaining(auditEventId -> {
			listToPass.add(auditEventId);
		});
		SyncCisToS3Worker.syncAuditEventsCis2S3(spcpc, reqCtxt.getOrgData(), listTestStats, listToPass);
		waitForStatsCount(testName, listSizeToTest, 120000, listTestStats);
		_log.info("PASS: " + testName);
		 */

	}

	/**
	 * Safely saves an AuditEvent in H2 or MySQL.  Primarily used for integration and sync testing.
	 *
	 * When interacting with H2 for some reason the CIS/Mantis-Platform code base always issues an UPDATE
	 * when calling context.save(auditEvent) or crudService.save(auditEvent).  This results in a failure
	 * for net-new audit events that don't already exist in the database.  We're migrating _away_ from CIS
	 * MySQL persistence of AuditEvents, and there is little reason to invest in making this work cleanly in
	 * H2, so this test driver uses this adapter method that saves a stub via direct SQL before saving the
	 * full event in H2.
	 *
	 * @param auditEvents - List of events to persist to CIS context.
	 */
	public void saveAuditEventH2Safe(List<AuditEvent> auditEvents) {

		this.runWithContext(context -> {

			Log log = LogFactory.getLog(S3PersistenceIntegrationTests.class);

			final String sql = "INSERT INTO spt_audit_event (id, created) VALUES (?, ?);";

			for (AuditEvent auditEvent : auditEvents) {

				if (null == auditEvent.getId()) {
					auditEvent.setId(UUID.randomUUID().toString().replaceAll("-", ""));
				}

				if (null == auditEvent.getCreated()) {
					auditEvent.setCreated(new Date());
				}

				try {

					Connection dbCxn = context.getJdbcConnection();
					try (
							PreparedStatement ps = dbCxn.prepareStatement(sql);
					) {
						ps.setString(1, auditEvent.getId());
						ps.setLong(2, auditEvent.getCreated().getTime());
						int rowCount = ps.executeUpdate();
						dbCxn.commit();
						if (1 != rowCount) {
							log.warn("Expected rowCount:0, got:" + rowCount + " saving:" + JsonUtils.toJson(auditEvent));
						}
					}

					context.saveObject(auditEvent);

				} catch (GeneralException e) {
					log.error("Failure transacting with SailPointContext", e);
				} catch (SQLException throwables) {
					log.error("Failure transacting with SailPointContext", throwables);
				}
			}
			return null; // from runWithContext lambda.
		});
	}

	public void ensureAuditArchiveTableExists(boolean shouldBePresent) {

		final String dropSql = "DROP TABLE IF EXISTS spt_audit_event_archive;";
		final String createSql = "CREATE MEMORY TABLE IF NOT EXISTS PUBLIC.spt_audit_event_archive ( ID VARCHAR_IGNORECASE(128) NOT NULL PRIMARY KEY, CREATED BIGINT, MODIFIED BIGINT, OWNER VARCHAR_IGNORECASE(128), ASSIGNED_SCOPE VARCHAR_IGNORECASE(128), ASSIGNED_SCOPE_PATH VARCHAR_IGNORECASE(450), INTERFACE VARCHAR_IGNORECASE(128), SOURCE VARCHAR_IGNORECASE(128), ACTION VARCHAR_IGNORECASE(128), TARGET VARCHAR_IGNORECASE(255), APPLICATION VARCHAR_IGNORECASE(128), ACCOUNT_NAME VARCHAR_IGNORECASE(256), INSTANCE VARCHAR_IGNORECASE(128), ATTRIBUTE_NAME VARCHAR_IGNORECASE(128),ATTRIBUTE_VALUE VARCHAR_IGNORECASE(450), TRACKING_ID VARCHAR_IGNORECASE(128), ATTRIBUTES LONGTEXT, STRING1 VARCHAR_IGNORECASE(255), STRING2 VARCHAR_IGNORECASE(255), STRING3 VARCHAR_IGNORECASE(255), STRING4 VARCHAR_IGNORECASE(255) );";

		this.runWithContext(context -> {
			try {
				Connection jdbcCxn = context.getJdbcConnection();
				try ( Statement stmt = jdbcCxn.createStatement() ) {

					if (shouldBePresent) {
						stmt.executeUpdate(createSql);
					} else {
						stmt.executeUpdate(dropSql);
					}
				} catch (SQLException throwables) {
					_log.error("Failure transacting with DB", throwables);
				}
			} catch (GeneralException e) {
				_log.error("Failure transacting with DB", e);
			}
			return null;
		});
	}

	// Moves all the records currently in the audit event table into the archive table.
	public void moveAuditsIntoArchiveTable() {

		final String sql1 = "INSERT INTO spt_audit_event_archive SELECT * FROM spt_audit_event;";
		final String sql2 = "DELETE FROM spt_audit_event WHERE 1=1;";

		this.runWithContext(context -> {
			try {
				Connection jdbcCxn = context.getJdbcConnection();
				try ( Statement stmt = jdbcCxn.createStatement() ) {
					int movedCount = stmt.executeUpdate(sql1);
					int purgeCount = stmt.executeUpdate(sql2);
					_log.info("Moved " + movedCount + " into spt_audit_event_archive; purged: " + purgeCount);
				} catch (SQLException throwables) {
					_log.error("Failure transacting with DB", throwables);
				}
			} catch (GeneralException e) {
				_log.error("Failure transacting with DB", e);
			}
			return null;
		});
	}

	/**
	 * Purges all the AuditEvent records from spt_audit_event and spt_audit_event_archive.
	 */
	public void purgeAuditEvents() {
		this.runWithContext(context -> {
			try {
				Connection jdbcCxn = context.getJdbcConnection();
				try ( Statement stmt = jdbcCxn.createStatement() ) {
					int delCount = stmt.executeUpdate("DELETE FROM spt_audit_event WHERE 1=1;");
					_log.info("Deleted " + delCount + " previous AuditEvents");
					int delArcCount = stmt.executeUpdate("DELETE FROM spt_audit_event_archive WHERE 1=1;");
					_log.info("Deleted " + delArcCount + " previous AuditEvents from spt_audit_event_archive");
				} catch (SQLException throwables) {
					_log.error("Failure transacting with DB", throwables);
				}
			} catch (GeneralException e) {
				_log.error("Failure transacting with DB", e);
			}
			return null;
		});
	}

	// Get a random date any time in the last 7 years.
	public Date getRandomDate() {
		Date nowDate = new Date();
		long startMillis = nowDate.getTime() - (7 * 31_536_000_000l); // Now minus 5 years of milliseconds.
		long endMillis = nowDate.getTime();
		long randomMillisSinceEpoch = ThreadLocalRandom.current().nextLong(startMillis, endMillis);
		return new Date(randomMillisSinceEpoch);
	}

	// Get a stub of an AuditEvent for testing. If no date provided, pick a random one in the last 7 years.
	public AuditEvent getStubAuditEvent(Date createdDate) {
		if (null == createdDate) createdDate = getRandomDate();
		AuditEvent auditEvent = new AuditEvent();
		auditEvent.setCreated(createdDate);
		auditEvent.setId(UUID.randomUUID().toString().replaceAll("-", ""));
		return auditEvent;
	}

	private AuditEvent enrichAuditEvent(AuditEvent auditEvent) {
		auditEvent.setAccountName("jane.doe");
		auditEvent.setAction(AuditEventActions.ACTION_CREATE_ACCOUNT);
		auditEvent.setTarget("jane.doe");
		auditEvent.setSource("identityRefresh");
		auditEvent.setInstance("SSO");
		auditEvent.setApplication("Active Directory");
		auditEvent.setString1("localhost");
		auditEvent.setString2("127.0.0.1");
		auditEvent.setString3("7c5ac42af980344b01");
		auditEvent.setString4("NONE");
		// note: The ordering of keys in this map makes JSON diff'ing interesting.
		auditEvent.setAttribute("attrib1Key", "attrib1Val");
		auditEvent.setAttribute("attrib2Key", "attrib2Val");
		auditEvent.setInterface("Is this ever used!?");
		// note: The Description field of audit events is not persisted in H2?
		// auditEvent.setDescription("A unit or integration test AuditEvent");
		// Talking to the team, this column appears to be un-used and we can ignore.
		return auditEvent;
	}

	@Test
	public void syncJobTests() {

		// Generate a bunch of Audit Events and save them in H2.  Have at least one record in the archive table.
		ensureAuditArchiveTableExists(true);
		{
			AuditEvent ae = enrichAuditEvent(getStubAuditEvent(new Date()));
			saveAuditEventH2Safe(Collections.singletonList(ae));
			moveAuditsIntoArchiveTable();
		}

		long listSizeToTest = 128;
		for (long i=0; i<listSizeToTest; i++) {
			AuditEvent ae = enrichAuditEvent(getStubAuditEvent(null));
			saveAuditEventH2Safe(Collections.singletonList(ae));
		}

		SailPointContextProviderCache spcpCache = ((MantisApplication)_application).getContextProviderCache();

		SyncCisToS3Job cis2S3Svc = new SyncCisToS3Job();
		cis2S3Svc.syncOrgCisToS3(RequestContext.ensureGet().getOrgData(), spcpCache, null);

	}

	@Test
	public void cisQueriesTests() {

		ensureAuditArchiveTableExists(false);

		runWithContext((context) -> {

			SyncCisToS3Queries cisQueries = new SyncCisToS3Queries(context);

			Instant nowInstant = Instant.now();
			Instant earlierInstant = nowInstant.minus(1, ChronoUnit.HOURS);

			Date nowDate = new Date(nowInstant.toEpochMilli());
			Date earlierDate = new Date(earlierInstant.toEpochMilli());

			long invertedCount = cisQueries.getAuditEventCountsByDateRange(nowDate, earlierDate);
			assertTrue("Should get zero, got: " + invertedCount, 0 == invertedCount);

			AtomicInteger counter = new AtomicInteger(0);
			cisQueries.getAuditEventIdCreatedByDateRange(nowDate, earlierDate, (id, createdDate) -> {
				counter.incrementAndGet();
			});
			assertTrue("Should get zero, got: " + counter.get(), 0 == counter.get());

			// Non Existent Archive Table Test, set above, ensures we hit a return null case.
			String auditEventId = "does-not-exist";
			AuditEvent auditEvent = cisQueries.getAuditEvent(auditEventId);
			assertNull("Should get back null, got: "+ auditEvent, auditEvent);

			return null;
		});

	}

	@Test
	public void syncCisToS3ServiceTests() {

		// Sync this org's data!
		_syncCisToS3Service.enqueueOrgForSyncing(RequestContext.ensureGet().getOrgData());
		_syncCisToS3Service.execSvc.shutdown();
		try {
			_syncCisToS3Service.execSvc.awaitTermination(3, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			fail("Failed while waiting for executor service to finish: " + e.getMessage());
		}

	}

}
