package com.sailpoint.audit.persistence;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import com.sailpoint.audit.service.S3ClientProvider;
import com.sailpoint.mantis.core.service.model.AuditEventActions;
import com.sailpoint.utilities.JsonUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import sailpoint.object.AuditEvent;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.sailpoint.audit.utils.TestUtils.setDummyRequestContext;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class S3PersitenceManagerTest {

	public static final Log log = LogFactory.getLog(S3PersitenceManagerTest.class);

	private static final String tenantId = UUID.randomUUID().toString();
	private static final String org = "acme-solar-auevt";
	private static final String pod = "dev";

	private static final ArrayList<String> tenantIdsToPurgeAfterTesting = new ArrayList<>();

	Date nowDate = new Date();

	/**
	 * The s3Client is usually injected by Google Guice.  For unit testing we need our own, so we side load one.
	 */
	@Before
	public void allocateTestingS3Client() {

		// Re-implementation of S3ClientProvider's logic.
		AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
				.withClientConfiguration(
						new ClientConfiguration()
								.withClientExecutionTimeout(S3ClientProvider.CLIENT_EXECUTION_TIMEOUT)
								.withMaxConnections(S3ClientProvider.MAX_CONCURRENT_S3_CONNECTIONS)
				).build();

		S3PersistenceManager.overrideS3Client(s3Client);
	}

	@Before
	public void setContextValues() {
		setDummyRequestContext(tenantId, org, pod);
	}

	@After
	public void cleanupTestKeyInS3() throws InterruptedException {

		// Give S3 a little bit of time to commit and replicate all of its transactions before issuing deletes.
		Thread.sleep(2000);

		S3PersistenceManager.deleteAuditEventsForTenant(tenantId);
		for (String toDelete : tenantIdsToPurgeAfterTesting) {
			S3PersistenceManager.deleteAuditEventsForTenant(toDelete);
		}
		tenantIdsToPurgeAfterTesting.clear();
	}

	// Get a random date any time in the last 7 years.
	public Date getRandomDate() {
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

	@Test
	public void objectKeyGenerationTest() {

		AuditEvent auditEvent = getStubAuditEvent(nowDate);

		// Check the 3 different approaches to getting the S3 Key for the Audit Event.
		String byAeOnly = S3PersistenceManager.getObjectKey(auditEvent);
		String byTidAid = S3PersistenceManager.getObjectKey(tenantId, auditEvent);
		String byTiDtId = S3PersistenceManager.getObjectKey(tenantId, auditEvent.getCreated(), auditEvent.getId());

		log.info("byAeOnly:" + byAeOnly);
		log.info("byTidAid:" + byTidAid);
		log.info("byTiDtId:" + byTiDtId);

		String pattern = "yyyy/MM/dd/HH/mm";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Etc/UTC")));

		String expected = tenantId + "/" + simpleDateFormat.format(nowDate) + "/" + auditEvent.getId();
		log.debug("expected:" + expected);

		assertTrue("byAeOnly should match expected, got: " + byAeOnly, expected.equals(byAeOnly));
		assertTrue("byTidAid should match expected, got: " + byAeOnly, expected.equals(byTidAid));
		assertTrue("byTiDtId should match expected, got: " + byAeOnly, expected.equals(byTiDtId));

	}

	@Test
	public void objectKeyRandomDateTest() {

		int numCycles = 100;

		String pattern = "yyyy/MM/dd/HH/mm";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Etc/UTC")));

		for (int i=0; i<numCycles; i++) {

			Date randomDate = getRandomDate();
			AuditEvent auditEvent = getStubAuditEvent(randomDate);

			// Check the 3 different approaches to getting the S3 Key for the Audit Event.
			String byAeOnly = S3PersistenceManager.getObjectKey(auditEvent);
			String byTidAid = S3PersistenceManager.getObjectKey(tenantId, auditEvent);
			String byTiDtId = S3PersistenceManager.getObjectKey(tenantId, auditEvent.getCreated(), auditEvent.getId());
			String expected = tenantId + "/" + simpleDateFormat.format(randomDate) + "/" + auditEvent.getId();

			log.debug("expected:" + expected);

			assertTrue("byAeOnly should match expected, got: " + byAeOnly, expected.equals(byAeOnly));
			assertTrue("byTidAid should match expected, got: " + byAeOnly, expected.equals(byTidAid));
			assertTrue("byTiDtId should match expected, got: " + byAeOnly, expected.equals(byTiDtId));
		}
	}

	@Test
	public void getNonExistentRecordTest() {
		S3AuditEventEnvelope envelope = S3PersistenceManager.getAuditEvent(
				"invalid-tenant", nowDate, "invalid-id-" + UUID.randomUUID()
		);
		assertNull("Invalid tenant record envelope should come back null.", envelope);
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
		auditEvent.setAttribute("attrib1Key", "attrib1Val");
		auditEvent.setAttribute("attrib2Key", "attrib2Val");
		auditEvent.setDescription("A unit or integration test AuditEvent");
		auditEvent.setInterface("Is this ever used!?");
	}

	@Test
	public void singleAuditEventPersistenceTest() {

		AuditEvent auditEvent = getStubAuditEvent(nowDate);
		enrichAuditEvent(auditEvent);

		String aeJson = JsonUtil.toJson(auditEvent);
		log.info("aeJson: " + aeJson);

		S3AuditEventEnvelope envelope = S3PersistenceManager.saveAuditEvent(auditEvent);
		assertNotNull("We should get a valid persistence envelope back.", envelope);

		try {
			String s3Sha = envelope.getSha256Hash();
			String mySha = S3PersistenceManager.getSha256Hash(auditEvent);
			assertTrue(
					"We should have equal hashes, envHash:" + s3Sha + " calculated:" + mySha,
					s3Sha.equalsIgnoreCase(mySha)
			);
			assertTrue(
					"We should have equal returned strings",
					aeJson.equals(envelope.auditEventJson)
			);
		} catch (Exception e) {
			log.error("Failure comparing hashes", e);
			fail("Failure comparing hashes: " + e.getMessage());
		}

		S3PersistenceManager.deleteAuditEvent(tenantId, auditEvent.getCreated(), auditEvent.getId());

		S3AuditEventEnvelope readBack = S3PersistenceManager.getAuditEvent(
				tenantId, auditEvent.getCreated(), auditEvent.getId()
		);
		assertNull("Expected nothing back, got:" + JsonUtil.toJson(readBack), readBack);

	}

	@Test
	public void repeatedSavingTest() {

		AuditEvent auditEvent = getStubAuditEvent(nowDate);
		enrichAuditEvent(auditEvent);

		// Save an audit event multiple times and ensure it's version is always one.
		S3AuditEventEnvelope env1 = S3PersistenceManager.saveAuditEvent(auditEvent);
		S3AuditEventEnvelope env2 = S3PersistenceManager.saveAuditEvent(auditEvent);
		S3AuditEventEnvelope env3 = S3PersistenceManager.saveAuditEvent(auditEvent);

		assertTrue("SHA256 should match.", env1.getSha256Hash().equals(env2.getSha256Hash()));
		assertTrue("SHA256 should match.", env1.getSha256Hash().equals(env3.getSha256Hash()));

	}

	@Test
	public void iterationTest() {

		int numCycles = 3;
		for (int i=0; i<numCycles; i++) {
			AuditEvent auditEvent = getStubAuditEvent(nowDate);
			enrichAuditEvent(auditEvent);
			S3PersistenceManager.saveAuditEvent(auditEvent);
		}

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(nowDate);
		calendar.add(Calendar.HOUR_OF_DAY, 2);
		Date upToDate = calendar.getTime();

		AtomicInteger counter = new AtomicInteger(0);
		S3PersistenceManager.iterateByDateRange(tenantId, nowDate, upToDate, (objKey) -> {
			log.info("iterated: " + objKey);
			counter.incrementAndGet();
		});
		assertTrue("Should be >= " + numCycles + " got:" + counter.get(), numCycles <= counter.get());

	}

	@Test
	public void concurrentSaveCompareTest() {

		int numCycles = 512;
		AtomicInteger eventCount = new AtomicInteger(0);

		ExecutorService es = Executors.newFixedThreadPool(8);

		for (int i=0; i<numCycles; i++) {
			es.execute(() -> {
				setContextValues(); // We need tenantId, etc. in context.
				AuditEvent auditEvent = getStubAuditEvent(null);
				enrichAuditEvent(auditEvent);
				log.info(Thread.currentThread().getName() + " key:" + S3PersistenceManager.getObjectKey(auditEvent));
				S3AuditEventEnvelope env1 = S3PersistenceManager.saveAuditEvent(auditEvent);
				S3AuditEventEnvelope env2 = S3PersistenceManager.getAuditEvent(
						tenantId, auditEvent.getCreated(), auditEvent.getId()
				);
				assertTrue("SHA256 should match.", env1.getSha256Hash().equals(env2.getSha256Hash()));
				eventCount.incrementAndGet();
			});
		}

		try {
			es.shutdown();
			boolean atResult = es.awaitTermination(300, TimeUnit.SECONDS);
			assertTrue("Executor needs to terminate and not time out, got:" + atResult, atResult);
		} catch (InterruptedException e) {
			log.error("Interrupted while awaiting worker thread completion. ", e);
			fail("Interrupted while awaiting worker thread completion. " + e.getMessage());
		} finally {
			if (!es.isTerminated()) {
				log.error("cancel non-finished tasks");
			}
			es.shutdownNow();
			log.info("Executor shutdown finished.");
		}

		assertTrue("Got " + eventCount.get() + " of expected " + numCycles + " persisted events.",
				eventCount.get() == numCycles);

		log.info("Completed concurrent persistence of " + eventCount.get() + " records.");

	}

	@Test
	public void testDateFormatTest() {

		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(S3PersistenceManager.UTC_ZONE));

		// Test a smattering of randomly generated dates.
		int numCycles = 37;
		for (int i=0; i<numCycles; i++) {

			Date randomDate = getRandomDate();
			cal.setTime(randomDate);

			String datePath = S3PersistenceManager.getKeyPathDateParts(randomDate);

			// This output looks funny - remember the locales are being translated!
			log.debug("Testing " + randomDate.toString() + " ==> " + datePath);

			assertTrue("Year part must be present", datePath.startsWith(cal.get(Calendar.YEAR) + "/"));
			assertTrue("Month part must be present", datePath.contains(1 + cal.get(Calendar.MONTH) + "/"));
			assertTrue("Day part must be present", datePath.contains(cal.get(Calendar.DAY_OF_MONTH) + "/"));
			assertTrue("Hour part must be present", datePath.contains(cal.get(Calendar.HOUR_OF_DAY) + "/"));
			assertTrue("Minute part must be present", datePath.endsWith(cal.get(Calendar.MINUTE) + ""));

		}

	}

	@Test
	public void dateRangeRetrievalTest() {

		Random rnd = new Random(System.currentTimeMillis());

		// Populate 100 audit events, each a day apart with random minutes and seconds times
		int numEvents = 100;

		Instant iteratorInstant = LocalDateTime.of(
				2011, 3, 1,
				0,0,0,0
		).atZone(S3PersistenceManager.UTC_ZONE).toInstant();

		for (int i=0; i<numEvents; i++) {

			ZonedDateTime iteratorZdt = iteratorInstant.atZone(S3PersistenceManager.UTC_ZONE);

			Instant createdInstant = LocalDateTime.of(
					iteratorZdt.getYear(), iteratorZdt.getMonth(), iteratorZdt.getDayOfMonth(),
					rnd.nextInt(24), rnd.nextInt(60), rnd.nextInt(60), 0
			).atZone(S3PersistenceManager.UTC_ZONE).toInstant();

			AuditEvent auditEvent = getStubAuditEvent(Date.from(createdInstant));
			enrichAuditEvent(auditEvent);

			S3PersistenceManager.saveAuditEvent(auditEvent);

			iteratorInstant = iteratorInstant.plus(1, ChronoUnit.DAYS);

		}

		// Case 1: Search just the month of 2011/03, should get 31 records back.  Tests iterating hours.
		{
			Instant fromDate = LocalDateTime.of(
					2011, 3, 1,
					0, 0, 0, 0
			).atZone(S3PersistenceManager.UTC_ZONE).toInstant();

			Instant toDate = LocalDateTime.of(
					2011, 3, 31,
					23, 59, 59, 0
			).atZone(S3PersistenceManager.UTC_ZONE).toInstant();

			final AtomicInteger counter = new AtomicInteger(0);
			S3PersistenceManager.iterateByDateRange(tenantId, Date.from(fromDate), Date.from(toDate), (objKey) -> {
				counter.incrementAndGet();
			});

			assertTrue("Should get 31 records; Got:" + counter.get(), 31 == counter.get());
		}

		// Case 2: Search from 2011/02, through 2011/04, should get 100 records back.  Tests iterating days.
		{
			Instant fromDate = LocalDateTime.of(
					2011, 2, 1,
					0, 0, 0, 0
			).atZone(S3PersistenceManager.UTC_ZONE).toInstant();

			Instant toDate = LocalDateTime.of(
					2011, 6, 30,
					23, 59, 59, 0
			).atZone(S3PersistenceManager.UTC_ZONE).toInstant();

			final AtomicInteger counter = new AtomicInteger(0);
			S3PersistenceManager.iterateByDateRange(tenantId, Date.from(fromDate), Date.from(toDate), (objKey) -> {
				counter.incrementAndGet();
			});

			assertTrue("Should get 100 records; Got:" + counter.get(), 100 == counter.get());
		}

	}

}
