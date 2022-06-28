/*
 * Copyright (c) 2021.  SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.integration;

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
import com.sailpoint.audit.util.AuditEventComparator;
import com.sailpoint.audit.utils.TestUtils;
import com.sailpoint.featureflag.impl.MockFeatureFlagClient;
import com.sailpoint.mantis.core.service.CrudService;
import com.sailpoint.mantis.core.service.model.AuditEventActions;
import com.sailpoint.mantis.event.MantisEventHandlerModule;
import com.sailpoint.mantis.platform.MantisApplication;
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
import org.junit.Before;
import org.junit.Test;
import sailpoint.api.SailPointContext;
import sailpoint.object.AuditEvent;
import sailpoint.object.QueryOptions;
import sailpoint.tools.GeneralException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test cases exercising the queries to retrieve counts of AuditEvent records from both the
 * spt_audit_event and spt_audit_even_arhcive database tables.  Two methods are primarily getting
 * exercised here:
 *
 *   1) bulkUploadAuditEventsService.getAuditEventCountsByDateRange - gets counts for date range
 *   2) bulkUploadAuditEventsService.getAuditEventIdCreatedByDateRange - gets IDs and CreatedDates for date range.
 *
 * The test cases generate random-ish audit events going back several days.  These are persisted in the H2 in-memory
 * database (which is pretty fast).  Then both functions are exercised issuing queries day-by-day to retrieve the data.
 * The test cases also exercise SUM-ing and UNION-ing the spt_audit_event and spt_audit_even_arhcive database tables
 * so we have complete coverage of everything that exists in the CIS database.  This will enable full bulk export
 * to S3 in a future PR.
 *
 */
public class S3PersistenceIntegrationTests extends MantisIntegrationTest {

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




	public void purgeAuditEvents() {
		this.runWithContext(context -> {
			try {
				Connection jdbcCxn = context.getJdbcConnection();
				try ( Statement stmt = jdbcCxn.createStatement() ) {
					int delCount = stmt.executeUpdate("DELETE FROM spt_audit_event WHERE 1=1;");
					_log.info("Deleted " + delCount + " previous AuditEvents");
				} catch (SQLException throwables) {
					_log.error("Failure transacting with DB", throwables);
				}
			} catch (GeneralException e) {
				_log.error("Failure transacting with DB", e);
			}
			return null;
		});
	}

	// Not to be confused with: loadTestData() - which is inherited from the superclass.
	public int loadAuditTestData() {

		_log.info("Loading AuditEvent Test Data...");

		ArrayList<AuditEvent> aeList = new ArrayList<>();

		Date nowDate = new Date();

		// Populate a couple of year's worth of Audit Event data. Since H2 is an in-memory DB, this should be quick.
		int savedCount = 0;
		Date earliestDate = null;

		long numDaysBack = NUM_DAYS_BACK;
		long recsPerDay = 3;
		long msecsPerDay = MSECS_PER_DAY;

		long startTime = System.currentTimeMillis();
		for (long i=2; i<numDaysBack; i++) {

			for (long j=0; j<recsPerDay; j++) {

				long daysToSubtract = i * msecsPerDay;
				long timeStamp = nowDate.getTime() - daysToSubtract + random.nextInt((int)msecsPerDay);

				Date auditEventDate = new Date(timeStamp);
				// _log.info("i:" + i + " auditEventDate:" + auditEventDate.toString());

				if (null == earliestDate) {
					earliestDate = auditEventDate;
				} else if (auditEventDate.before(earliestDate)) {
					earliestDate = auditEventDate;
				}

				AuditEvent auditEvent = getStubAuditEvent(auditEventDate);
				enrichAuditEvent(auditEvent);

				aeList.add(auditEvent);
			}

			saveAuditEventH2Safe(aeList);
			savedCount+= aeList.size();
			aeList.clear();
		}
		long duration = System.currentTimeMillis() - startTime;
		_log.info("Persisted " + savedCount + " AuditEvents in " + duration + " msecs, going back to: " + earliestDate);

		return savedCount;

		/*
		// Useful for examining AuditEvents in time sequence order.
		OrgData orgData = _application.getOrgDataProvider().find("acme-solar").get();
		_jdbcConnection = new JdbcConnection(MantisOrgData.getConnectionInfo(orgData));
		_jdbcConnection.execute(statement ->{
			String sql = "SELECT * FROM spt_audit_event ORDER BY created;";
			try ( ResultSet rs = statement.executeQuery(sql) ) {
				while (rs.next()) {
					int idx = 1;
					_log.info("id: "       + rs.getString(idx++));
					_log.info("created: "  + rs.getLong(idx++));
					_log.info("modified: " + rs.getLong(idx++));
					_log.info("owner: "    + rs.getString(idx++));
				}
			}
		});
		*/

	}

	/**
	 * Return a simple count * from the spt_audit_event table (not the archive table!).
	 * @return
	 */
	public int countAllAuditEvents() {
		return this.runWithContext(context -> {
			Log log = LogFactory.getLog(S3PersistenceIntegrationTests.class);
			log.info("context: " + context.toString());
			try {
				int count = context.countObjects(AuditEvent.class, new QueryOptions());
				log.info("count of AuditEvents:" + count);
				return count;
			} catch (GeneralException e) {
				log.error("Failure counting objects", e);
			}
			return 0;
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

	private void enrichAuditEvent(AuditEvent auditEvent) {
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
	}

	/**
	* Count the number of audit events for day N days before and leading up to the baseDate.
	* @return
	*/
	public int getDaysBackCount(Date baseDate, long daysBeforeBase, SailPointContext context) {
		int totalCount = 0;
		Instant today = baseDate.toInstant().truncatedTo(ChronoUnit.DAYS);
		for (long daysBack = 0; daysBack <= daysBeforeBase; daysBack++) {
			Instant startOfDay = today.minus(daysBack, ChronoUnit.DAYS);
			Instant endOfDay = startOfDay.plus((MSECS_PER_DAY - 1), ChronoUnit.MILLIS);
			int dayCount = _bulkUploadAuditEventsService.getAuditEventCountsByDateRange(
					Date.from(startOfDay), Date.from(endOfDay)
			);
			_log.info("From [" + startOfDay.toString() + "] to [" + endOfDay.toString() + "] count:" + dayCount);
			totalCount += dayCount;
		}
		return totalCount;
	}

	@Test
	public void testDbQueryAuditEventsByTimeRange() {

		_log.info("Hello World - testQueryAuditEventsByTimeRange");

		purgeAuditEvents();
		final AtomicInteger nonArchiveAuditCount = new AtomicInteger(loadAuditTestData());

		Date nowDate = new Date();

		// Count day by day, looking back in time to make sure everything we persisted is there.
		this.runWithContext(context -> {
			int totalCount = 0;
			for (long daysBack = 0; daysBack <= NUM_DAYS_BACK; daysBack++) {

				Date fromDate = new Date(nowDate.getTime() - (MSECS_PER_DAY * daysBack));
				Date upToDate = new Date(fromDate.getTime() + (MSECS_PER_DAY - 1));

				int count = _bulkUploadAuditEventsService.getAuditEventCountsByDateRange(fromDate, upToDate);

				String fromStr = fromDate.toInstant().toString();
				String upToStr = upToDate.toInstant().toString();
				_log.info("From [" + fromStr + "] to [" + upToStr + "] count:" + count);

				totalCount += count;
			}
			_log.info("totalCount:" + totalCount);
			assertEquals(nonArchiveAuditCount.get(), totalCount);
			return null;
		});

		// Try counting with a different approach, using more modern Instant. Easier for ZonedDateTime to UTC/GMT.
		this.runWithContext(context -> {
			int totalCount = getDaysBackCount(nowDate, NUM_DAYS_BACK, context);
			_log.info("totalCount:" + totalCount);
			assertEquals(nonArchiveAuditCount.get(), totalCount);
			return null;
		});

		// Test counting via time ranges with only the Archive table populated.
		ensureAuditArchiveTableExists(true);
		moveAuditsIntoArchiveTable();

		this.runWithContext(context -> {
			int totalCount = getDaysBackCount(nowDate, NUM_DAYS_BACK, context);
			_log.info("totalCount:" + totalCount);
			assertEquals(nonArchiveAuditCount.get(), totalCount);
			return null;
		});

		// Re-Populate the audit event table with the archive table already populated; count should sum both.
		int arhchiveAuditCount = nonArchiveAuditCount.get();
		nonArchiveAuditCount.set(loadAuditTestData());

		this.runWithContext(context -> {
			int totalCount = getDaysBackCount(nowDate, NUM_DAYS_BACK, context);
			_log.info("totalCount:" + totalCount);
			assertEquals((arhchiveAuditCount + nonArchiveAuditCount.get()), totalCount);
			return null;
		});

		AuditEvent ae = getStubAuditEvent(nowDate);
		enrichAuditEvent(ae);
		ae.setModified(null);
		ae.setId(UUID.randomUUID().toString().replaceAll("-", ""));

		_log.info("Saving net-new audit event: " + ae.getId());
		saveAuditEventH2Safe(Collections.singletonList(ae));

		AuditEvent readBack = this.findObjectById(AuditEvent.class, ae.getId());
		assertNotNull("Should get an AuditEvent reference back", readBack);

		// Verify the round trip back from the database.  This is interesting because the ordering
		// of the Attributes maps is subject to change so we can not directly compare them in JSON.
		AuditEventComparator aeComp = new AuditEventComparator();
		String difference = aeComp.diffAuditEvents(ae, readBack);
		assertNull("Should get no difference, got: " + difference, difference);

	}

	/**
	 * Retrieve the IDs and CreatedDates audit events for day N days before and leading up to the baseDate.
	 *
	 * @param baseDate - the date to start looking back from.
	 * @param daysBeforeBase - how many days to look back before the base date.
	 * @param context - the SailPointContext to issue the audit event queries against.
	 * @param idsList - List of IDs of Audit Events in time rage, parallel with createdDatesList
	 * @param createdDatesList - List of createdDates of Audit Events in time rage, parallel with idsList
	 * @return - the Count of the number of audit events for the given time range.
	 */
	public int getDaysBackIdCreated(Date baseDate, long daysBeforeBase, SailPointContext context,
									ArrayList<String> idsList, ArrayList<Date> createdDatesList) {
		int totalCount = 0;
		Instant today = baseDate.toInstant().truncatedTo(ChronoUnit.DAYS);
		for (long daysBack = 0; daysBack <= daysBeforeBase; daysBack++) {
			Instant startOfDay = today.minus(daysBack, ChronoUnit.DAYS);
			Instant endOfDay = startOfDay.plus((MSECS_PER_DAY - 1), ChronoUnit.MILLIS);
			int dayCount = _bulkUploadAuditEventsService.getAuditEventIdCreatedByDateRange(
					Date.from(startOfDay), Date.from(endOfDay),(id, createdDate) -> {
						idsList.add(id);
						createdDatesList.add(createdDate);
					}
			);
			_log.info("From [" + startOfDay.toString() + "] to [" + endOfDay.toString() + "] count:" + dayCount);
			totalCount += dayCount;
		}
		return totalCount;
	}

	@Test
	public void testAuditEventIdRetrieval() {

		_log.info("Hello World - testAuditEventIdRetrieval");

		purgeAuditEvents();
		final AtomicInteger nonArchiveAuditCount = new AtomicInteger(loadAuditTestData());

		Date nowDate = new Date();

		// Retrieve day by day, looking back in time to make sure everything we persisted is there.
		this.runWithContext(context -> {

			int totalCount = 0;

			// Note parallel arrays populated by the consumer.
			final ArrayList<String> idList = new ArrayList<>();
			final ArrayList<Date> createdList = new ArrayList<>();

			for (long daysBack = 0; daysBack <= NUM_DAYS_BACK; daysBack++) {

				idList.clear();
				createdList.clear();

				Date fromDate = new Date(nowDate.getTime() - (MSECS_PER_DAY * daysBack));
				Date upToDate = new Date(fromDate.getTime() + (MSECS_PER_DAY - 1));

				int count = _bulkUploadAuditEventsService.getAuditEventIdCreatedByDateRange(
						fromDate, upToDate, (id, createdDate) -> {
							_log.info("id:" + id + " createdDate:" + createdDate.toInstant().toString());
							idList.add(id);
							createdList.add(createdDate);
						}
				);

				String fromStr = fromDate.toInstant().toString();
				String upToStr = upToDate.toInstant().toString();
				_log.info("From [" + fromStr + "] to [" + upToStr + "] count:" + count);

				assertEquals("Returned count should match idList size", count, idList.size());
				assertEquals("Returned count should match createdList size", count, createdList.size());

				totalCount += count;

			}
			_log.info("totalCount:" + totalCount);
			assertEquals(nonArchiveAuditCount.get(), totalCount);
			return null;
		});

		// Now run the test again with the audit archive table included.
		// Move the records over to the spt_audit_event_archive table and then Re-Populate the audit event
		// table with the archive table already populated; queries afterwards should union both.
		ensureAuditArchiveTableExists(true);
		moveAuditsIntoArchiveTable();
		int arhchiveAuditCount = nonArchiveAuditCount.get();
		nonArchiveAuditCount.set(loadAuditTestData());

		this.runWithContext(context -> {
			// Note parallel arrays populated by the consumer.
			final ArrayList<String> idList = new ArrayList<>();
			final ArrayList<Date> createdList = new ArrayList<>();
			int totalCount = getDaysBackIdCreated(nowDate, NUM_DAYS_BACK, context, idList, createdList);
			assertEquals("Returned count should match idList size", totalCount, idList.size());
			assertEquals("Returned count should match createdList size", totalCount, createdList.size());
			assertEquals((arhchiveAuditCount + nonArchiveAuditCount.get()), totalCount);
			return null;
		});

	}

}
