/*
 *
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 *
 */

package com.sailpoint.audit.rest;

import com.google.inject.Inject;
import com.sailpoint.atlas.idn.IdnMessageScope;
import com.sailpoint.atlas.messaging.client.Job;
import com.sailpoint.atlas.messaging.client.JobSubmission;
import com.sailpoint.atlas.messaging.client.MessagePriority;
import com.sailpoint.atlas.messaging.client.Payload;
import com.sailpoint.atlas.messaging.client.SendMessageOptions;
import com.sailpoint.atlas.security.RequireRight;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.atlas.service.MessageClientService;
import com.sailpoint.audit.message.AddAthenaPartitions;
import com.sailpoint.audit.message.PublishAuditCounts;
import com.sailpoint.audit.service.FeatureFlags;
import com.sailpoint.audit.service.model.AddAthenaPartitionsDTO;
import com.sailpoint.audit.service.model.PublishAuditCountsDTO;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.POST;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;


@Path("data")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DataManagementResource {

	@Inject
	MessageClientService _messageClientService;

	@Inject
	FeatureFlagService _featureFlagService;

	@POST
	@Path("add-partitions")
	@Produces(MediaType.APPLICATION_JSON)
	@RequireRight("idn:audit-athena-partitions:add")
	public Response addAthenaPartitions(){
		if( _featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, false) ) {
			AddAthenaPartitionsDTO payload = new AddAthenaPartitionsDTO();
			JobSubmission addAthenaPartitionsJob = new JobSubmission(
					new Payload(AddAthenaPartitions.PAYLOAD_TYPE.ADD_ATHENA_PARTITIONS, payload));

			SendMessageOptions messageOptions = new SendMessageOptions(MessagePriority.LOW);

			Job job = _messageClientService.submitJob(IdnMessageScope.AUDIT, addAthenaPartitionsJob, messageOptions);

			return Response.status(HttpServletResponse.SC_OK).entity(job).build();
		}

		return Response.noContent().build();
	}

	@POST
	@Path("add-partitions-givendate")
	@Produces(MediaType.APPLICATION_JSON)
	@RequireRight("idn:audit-athena-partitions:add")
	public Response addAthenaPartitions(AddAthenaPartitionsDTO payload){
		if( _featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, false) ) {
			JobSubmission addAthenaPartitionsJob = new JobSubmission(
					new Payload(AddAthenaPartitions.PAYLOAD_TYPE.ADD_ATHENA_PARTITIONS, payload));
			Job job = _messageClientService.submitJob(IdnMessageScope.AUDIT, addAthenaPartitionsJob);

			return Response.status(HttpServletResponse.SC_OK).entity(job).build();
		}

		return Response.noContent().build();
	}

	@POST
	@Path("publish-counts")
	@Produces(MediaType.APPLICATION_JSON)
	@RequireRight("idn:audit-data-metrics:publish")
	public Response publishAuditEventCounts(){
		if( _featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, false) ) {
			//submit the job to publish metrics for previous day - this API gets call at 00:05 as per TPE schedule.
			PublishAuditCountsDTO payload = new PublishAuditCountsDTO(LocalDate.now().minus(1, ChronoUnit.DAYS).toString());
			JobSubmission publishAuditCountsJob = new JobSubmission(
					new Payload(PublishAuditCounts.PAYLOAD_TYPE.PUBLISH_AUDIT_COUNTS, payload));
			Job job = _messageClientService.submitJob(IdnMessageScope.AUDIT, publishAuditCountsJob);
			return Response.status(HttpServletResponse.SC_OK).entity(job).build();
		}

		return Response.noContent().build();
	}

	@POST
	@Path("publish-counts-givendate")
	@Produces(MediaType.APPLICATION_JSON)
	@RequireRight("idn:audit-data-metrics:publish")
	public Response publishAuditEventCounts(PublishAuditCountsDTO payload){
		if( _featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, false) ) {
			JobSubmission publishAuditCountsJob = new JobSubmission(
					new Payload(PublishAuditCounts.PAYLOAD_TYPE.PUBLISH_AUDIT_COUNTS, payload));
			Job job = _messageClientService.submitJob(IdnMessageScope.AUDIT, publishAuditCountsJob);
			return Response.status(HttpServletResponse.SC_OK).entity(job).build();
		}

		return Response.noContent().build();
	}

}
