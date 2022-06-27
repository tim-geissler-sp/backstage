/*
 * Copyright (c) 2022 SailPoint Technologies, Inc.  All rights reserved
 */

package com.sailpoint.audit.rest;

import com.sailpoint.atlas.idn.RestClientProvider;
import com.sailpoint.atlas.idn.ServiceNames;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.audit.event.util.ResourceUtils;
import com.sailpoint.audit.service.AuditEventReportingService;
import com.sailpoint.audit.service.AuditReportService;
import com.sailpoint.audit.service.model.AuditReportRequest;
import com.sailpoint.audit.service.model.AuditReportResponse;
import com.sailpoint.audit.util.AuditEventSearchQueryUtil;
import com.sailpoint.mantis.core.service.CrudService;
import com.sailpoint.mantis.platform.rest.ListResult;
import com.sailpoint.mantisclient.BaseRestClient;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import sailpoint.api.SailPointContext;
import sailpoint.object.AuditEvent;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.QueryOptions;
import sailpoint.tools.GeneralException;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuditEventsResourceTest {

	@Mock
	private AuditEventReportingService _auditEventReportingService;

	@Mock
	private AuditReportService _auditReportService;

	@Mock
	private CrudService _crudService;

	@Mock
	private Iterator<AuditEvent> _iterator;

	@Mock
	private SailPointContext _sailPointContext;

	@Mock
	private AuditEvent _auditEvent;

	@Mock
	private RestClientProvider _restClientProvider;

	@Mock
	private BaseRestClient _baseRestClient;

	@Mock
	private Identity _identity;

	@Mock
	private AuditEventSearchQueryUtil _auditEventSearchQueryUtil;

	@InjectMocks
	private AuditEventsResource _resource;

	@Before
	public void setUp() {
		when(_iterator.hasNext()).thenReturn(Boolean.TRUE, Boolean.FALSE);
		when(_iterator.next()).thenReturn(_auditEvent);
		try {
			when(_auditReportService.getAuditEventsCount(any(QueryOptions.class))).thenReturn(1L);
			when(_sailPointContext.getObjects(anyObject(), anyObject())).thenReturn(Arrays.asList(_auditEvent));
		} catch (GeneralException ignored) {}
		when(_crudService.findAll(eq(AuditEvent.class), any(QueryOptions.class))).thenReturn(_iterator);
		when(_crudService.getContext()).thenReturn(_sailPointContext);

		when(_crudService.findByName(Identity.class, "testUserId")).thenReturn(Optional.of(_identity));
		when(_identity.getAttribute("uid")).thenReturn("999999");

		when(_restClientProvider.getContextRestClient(eq(ServiceNames.SDS))).thenReturn(_baseRestClient);
		when((_baseRestClient.postJson(any(), anyString(), any(), any()))).thenReturn(getAuditEventResponse());

		when(_auditEventSearchQueryUtil.buildSearchQuery(anyString(), anyString())).thenReturn("query");
	}

	private QueryOptions getBaseQueryOptions(){
		QueryOptions queryOptions = new QueryOptions();
		queryOptions.setOrderBy("created");
		queryOptions.setOrderAscending(false);
		queryOptions.add(Filter.and(
				Filter.not(Filter.eq("action", "emailFailure")),
				Filter.not(Filter.eq("action", "import"))
		));
		return queryOptions;
	}


	@Test
	public void testBaseAuditEventList(){
		QueryOptions qOpt = getBaseQueryOptions();
		try {
			ListResult results = _resource.list(null, null, null, 0, null, null, 0, 0, null);
			//verify(_auditReportService).getAuditEventsCount(eq(qOpt));
			Assert.assertEquals(1,results.getCount());
		}catch( GeneralException genEx){
			assertNotNull(genEx);
		}
	}

	@Test
	public void testBaseAuditEventListLimitFlagEnabled(){
		QueryOptions qOpt = getBaseQueryOptions();
		try {
			ListResult results = _resource.list(null, null, null, 0, null,
					null, 0, 0, null);
			//verify(_auditReportService).getAuditEventsCount(eq(qOpt));
			Assert.assertEquals(1,results.getCount());

			results = _resource.list(null, null, null, 0, null, null, 350,
					0, null);

			Assert.assertEquals(1,results.getCount());

			results = _resource.list(null, null, null, 0, null, null, 25,
					0, null);

			Assert.assertEquals(1,results.getCount());
		}catch( GeneralException genEx){
			assertNotNull(genEx);
		}
	}


	@Test
	public void testNormalAuditEventListLimitFlagEnabled(){
		QueryOptions qOpt = getBaseQueryOptions();
		try {

			ListResult results = _resource.list("type","action","application", 7,
					null, "Mark", 10, 0, null);
			//verify(_auditReportService).getAuditEventsCount(eq(qOpt));
			Assert.assertEquals(1,results.getCount());

			results = _resource.list(null, null, null, 0, null, null, 350,
					0, null);

			Assert.assertEquals(1,results.getCount());

			results = _resource.list(null, null, null, 0, null, null, 25,
					0, null);

			Assert.assertEquals(1,results.getCount());
		}catch( GeneralException genEx){
			assertNotNull(genEx);
		}
	}

	@Test
	public void testNormalAuditEventList(){
		QueryOptions qOpt = getBaseQueryOptions();
		try {

			ListResult results = _resource.list("type","action","application", 7, null, "Mark", 10, 0, null);
			//verify(_auditReportService).getAuditEventsCount(eq(qOpt));
			Assert.assertEquals(1,results.getCount());
		}catch( GeneralException genEx){
			assertNotNull(genEx);
		}
	}

	@Test
	public void testIdentityList() {
		Response response = _resource.getAuditEventsForIdentity("testUserId", null);
		Map<String, Object> entity = (Map) response.getEntity();

		assertEquals(1, (int) entity.get("total"));

		response = _resource.getAuditEventsForIdentity("testUserId", "test");
		assertEquals(1, (int) ((Map) response.getEntity()).get("total"));

		response = _resource.getAuditEventsForIdentity("", null);
		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());

		response = _resource.getAuditEventsForIdentity(null, null);
		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());

		response = _resource.getAuditEventsForIdentity("*", null);
		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());

		List<Map> auditEvents = getAuditEventResponse();
		auditEvents.get(0).remove("attributes");
		when((_baseRestClient.postJson(any(), anyString(), any(), any()))).thenReturn(auditEvents);
		response = _resource.getAuditEventsForIdentity("testUserId", "test");
		assertEquals(1, (int) ((Map) response.getEntity()).get("total"));

		when((_baseRestClient.postJson(any(), anyString(), any(), any()))).thenReturn(null);
		response = _resource.getAuditEventsForIdentity("testUserId", null);
		assertEquals(0, (int) ((Map) response.getEntity()).get("total"));
	}

	@Test
	public void testAuditEventReports() {
		AuditReportRequest reportRequest = new AuditReportRequest();
		reportRequest.setUserId("testUserId");
		reportRequest.setSearchText("export");
		reportRequest.setDays(7);

		ResourceUtils resourceUtils = new ResourceUtils();
		AuditReportResponse auditReportResponseSample = resourceUtils.loadResource(AuditReportResponse.class, "reports/rde-audit-activity-report-response.json", "audit event activity");
		when(_auditEventReportingService.generateReport(any())).thenReturn(auditReportResponseSample);
		Response auditEventReports = _resource.getAuditEventReports(reportRequest);
		
		AuditReportResponse auditReportResponseActual =  (AuditReportResponse)auditEventReports.getEntity();
		assertNotNull(auditReportResponseActual.getId());
	}

	@Test
	public void testAuditEventErrorsOnInvalidRequest() {
		AuditReportRequest reportRequest = new AuditReportRequest();
		Response auditEventReports = _resource.getAuditEventReports(reportRequest);
		assertEquals(auditEventReports.getStatus(), HttpStatus.SC_BAD_REQUEST);
	}

	@Test
	public void testAuditEventErrorsOnNullRequest() {
		Response auditEventReports = _resource.getAuditEventReports(null);
		assertEquals(auditEventReports.getStatus(), HttpStatus.SC_BAD_REQUEST);
	}

	private List<Map> getAuditEventResponse() {
		List<Map> auditList = new ArrayList<>();
		Map<String, Object> audit = new LinkedHashMap<>();
		audit.put("id", "1234");
		audit.put("created", String.valueOf((new Date())));
		audit.put("name", "Test Audit Event");
		audit.put("ipAddress", "Test Ip Address");

		Map<String, String> simpleIdentity = new HashMap<>();
		simpleIdentity.put("name", "testIdentity");

		audit.put("actor", simpleIdentity);
		audit.put("target", simpleIdentity);

		Map<String, String> attributes = new HashMap<>();
		attributes.put("sourceName", "testSourceName");
		audit.put("attributes", attributes);

		auditList.add(audit);
		auditList.add(null);

		return auditList;
	}
}
