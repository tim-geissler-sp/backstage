/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.atlas.AtlasApplication;
import com.sailpoint.atlas.OrgData;
import com.sailpoint.atlas.OrgDataProvider;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.atlas.security.AdministratorSecurityContext;
import com.sailpoint.audit.persistence.S3PersistenceManager;
import com.sailpoint.mantis.platform.MantisApplication;
import com.sailpoint.mantis.platform.db.SailPointContextProviderCache;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.util.Strings;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Objects.requireNonNull;

/**
 * Service that handles requests to bulk-sync an Org's CIS AuditEvents over to S3 by-time storage bucket. <br><br>
 *
 * When an org sync request comes in its OrgData JSON gets added to a Redis SET that acts as a queue for bulk sync
 * requests.  A worker thread pulls these records out of the queue, one at a time, and syncs that one Org's data
 * from CIS to S3, using worker threads that query and sync Audit Event records one day per worker thread, with
 * up to a month's worth of worker threads firing concurrently.  {@link com.sailpoint.audit.service.SyncCisToS3Worker}
 * has the details on the concurrent worker threads.
 * <br><br>
 *
 * This decouples the long-running job of bulk-sync'ing data from the handling request (HTTP, Iris, or Redis message).
 */
@Singleton
public class SyncCisToS3Service {

	public static final Log log = LogFactory.getLog(S3PersistenceManager.class);

	public static final String REDIS_SYNC_SET_KEY  = "aerCisToS3SyncSet";

	public static final ExecutorService execSvc = Executors.newSingleThreadExecutor(
			new ThreadFactoryBuilder()
					.setNameFormat("cis2s3-job-%d")
					.setDaemon(true) // Allow process exit when long-running CIS->S3 sync job is active.
					.build()
	);

	@Inject
	RedisPool _redisPool;

	@Inject
	OrgDataProvider _orgDataProvider;

	/**
	 * Inner class that processes of sync jobs from the sync set stored in Redis. The run() method
	 * is carried out in the ExecutorService's thread pool, so this is decoupled from Iris, Redis or REST
	 * processing.
	 */
	public class SyncJobProcessor implements Runnable {

		RedisPool redisPool;
		SailPointContextProviderCache providerCache;

		public SyncJobProcessor(RedisPool redisPool) {
			this.redisPool = redisPool;
			this.providerCache = ((MantisApplication)AtlasApplication.getInstance()).getContextProviderCache();
		}

		/**
		 * Try to pull a queue item out of the Redis set. Upon success, run
		 */
		@Override
		public void run() {

			int orgDataCount = 0;
			OrgData orgData = null;
			try {
				do {
					orgData = redisPool.exec(jedis -> {

						String orgDataJson = jedis.spop(REDIS_SYNC_SET_KEY);
						if (Strings.isBlank(orgDataJson)) return null;

						// Older model: This is a serialized JSON of OrgData object.
						if (orgDataJson.trim().startsWith("{")) {
							return JsonUtils.parse(OrgData.class, orgDataJson);
						}

						// Newer model: The contents are simply an orgName string.
						// In development environments the Atom orgs get deleted frequently.
						// It is common for this code to reach for an org that is no longer present.
						try {
							return _orgDataProvider.ensureFind(orgDataJson.trim());
						} catch (java.util.NoSuchElementException nseEx) {
							log.warn("Failure looking up CIS->S3 org: " + orgDataJson, nseEx);
							return null;
						}

					});

					if (null != orgData) {

						orgDataCount++;

						// Set the RequestContext so our log messages present MDC data appropriately.
						RequestContext requestContext = new RequestContext();
						requestContext.setOrgData(orgData);
						requestContext.setSecurityContext(new AdministratorSecurityContext());
						RequestContext.set(requestContext);

						log.info("Starting CIS->S3 Sync for " + orgData.getOrg());
						SyncCisToS3Job syncCisToS3Job = new SyncCisToS3Job();
						syncCisToS3Job.syncOrgCisToS3(orgData, providerCache, _redisPool);

					}

				} while (null != orgData);

				log.info("Processor exiting for CIS->S3 sync, orgDataCount:" + orgDataCount);

			} catch (Exception ex) {

				// Redact the JDBC connection attributes in what we present for logging.
				OrgData redactedOrgData = new OrgData();
				redactedOrgData.setOrg(orgData.getOrg());
				redactedOrgData.setPod(orgData.getPod());
				redactedOrgData.setTenantId(orgData.getTenantId().get());

				log.error("Failure processing CIS->S3 sync, redacted orgData:" +
						JsonUtils.toJson(redactedOrgData) + " orgDataCount:" + orgDataCount, ex);
			}
		}

	}

	/**
	 * Enqueues an org for CIS->S3 syncing.  Adds a Key to a Redis SET of keys of OrgData
	 * for orgs that are queued to be sync'ed over.  Kicks off a worker thread pool Runnable
	 * to pull out any records from that SET and run a sync for any keys that come back from
	 * Redis.
	 *
	 * @param orgData - The org to enqueue for sync'ing.
	 */
	public void enqueueOrgForSyncing (OrgData orgData) {

		String orgDataJson = JsonUtils.toJson(orgData);

		// Sanity check that the OrgData has a `tenantId` property. We found in Dev that there
		// are circumstances where the Optional is not present/populated, catch that dirty data here.
		if (!orgData.getTenantId().isPresent()) {
			log.warn(String.format(
					"Skipping CIS->S3 sync for Org %s with no tenantId present. orgDataJson: %s",
					orgData.getOrg(),
					orgDataJson
			));
			return;
		}

		// More sanity checking and data cleanup.
		if (Strings.isBlank(orgData.getOrg())) {
			log.warn(String.format(
					"Skipping CIS->S3 sync for OrgData with no org name present. orgDataJson: %s",
					orgDataJson
			));
			return;
		}

		// More sanity checking and data cleanup.
		if (Strings.isBlank(orgData.getTenantId().orElse(""))) {
			log.warn(String.format(
					"Skipping CIS->S3 sync for OrgData with blank tenantId. orgDataJson: %s",
					orgDataJson
			));
			return;
		}

		_redisPool.exec(jedis -> jedis.sadd(REDIS_SYNC_SET_KEY, orgData.getOrg()));

		log.info(String.format("Added org:%s tenantId:%s to Redis queue for CIS->S3 sync",
				orgData.getOrg(), orgData.getTenantId().get()));

		// Tell the executor service to kick off another cycle of looking for orgs to sync in Redis.
		SyncJobProcessor sjp = new SyncJobProcessor(_redisPool);
		execSvc.execute(sjp);

	}

}
