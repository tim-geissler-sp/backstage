/*
 * Copyright (C) 2020. SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.service;

import com.sailpoint.audit.service.mapping.AuditEventActionTypes;
import com.sailpoint.audit.service.model.AuditReportDetails;
import com.sailpoint.audit.service.model.ReportDTO;
import com.sailpoint.audit.utils.TestUtils;
import com.sailpoint.mantis.core.service.CachedConfigService;
import com.sailpoint.mantis.core.service.CrudService;
import com.sailpoint.mantis.core.service.model.AuditEventActions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sailpoint.object.Attributes;
import sailpoint.object.AuditEvent;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.QueryOptions;
import sailpoint.tools.GeneralException;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuditReportService
 */
public class AuditReportServiceTest {

	@Mock
	CrudService _crudService;

	@Mock
	CachedConfigService _configService;

	@Mock
	Identity _identity;

	@Mock
	Identity _identity2;

	@Mock
	RDEServiceClient _rdeService;

	@Mock
	ReportDTO _reportDTO_all;

	@Mock
	AuditReportDetails _auditReportDetails;

	AuditReportService _reportsService;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		TestUtils.setDummyRequestContext();

		_reportsService = new AuditReportService();
		_reportsService._rdeService  = _rdeService;
		_reportsService._crudService = _crudService;
		_reportsService._configService = _configService;

		when(_identity.getAttribute("uid")).thenReturn("999999");
		when(_identity2.getAttribute("uid")).thenReturn(null);

		when(_rdeService.reportResult(eq("audit-type-report-all"), any())).thenReturn(getReportDTO(AuditReportService.ReportPrefix.AUDIT_TYPE_REPORT.toString() , "all"));
		when(_rdeService.reportResult(eq("audit-type-report-auth"), any())).thenReturn(getReportDTO(AuditReportService.ReportPrefix.AUDIT_TYPE_REPORT.toString() , "auth"));
		when(_rdeService.reportResult(eq("audit-type-report-sso"), any())).thenReturn(getReportDTO(AuditReportService.ReportPrefix.AUDIT_TYPE_REPORT.toString() , "sso"));
		when(_rdeService.reportResult(eq("audit-type-report-provisioning"), any())).thenReturn(getReportDTO(AuditReportService.ReportPrefix.AUDIT_TYPE_REPORT.toString() , "provisioning"));
		when(_rdeService.reportResult(eq("audit-type-report-password_change"), any())).thenReturn(getReportDTO(AuditReportService.ReportPrefix.AUDIT_TYPE_REPORT.toString() , "password_change"));
		when(_rdeService.reportResult(eq("audit-type-report-source"), any())).thenReturn(getReportDTO(AuditReportService.ReportPrefix.AUDIT_TYPE_REPORT.toString() , "source"));
		when(_rdeService.reportResult(eq("audit-type-report-access_request"), any())).thenReturn(getReportDTO(AuditReportService.ReportPrefix.AUDIT_TYPE_REPORT.toString() , "access_request"));

		when(_rdeService.reportResult(eq("audit-user-report-mark"), any())).thenReturn(getReportDTO(AuditReportService.ReportPrefix.AUDIT_USER_REPORT.toString() , "mark"));
		when(_rdeService.reportResult(eq("audit-user-report-bill"), any())).thenReturn(getReportDTO(AuditReportService.ReportPrefix.AUDIT_USER_REPORT.toString() , "bill"));
		when(_rdeService.reportResult(eq("audit-user-report-jane"), any())).thenReturn(getReportDTO(AuditReportService.ReportPrefix.AUDIT_USER_REPORT.toString() , "jane"));


		when(_crudService.count(AuditEvent.class,new QueryOptions())).thenReturn(20);
		when(_crudService.findByName(Identity.class, "Mark")).thenReturn(Optional.of(_identity));
		when(_crudService.findByName(Identity.class, "Jane")).thenReturn(Optional.of(_identity2));

		when(_auditReportDetails.getId()).thenReturn("1234");
		when(_auditReportDetails.getStatus()).thenReturn("Success");
		when(_auditReportDetails.getDate()).thenReturn(new Date());

		when(_configService.getString("orgDataS3Bucket")).thenReturn("bucket");
	}

	private ReportDTO getReportDTO(String prefix, String name){
		ReportDTO _report = new ReportDTO();
		_report.setName(name);
		_report.setTaskDefName(prefix + name);
		_report.setReportName(prefix + name);
		_report.setId("1234");
		return _report;
	}

	@Test
	public void auditEventsCountTest(){
		try {

			long count = _reportsService.getAuditEventsCount(new QueryOptions());
			Assert.assertEquals(20,count);
		}
		catch( GeneralException genEx ){
			Assert.assertNull(genEx);
		}
		try {
			long count = _reportsService.getAuditEventsCount(null);
			Assert.assertEquals(20,count);
		}
		catch( GeneralException genEx ){
			Assert.assertNull(genEx);
		}
	}


	@Test
	public void buildFiltersTypesTest() {
		Filter filter;
		/**
		 * byType
		 */
		filter = _reportsService.buildFilterByType("foo");
		Assert.assertNull(filter);
		filter = _reportsService.buildFilterByType("AUTH");
		Assert.assertTrue(filter.toString().equals("instance == \"AUTH\""));
		filter = _reportsService.buildFilterByType(AuditEventActionTypes.AuditActionType.SSO);
		Assert.assertTrue(filter.toString().equals("instance == \"SSO\""));

		filter = _reportsService.buildFilterByType(AuditEventActionTypes.AuditType.ACCESS_REQUEST.toString());
		Assert.assertTrue(filter.toString().contains("AccessRequest"));

		filter = _reportsService.buildFilterByType(AuditEventActionTypes.CISAuditReportType.SOURCE.toString());
		Assert.assertTrue(filter.toString().contains(AuditEventActions.SOURCE_UPDATE));

		filter = _reportsService.buildFilterByType(AuditEventActionTypes.CISAuditReportType.PASSWORD_CHANGE.toString());
		Assert.assertTrue(filter.toString().contains("USER_PASSWORD_UPDATE_PASSED"));

		filter = _reportsService.buildFilterByType(AuditEventActionTypes.AuditType.PROVISIONING.toString());
		Assert.assertTrue(filter.toString().contains(AuditEvent.ManualChange));

		filter = null;
		for (AuditEventActionTypes.CISAuditReportType type : AuditEventActionTypes.CISAuditReportType.values()) {
			filter = _reportsService.buildFilterByType(type.toString());
			Assert.assertNotNull(filter);
		}

	}

	@Test
	public void buildFiltersTypesTestForSearch() {
		Filter filter;
		/**
		 * byType
		 */
		filter = _reportsService.buildFilterByTypeForSearch("foo");
		Assert.assertNull(filter);

		filter = _reportsService.buildFilterByTypeForSearch(AuditEventActionTypes.AuditType.USER_MANAGEMENT.toString());
		Assert.assertTrue(filter.toString().contains("USER_UNLOCK"));

		filter = _reportsService.buildFilterByTypeForSearch(AuditEventActionTypes.AuditType.CERTIFICATION.toString());
		Assert.assertTrue(filter.toString().contains("CERT_CAMPAIGN_COMPLETE"));
		filter = _reportsService.buildFilterByTypeForSearch(AuditEventActionTypes.AuditType.PROVISIONING.toString());
		Assert.assertTrue(filter.toString().contains(AuditEvent.ManualChange));

		filter = null;
		for (AuditEventActionTypes.AuditType type : AuditEventActionTypes.AuditType.values()) {
			filter = _reportsService.buildFilterByTypeForSearch(type.toString());
			Assert.assertNotNull(filter);
		}

	}

	@Test
	public void buildFiltersActionTest(){
		Filter filter = _reportsService.buildFilterByAction("barring");
		Assert.assertTrue(filter.toString().equals("action == \"barring\""));

	}

	@Test
	public void buildFiltersApplicationTest(){
		Filter filter = _reportsService.buildFilterByApplication("foo baz");
		Assert.assertTrue(filter.toString().equals("application.contains(\"foo baz\")"));
		
		filter = _reportsService.buildFilterByApplication("[CC] baz");
		Assert.assertTrue(filter.toString().equals("application.contains(\"baz\")"));
	}

	@Test
	public void buildFilterByDaysTest(){

		Filter filter = _reportsService.buildFilterByDays(0);
		Assert.assertNotNull(filter);
		Assert.assertTrue(filter.toString().contains("created >= DATE$"));
		filter = _reportsService.buildFilterByDays(200);
		Assert.assertTrue(filter.toString().contains("created >= DATE$"));
	}

	@Test
	public void buildFilterBySinceDateTest(){

		Filter filter = _reportsService.buildFilterSinceDate("2017-02-14T12:01:34Z");
		Assert.assertTrue(filter.toString().contains("created >= DATE$"));
		filter = _reportsService.buildFilterSinceDate("2017-02-14");
		Assert.assertTrue(filter.toString().contains("created >= DATE$"));
		filter = _reportsService.buildFilterSinceDate("2117-02-14"); // the future?
		Assert.assertTrue(filter.toString().contains("created >= DATE$"));
	}

	@Test
	public void buildFiltersUserTest(){
		Filter filter = null ;

		filter = _reportsService.buildFilterByUser("Mark");
		Assert.assertTrue(filter.toString().contains("Mark"));   // username
		Assert.assertTrue(filter.toString().contains("999999")); // uid

		filter = null;
		filter = _reportsService.buildFilterByUser("Jane");
		Assert.assertTrue(filter.toString().contains("Jane"));   // username

	}

	@Test
	public void listTypeReportsTest(){
		try {
			int numberOfDays = 7;
			List<AuditReportDetails> details = _reportsService.listTypeReports(numberOfDays);
			Assert.assertEquals( 7, details.size() );
			for (AuditReportDetails detail:details ) {
				Assert.assertTrue(detail.getReportName().equals(AuditReportService.ReportPrefix.AUDIT_TYPE_REPORT + detail.getName()));
				Assert.assertEquals(numberOfDays, detail.getAttributes().get("numDays"));
				Assert.assertEquals("by type", detail.getType());
				Attributes<String, Object> attr = detail.getAttributes();
				if( attr.containsKey("types") ){
					Assert.assertNotNull(attr.get("types"));
					Assert.assertTrue(attr.get("types") instanceof List);
				}
				if( attr.containsKey("actions") ){
					Assert.assertNotNull(attr.get("actions"));
					Assert.assertTrue(attr.get("actions") instanceof List);
				}
			}
		}
		catch(GeneralException genEx){
			System.out.println("List Type Reports Failed : " + genEx.getMessage());
		}
	}

	@Test
	public void getUserReportsTest(){
		try{
			String user = "Mark";
			List<AuditReportDetails> details = _reportsService.getUserReport(7,user);
			Assert.assertEquals(1, details.size());
			Assert.assertTrue(details.get(0).getName().equals(user.toLowerCase()));
			Assert.assertTrue(details.get(0).getReportName().equals(AuditReportService.ReportPrefix.AUDIT_USER_REPORT + user.toLowerCase()));

		}
		catch(GeneralException genEx){
			System.out.println("get User Reports Failed : " + genEx.getMessage());
		}

		try{
			String user = "Bill";
			List<AuditReportDetails> details = _reportsService.getUserReport(7,user);
			Assert.assertEquals(1, details.size());
			Assert.assertTrue(details.get(0).getName().equals(user.toLowerCase()));
			Assert.assertTrue(details.get(0).getReportName().equals(AuditReportService.ReportPrefix.AUDIT_USER_REPORT + user.toLowerCase()));

		}
		catch(GeneralException genEx){
			System.out.println("get User Reports Failed : " + genEx.getMessage());
		}
	}

}
