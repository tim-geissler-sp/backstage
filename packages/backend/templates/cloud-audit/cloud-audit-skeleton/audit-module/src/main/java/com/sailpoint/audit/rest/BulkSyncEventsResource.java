/*
 *
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 *
 */

package com.sailpoint.audit.rest;

import com.google.inject.Inject;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.OrgData;
import com.sailpoint.atlas.OrgDataProvider;
import com.sailpoint.atlas.idn.IdnMessageScope;
import com.sailpoint.atlas.messaging.client.Job;
import com.sailpoint.atlas.messaging.client.JobSubmission;
import com.sailpoint.atlas.messaging.client.Payload;
import com.sailpoint.atlas.security.RequireRight;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.atlas.service.MessageClientService;
import com.sailpoint.audit.message.BulkSyncAuditEvents;
import com.sailpoint.audit.message.BulkSyncPayload;
import com.sailpoint.audit.message.BulkSyncS3AuditEvents;
import com.sailpoint.audit.message.BulkUploadAuditEvents;
import com.sailpoint.audit.message.BulkUploadPayload;
import com.sailpoint.audit.message.CisToS3Payload;
import com.sailpoint.audit.service.SyncCisToS3Service;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.util.Strings;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("bulk")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BulkSyncEventsResource {

	public static final Log log = LogFactory.getLog(BulkSyncEventsResource.class);

	@Inject
	MessageClientService _messageClientService;

	@Inject
	SyncCisToS3Service _syncCisToS3Service;

	@Inject
	FeatureFlagService _featureFlagService;

	@Inject
	OrgDataProvider _orgDataProvider;

	@Inject
	AtlasConfig _atlasConfig;

	@POST
	@Path("sync")
	@Produces(MediaType.APPLICATION_JSON)
	@RequireRight("idn:audit-bulk-sync:create")
	public Response runBulkSync(BulkSyncPayload bulkSyncPayload){
		JobSubmission syncJob = new JobSubmission(
					new Payload(BulkSyncAuditEvents.PAYLOAD_TYPE.BULK_SYNCHRONIZE_AUDIT_EVENTS, bulkSyncPayload));
		Job job = _messageClientService.submitJob(IdnMessageScope.AUDIT, syncJob);

		return Response.status(HttpServletResponse.SC_OK).entity(job).build();
	}

	@POST
	@Path("sync/reset")
	@Produces(MediaType.APPLICATION_JSON)
	@RequireRight("idn:audit-bulk-sync:create")
	public Response runSyncReset(BulkSyncPayload bulkSyncPayload){
		Map<String,Object> arguments = new HashMap<>();
		arguments.put("purgeOrg", true);
		bulkSyncPayload.setArguments(arguments);
		bulkSyncPayload.setReset(true);
		bulkSyncPayload.setOverride(true);
		JobSubmission syncJob = new JobSubmission(
				new Payload(BulkSyncAuditEvents.PAYLOAD_TYPE.BULK_SYNCHRONIZE_AUDIT_EVENTS, bulkSyncPayload));
		Job job = _messageClientService.submitJob(IdnMessageScope.AUDIT, syncJob);

		return Response.status(HttpServletResponse.SC_OK).entity(job).build();
	}

	@POST
	@Path("s3/sync")
	@Produces(MediaType.APPLICATION_JSON)
	@RequireRight("idn:audit-bulk-sync:create")
	public Response runBulkS3Sync(BulkSyncPayload bulkSyncPayload){
		JobSubmission syncJob = new JobSubmission(
				new Payload(BulkSyncS3AuditEvents.PAYLOAD_TYPE.BULK_SYNCHRONIZE_S3_AUDIT_EVENTS, bulkSyncPayload));
		Job job = _messageClientService.submitJob(IdnMessageScope.AUDIT, syncJob);

		return Response.status(HttpServletResponse.SC_OK).entity(job).build();
	}

	@POST
	@Path("s3/sync/reset")
	@Produces(MediaType.APPLICATION_JSON)
	@RequireRight("idn:audit-bulk-sync:create")
	public Response runBulkS3SyncReset(BulkSyncPayload bulkSyncPayload){
		bulkSyncPayload.setReset(true);
		bulkSyncPayload.setOverride(true);
		JobSubmission syncJob = new JobSubmission(
				new Payload(BulkSyncS3AuditEvents.PAYLOAD_TYPE.BULK_SYNCHRONIZE_S3_AUDIT_EVENTS, bulkSyncPayload));
		Job job = _messageClientService.submitJob(IdnMessageScope.AUDIT, syncJob);

		return Response.status(HttpServletResponse.SC_OK).entity(job).build();
	}

	@GET
	@Path("s3/sync/count")
	@Produces(MediaType.APPLICATION_JSON)
	@RequireRight("idn:audit-bulk-sync:create")
	public Response runBulkS3SyncCount(){
		BulkSyncPayload bulkSyncPayload = new BulkSyncPayload();
		bulkSyncPayload.setCountOnly(true);
		JobSubmission syncJob = new JobSubmission(
				new Payload(BulkSyncS3AuditEvents.PAYLOAD_TYPE.BULK_SYNCHRONIZE_S3_AUDIT_EVENTS, bulkSyncPayload));
		Job job = _messageClientService.submitJob(IdnMessageScope.AUDIT, syncJob);

		return Response.status(HttpServletResponse.SC_OK).entity(job).build();
	}

	@POST
	@Path("upload")
	@Produces(MediaType.APPLICATION_JSON)
	@RequireRight("idn:audit-bulk-sync:create")
	public Response uploadAuditEventsIntoS3(BulkUploadPayload bulkUploadPayload) {
		JobSubmission syncJob = new JobSubmission(
				new Payload(BulkUploadAuditEvents.PAYLOAD_TYPE.BULK_UPLOAD_AUDIT_EVENTS, bulkUploadPayload));
		Job job = _messageClientService.submitJob(IdnMessageScope.AUDIT, syncJob);

		return Response.status(HttpServletResponse.SC_OK).entity(job).build();
	}

	@POST
	@Path("upload/reset")
	@Produces(MediaType.APPLICATION_JSON)
	@RequireRight("idn:audit-bulk-sync:create")
	public Response resetStatus(BulkUploadPayload bulkUploadPayload) {
		bulkUploadPayload.setReset(true);
		bulkUploadPayload.setOverride(true);
		JobSubmission syncJob = new JobSubmission(
				new Payload(BulkUploadAuditEvents.PAYLOAD_TYPE.BULK_UPLOAD_AUDIT_EVENTS, bulkUploadPayload));
		Job job = _messageClientService.submitJob(IdnMessageScope.AUDIT, syncJob);

		return Response.status(HttpServletResponse.SC_OK).entity(job).build();
	}

	@GET
	@Path("count")
	@RequireRight("idn:audit-bulk-sync:create")
	public Response count() {
		List<Job> jobs = new ArrayList<>();

		BulkUploadPayload  bulkUploadPayload = new BulkUploadPayload();
		bulkUploadPayload.setCountOnly(true);
		JobSubmission uploadJob = new JobSubmission(
				new Payload(BulkUploadAuditEvents.PAYLOAD_TYPE.BULK_UPLOAD_AUDIT_EVENTS, bulkUploadPayload));
		jobs.add(_messageClientService.submitJob(IdnMessageScope.AUDIT, uploadJob));

		BulkSyncPayload bulkSyncPayload = new BulkSyncPayload();
		bulkSyncPayload.setCountOnly(true);
		JobSubmission syncJob = new JobSubmission(
				new Payload(BulkSyncS3AuditEvents.PAYLOAD_TYPE.BULK_SYNCHRONIZE_S3_AUDIT_EVENTS, bulkSyncPayload));
		jobs.add(_messageClientService.submitJob(IdnMessageScope.AUDIT, syncJob));

		return Response.status(HttpServletResponse.SC_OK).entity(jobs).build();
	}

	/**
	 * Request that a specific org or ALL orgs get queued for CIS->S3 synchronization.  Payload for POST simply looks
	 * like: <pre>{ "orgName": "perflab-ahampton" }</pre> or <pre>{ "orgName": "*" }</pre>.  Successful calls return
	 * a 200 and either the JSON Payload of the OrgData (with secrets redacted) or just the star string for all orgs.
	 *
	 * This is an _internal_ only API and requires permissions to kick off the batch job.
	 *
	 * @param cisToS3Payload
	 * @return
	 */
	@POST
	@Path("cis2s3/sync")
	// @RequireRight("idn:audit-bulk-sync:create")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postCis2S3BulkSync(CisToS3Payload cisToS3Payload){

		if (null == cisToS3Payload) {
			return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity("Invalid CisToS3Payload").build();
		}

		String orgName = cisToS3Payload.getOrgName();

		if (Strings.isBlank(orgName)) {
			return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity("Invalid CisToS3Payload.orgName").build();
		}

		log.info("HTTP Request to CIS->S3 Bulk Sync org: " + orgName);

		// Special case for kicking off syncs for all orgs in the region.
		if ("*".equals(orgName)) {
			ArrayList<String> orgNames = new ArrayList<>();
			for (String pod : _atlasConfig.getPods()) {
				for (OrgData orgData : _orgDataProvider.findAll(pod)) {
					_syncCisToS3Service.enqueueOrgForSyncing(orgData);
					orgNames.add(orgData.getOrg());
				}
			}
			return Response.status(HttpServletResponse.SC_OK).entity(orgNames).build();
		}

		OrgData orgData = _orgDataProvider.ensureFind(orgName);
		_syncCisToS3Service.enqueueOrgForSyncing(orgData);

		// Redact the attributes in what we return to the caller.
		OrgData redactedOrgData = new OrgData();
		redactedOrgData.setOrg(orgData.getOrg());
		redactedOrgData.setPod(orgData.getPod());
		redactedOrgData.setTenantId(orgData.getTenantId().get());

		return Response.status(HttpServletResponse.SC_OK).entity(redactedOrgData).build();

	}

}
