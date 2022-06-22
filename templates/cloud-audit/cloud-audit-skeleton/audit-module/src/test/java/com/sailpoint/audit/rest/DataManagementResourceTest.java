/*
 * Copyright (C) 2021. SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.rest;

import com.sailpoint.atlas.idn.IdnMessageScope;
import com.sailpoint.atlas.messaging.client.JobSubmission;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.atlas.service.MessageClientService;
import com.sailpoint.audit.service.FeatureFlags;
import com.sailpoint.audit.service.model.AddAthenaPartitionsDTO;
import com.sailpoint.audit.service.model.PublishAuditCountsDTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.any;

/**
 * Unit tests for DataManagementResource .
 *  /audit/data/add-partitions
 */
public class DataManagementResourceTest {

	@Mock
	MessageClientService _messageClientService;

	@Mock
	FeatureFlagService _featureFlagService;

	DataManagementResource _resource;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		_resource = new DataManagementResource();
		_resource._featureFlagService = _featureFlagService;
		_resource._messageClientService = _messageClientService;
	}

	@Test
	public void testPublishAuditEventCounts() {
		when(_featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, eq(anyBoolean())))
				.thenReturn(true);
		Response response = _resource.publishAuditEventCounts();
		verify(_messageClientService, times(1)).submitJob(eq(IdnMessageScope.AUDIT),
				any(JobSubmission.class));
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());

		when(_featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, eq(anyBoolean())))
				.thenReturn(false);
		response = _resource.publishAuditEventCounts();
		verifyZeroInteractions(_messageClientService);
		Assert.assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());

	}

	@Test
	public void testPublishAuditEventCountsGivenDate() {
		when(_featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, eq(anyBoolean())))
				.thenReturn(true);
		PublishAuditCountsDTO payload = new PublishAuditCountsDTO("2021-03-24");
		Response response = _resource.publishAuditEventCounts(payload);
		verify(_messageClientService, times(1)).submitJob(eq(IdnMessageScope.AUDIT),
				any(JobSubmission.class));
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());

		when(_featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, eq(anyBoolean())))
				.thenReturn(false);
		response = _resource.publishAuditEventCounts(payload);
		verifyZeroInteractions(_messageClientService);
		Assert.assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());

	}

	@Test
	public void testAddAthenaPartitions() {
		when(_featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, eq(anyBoolean())))
				.thenReturn(true);
		Response response = _resource.addAthenaPartitions();
		verify(_messageClientService, times(1)).submitJob(eq(IdnMessageScope.AUDIT),
				anyObject(), any());
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());

		when(_featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, eq(anyBoolean())))
				.thenReturn(false);
		response = _resource.addAthenaPartitions();
		verifyZeroInteractions(_messageClientService);
		Assert.assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());

	}

	@Test
	public void testAddAthenaPartitionsGivenDate() {
		when(_featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, eq(anyBoolean())))
				.thenReturn(true);
		AddAthenaPartitionsDTO payload = new AddAthenaPartitionsDTO("2021-03-24");
		Response response = _resource.addAthenaPartitions(payload);
		verify(_messageClientService, times(1)).submitJob(eq(IdnMessageScope.AUDIT),
				anyObject());
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());

		when(_featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, eq(anyBoolean())))
				.thenReturn(false);
		response = _resource.addAthenaPartitions(payload);
		verifyZeroInteractions(_messageClientService);
		Assert.assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());

	}

}
