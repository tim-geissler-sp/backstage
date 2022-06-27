/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.persistence;

import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Objects.requireNonNull;

/**
 * When the AER service is cycled during a long-running org delete we may be left with orphaned
 * Audit Event records still under the no-longer used tenantId.  This implements a more durable job
 * that will resume purging of removed Org/Tenant's Audit Events after service re-starts.
 *
 * We can't simply implement an Org Delete handler in an Iris/Kafka handler. Kafka event handlers
 * time out after some large-ish (300?) number of seconds.  An org that has been around for years
 * might take minutes to hours to completely delete and purge their Audit Events from S3.  Handlers
 * have a few retry cycles to try and purge the remaining objects from S3, but that alone is not sufficient.
 *
 * Optimally we need some kind of state here: A list of tenantIds that have been requested / marked for
 * deletion and removal from S3.  We put those under the S3 bucket under a queue to be deleted by a worker
 * thread in the near future.  We have an S3 folder with objects like this:
 *
 *    _toDeleteQueue/${tenantId} - value simply contains just the String of the tenantId to delete.
 *
 * When an org delete request comes in its tenantId gets added to that bucket as an object.  Then AER
 * adds a request to a single-threaded runnable that iterates all the "delete requested" tenantIds, and
 * then calls the S3PersistenceManager's method to walk all those keys and remove them.  Once all a
 * tenant's keys are removed then the tenantId is removed from to-delete-queue and the worker thread
 * moves on to the next tenantId in the queue.  Once there is no more work to do the worker thread exits.
 * Several more worker requestes may be queued, and it is fine if they do a lot or almost zero work when
 * they finally execute.
 *
 * This decouples the long-running job of purging tenants from the handling of the org delete Kafka event.
 * This gives us a small, simple, database of tenants that we want to remove. And it makes deletions re-entrant
 * across service restarts; a resilient way for the processing of org deletes to be carried out.
 */
public class S3DeletionManager implements Runnable {

	public static final Log log = LogFactory.getLog(S3PersistenceManager.class);

	public static final String TO_DELETE_PREFIX = "_toDeleteQueue/";

	public static final ExecutorService execSvc = Executors.newSingleThreadExecutor(
			new ThreadFactoryBuilder()
					.setNameFormat("s3delete-worker-%d")
					.setDaemon(true) // Allow process exit when long-running S3 purge job is active.
					.build()
	);

	public static void queueTenantForDeletion(String tenantId) {

		requireNonNull(tenantId, "a tenantId UUID value is required");
		if (36 > tenantId.length()) {
			log.error("Refusing to enqueue an invalid non-UUID tenant ID for deletion: " + tenantId);
			return;
		}

		// S3's API prefers a file or InputStream payload.
		InputStream payloadToS3 = new ByteArrayInputStream(tenantId.getBytes(StandardCharsets.UTF_8));

		// Store the request to delete the tenant in: _toDeleteQueue/${tenantId}
		ObjectMetadata objMeta = new ObjectMetadata();
		objMeta.setContentType("text/plain");
		// Required to prevent `No content length specified for stream data.` warnings.
		objMeta.setContentLength(tenantId.getBytes(StandardCharsets.UTF_8).length);

		String path = TO_DELETE_PREFIX + tenantId;
		PutObjectRequest putObjectRequest = new PutObjectRequest(
				S3PersistenceManager.BUCKET_BY_TIME, path, payloadToS3, objMeta
		);

		// No exception indicates success.
		PutObjectResult putObjectResult = S3PersistenceManager.s3Client.putObject(putObjectRequest);

		// Queue up another run of the deletion worker.
		execSvc.execute(new S3DeletionManager());

	}

	@Override
	public void run() {

		log.info("Starting Audit Event deletion worker cycle.");

		for ( S3ObjectSummary summary : S3Objects.withPrefix(
				S3PersistenceManager.s3Client, S3PersistenceManager.BUCKET_BY_TIME, TO_DELETE_PREFIX)
		) {

			String tenantId = summary.getKey().replace(TO_DELETE_PREFIX, "");
			if (tenantId.isEmpty()) continue; // Do not delete the `_toDeleteQueue` folder key.

			// Sanity check to ensure we were given something resembling a valid UUID for tenantId.
			if (36 > tenantId.length()) {
				log.error("Refusing to delete all objects matching non-UUID tenantId: [" + tenantId + "]");
				continue; // on to next for() loop iteration.
			}

			log.info("Starting Audit Event deletion for tenantId: " + tenantId);

			long startTime = System.currentTimeMillis();

			// Run delete pass 2 times.  This catches laggard events coming in from other streams.
			S3PersistenceManager.deleteAuditEventsForTenant(tenantId);
			S3PersistenceManager.deleteAuditEventsForTenant(tenantId);

			// Remove the tenantId from the queue to delete.
			S3PersistenceManager.s3Client.deleteObject(S3PersistenceManager.BUCKET_BY_TIME, summary.getKey());

			long duration = System.currentTimeMillis() - startTime;

			log.info("Completed Audit Event deletion for tenantId: " + tenantId + " in msecs:" + duration);

		}

		log.info("Ending Audit Event deletion worker cycle.");
	}

}
