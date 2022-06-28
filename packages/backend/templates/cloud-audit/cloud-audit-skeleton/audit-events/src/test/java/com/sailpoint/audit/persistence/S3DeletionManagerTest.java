/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.persistence;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.sailpoint.mantis.core.service.model.AuditEventActions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import sailpoint.object.AuditEvent;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.sailpoint.audit.utils.TestUtils.setDummyRequestContext;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class S3DeletionManagerTest {

	public static final Log log = LogFactory.getLog(S3DeletionManagerTest.class);

	private static final String tenantId = UUID.randomUUID().toString();
	private static final String org = "acme-solar-auevt";
	private static final String pod = "dev";

	private static final ArrayList<String> tenantIdsToPurgeAfterTesting = new ArrayList<>();

	Date nowDate = new Date();

	/**
	 * The s3Client is usually injected by Google Guice.  For testing we need our own, so we side load one.
	 */
	@Before
	public void allocateTestingS3Client() {

		final int CLIENT_EXECUTION_TIMEOUT = 300000;

		AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
				.withClientConfiguration(
						new ClientConfiguration().withClientExecutionTimeout(CLIENT_EXECUTION_TIMEOUT)
				).build();

		S3PersistenceManager.overrideS3Client(s3Client);
	}

	@Before
	public void setContextValues() {
		setDummyRequestContext(tenantId, org, pod);
	}

	@After
	public void cleanupTestKeyInS3() {
		S3PersistenceManager.deleteAuditEventsForTenant(tenantId);
		for (String toDelete : tenantIdsToPurgeAfterTesting) {
			S3PersistenceManager.deleteAuditEventsForTenant(toDelete);
		}
		tenantIdsToPurgeAfterTesting.clear();
	}

	// Get a random date any time in the last 7 years.
	public Date getRandomDate() {
		long startMillis = nowDate.getTime() - (7 * 31_536_000_000L); // Now minus 5 years of milliseconds.
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
		auditEvent.setAttribute("attrib1Key", "attrib1Val");
		auditEvent.setAttribute("attrib2Key", "attrib2Val");
		auditEvent.setDescription("A unit or integration test AuditEvent");
		auditEvent.setInterface("Is this ever used!?");
	}

	@Test
	public void singleOrgDeletionTest() {

		int numCycles = 17;
		AtomicInteger eventCount = new AtomicInteger(0);

		ExecutorService es = Executors.newFixedThreadPool(8);

		for (int i=0; i<numCycles; i++) {
			es.execute(() -> {
				setContextValues(); // We need tenantId, etc. in context.
				AuditEvent auditEvent = getStubAuditEvent(null);
				enrichAuditEvent(auditEvent);
				log.info(Thread.currentThread().getName() + " key:" + S3PersistenceManager.getObjectKey(auditEvent));
				S3AuditEventEnvelope env1 = S3PersistenceManager.saveAuditEvent(auditEvent);
				eventCount.incrementAndGet();
			});
		}

		// Wait for the worker threads to complete, bail on any errors.
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

		// Mark the org for deletion.
		S3DeletionManager.queueTenantForDeletion(tenantId);

		// Poll for completion.
		int cycle = 0;
		final AtomicInteger numItems = new AtomicInteger(0);
		do  {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			numItems.set(0);
			S3PersistenceManager.iterateObjectKeys(tenantId + "/", objKey -> numItems.incrementAndGet());
			log.info("Found " + numItems.get() + " remaining in for tenantID: " + tenantId);
			assertTrue("Records not purged after 30 seconds.", cycle++ < 30);
		} while (numItems.get() > 0);

	}

	@Test
	public void multiOrgDeletionTest() {

		ArrayList<String> tenants = new ArrayList<>();
		int numOrgsToTest = 5;
		for (int i=0; i<numOrgsToTest; i++) {
			String uuid = UUID.randomUUID().toString();
			tenants.add(uuid);
			tenantIdsToPurgeAfterTesting.add(uuid);
		}

		// Populate audit events for N tenants in worker threads.
		ExecutorService es = Executors.newFixedThreadPool(8);
		for (String tenantId : tenants) {
			es.execute(() -> {
				setDummyRequestContext(tenantId, org, pod);
				int numEventsToMake = 17 + ThreadLocalRandom.current().nextInt(113);
				for (int i=0; i<numEventsToMake; i++) {
					AuditEvent auditEvent = getStubAuditEvent(null);
					enrichAuditEvent(auditEvent);
					log.info(Thread.currentThread().getName() + " key:" + S3PersistenceManager.getObjectKey(auditEvent));
					S3AuditEventEnvelope env1 = S3PersistenceManager.saveAuditEvent(auditEvent);
				}
			});
		}

		// Wait for the worker threads to complete, bail on any errors.
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

		// Mark the orgs for deletion.
		for (String tenantId: tenants) {
			S3DeletionManager.queueTenantForDeletion(tenantId);
		}

		for (String tenantId: tenants) {
			// Poll for completion.
			int cycle = 0;
			final AtomicInteger numItems = new AtomicInteger(0);
			do {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				numItems.set(0);
				S3PersistenceManager.iterateObjectKeys(tenantId + "/", objKey -> numItems.incrementAndGet());
				log.info("Found " + numItems.get() + " remaining in for tenantID: " + tenantId);
				assertTrue("Records not purged after 30 seconds.", cycle++ < 30);
			} while (numItems.get() > 0);
		}

	}

}
