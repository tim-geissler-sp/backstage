/*
 * Copyright (c) 2017. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.service;

import com.sailpoint.atlas.idn.RestClientProvider;
import com.sailpoint.atlas.idn.ServiceNames;
import com.sailpoint.audit.service.model.ReportDTO;
import com.sailpoint.mantisclient.BaseRestClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by mark.boyle on 4/25/17.
 */
public class RDEServiceClientTest {

	@Mock
	RestClientProvider _restClientProvider;

	@Mock
	BaseRestClient _baseRestClient;

	@Captor
	ArgumentCaptor<ReportDTO> _reportDTOCaptor;

	RDEServiceClient _rdeServiceClient;

	@Before
	public void setup(){
		MockitoAnnotations.initMocks(this);

		_rdeServiceClient = new RDEServiceClient();
		_rdeServiceClient._restClientProvider = _restClientProvider;
	}

	@Test
	public void reportResultTest() {

		BaseRestClient mockRestClient = getRestClient();

		String reportName = "audit-type-report-all";
		ReportDTO reportDTO = _rdeServiceClient.reportResult(reportName,7);

		verify(mockRestClient).postJson(eq(ReportDTO.class), eq("reporting/reports/result"), any(ReportDTO.class));
	}

	@Test
	public void reportResultThrowsException() {
		BaseRestClient mockRestClient = mock(BaseRestClient.class);
		doThrow(new RuntimeException("some")).when(mockRestClient).postJson(any(), any(), any());

		String reportName = "audit-type-report-all";
		ReportDTO reportDTO = _rdeServiceClient.reportResult(reportName,7);
		assertNotNull(reportDTO);
	}

	@Test
	public void reportRunTest() {

		BaseRestClient mockRestClient = getRestClient();

		String reportName = "audit-type-report-source";
		Map<String, Object> args = new HashMap<>();

		args.put("actions", Arrays.asList("foo", "bar", "baz"));
		args.put("numDays", 7);
		ReportDTO reportDTO = _rdeServiceClient.runReport(reportName,7, args);

		verify(mockRestClient).postJson(eq(ReportDTO.class), eq("reporting/reports/run"), _reportDTOCaptor.capture());
		Assert.assertNotNull(_reportDTOCaptor.getValue().getArguments());
		Map<String, Object> attrs = _reportDTOCaptor.getValue().getArguments();
		Assert.assertNotNull(attrs.get("actions"));
		Assert.assertEquals(((List<String>)attrs.get("actions")).size(), 3);
		Assert.assertEquals(attrs.get("numDays"), 7);

	}

	@Test
	public void testRunReportThrowsException() {
		String reportName = "audit-type-report-source";
		Map<String, Object> args = new HashMap<>();

		args.put("actions", Arrays.asList("foo", "bar", "baz"));
		args.put("numDays", 7);

		BaseRestClient mockRestClient = mock(BaseRestClient.class);
		doThrow(new RuntimeException("some")).when(mockRestClient).postJson(any(), any(), any());

		ReportDTO reportDTO = _rdeServiceClient.runReport(reportName, 7, args);

		assertNotNull(reportDTO);
	}

	private BaseRestClient getRestClient(){
		BaseRestClient mockRestClient = mock(BaseRestClient.class);
		when(_restClientProvider.getRestClient(ServiceNames.RDE)).thenReturn(mockRestClient);

		return mockRestClient;
	}
}
