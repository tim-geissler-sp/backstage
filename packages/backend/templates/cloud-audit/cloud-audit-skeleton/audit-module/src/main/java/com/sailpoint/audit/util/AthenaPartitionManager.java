/*
 * Copyright (C) 2021 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.idn.IdnMessageScope;
import com.sailpoint.atlas.messaging.client.JobSubmission;
import com.sailpoint.atlas.messaging.client.Payload;
import com.sailpoint.atlas.service.MessageClientService;
import com.sailpoint.audit.message.PublishAuditCounts;
import com.sailpoint.audit.service.DataCatalogService;
import com.sailpoint.audit.service.model.PublishAuditCountsDTO;
import com.sailpoint.audit.service.util.AuditUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Support for adding Athena Partition additions in worker threads.
 */
public class AthenaPartitionManager implements Runnable {

	public static final Log log = LogFactory.getLog(AthenaPartitionManager.class);

	public static final ExecutorService execSvc = Executors.newFixedThreadPool( 32,
			new ThreadFactoryBuilder()
					.setNameFormat("athena-partition-%d")
					.setDaemon(true) // Allow process exit when long-running S3 purge job is active.
					.build()
	);

	// Counter incremented when a request is submitted for processing.
	@VisibleForTesting
	public static final AtomicLong submissionCounter = new AtomicLong(0L);

	// Counter incremented after a partition is successfully added.
	@VisibleForTesting
	public static final AtomicLong completionCounter = new AtomicLong(0L);

	DataCatalogService _athenaService;

	AtlasConfig _atlasConfig;

	MessageClientService _messageClientService;

	String _orgName;
	String _pod;
	String _partitionDate;

	public AthenaPartitionManager(String org, String pod, String partitionDate, AtlasConfig atlasConfig,
								  DataCatalogService athenaService, MessageClientService messageClientService) {
		this._orgName = org;
		this._pod = pod;
		this._partitionDate = partitionDate;
		this._atlasConfig = atlasConfig;
		this._athenaService = athenaService;
		this._messageClientService = messageClientService;
	}

	/**
	 * Starts a task that runs a job to add a partition to an Athena database in the background.  This decouples
	 * the processing of Athena add partition workloads from threads servicing Redis messages.
	 *  @param partitionDate
	 * @param atlasConfig
	 * @param athenaService
	 * @param messageClientService
	 */
	public static void addPartition(String partitionDate, AtlasConfig atlasConfig,
									DataCatalogService athenaService, MessageClientService messageClientService) {

		RequestContext requestContext = RequestContext.ensureGet();
		String orgName = requestContext.getOrg();
		String pod = requestContext.getPod();

		AthenaPartitionManager athPartMgr = new AthenaPartitionManager(orgName, pod, partitionDate, atlasConfig,
				athenaService, messageClientService);
		athPartMgr.submitWorkerThreadJob();
	}

	public void submitWorkerThreadJob() {
		execSvc.execute(this);
		submissionCounter.incrementAndGet();
	}

	@Override
	public void run() {

		// Include the org data in log messages.
		RequestContext reqCon = new RequestContext();
		reqCon.setOrg(this._orgName);
		reqCon.setPod(this._pod);
		RequestContext.set(reqCon);

		String dbName = _atlasConfig.getString("AER_AUDIT_ATHENA_DATABASE");
		String s3Bucket = _atlasConfig.getString("AER_AUDIT_PARQUET_DATA_S3_BUCKET");
		log.info("Adding partition for org:" + _orgName + " to " + s3Bucket +":" + dbName + " date:" + _partitionDate);

		try {

			_athenaService.addPartitions(
					dbName, AuditUtil.getOrgAuditAthenaTableName(_orgName), s3Bucket, _orgName, _partitionDate
			);

			PublishAuditCountsDTO payload = new PublishAuditCountsDTO(_partitionDate);

			JobSubmission publishAuditCountsJob = new JobSubmission(
					new Payload(PublishAuditCounts.PAYLOAD_TYPE.PUBLISH_AUDIT_COUNTS, payload)
			);

			_messageClientService.submitJob(IdnMessageScope.AUDIT, publishAuditCountsJob);

			completionCounter.incrementAndGet();


		} catch (Exception e) {
			log.error("Error adding partition for " + _partitionDate, e);
		}

		log.info("Completed partition add for:" + _orgName + " to " + s3Bucket +":" + dbName + " date:" + _partitionDate);

	}

}
