/*
 * Copyright (C) 2020. SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.service.model;

import com.sailpoint.audit.service.AuditReportService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sailpoint.object.Attributes;
import sailpoint.object.TaskResult;

import java.util.Date;

import static org.mockito.Mockito.when;

/**
 * Created by mark.boyle on 4/7/17.
 */
public class AuditReportDetailsTest {

	@Mock
	ReportDTO _reportDTO;

	@Mock
	Attributes<String, Object> _attributes;

	AuditReportDetails _auditReportDetails;

	Date testDate = new Date();
	Long reportRowCount = 6L;
	Long reportDuration = 60000L;
	String name = "audit_test";

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		when(_reportDTO.getDate()).thenReturn(testDate.getTime());
		when(_reportDTO.getDuration()).thenReturn( reportDuration );
		when(_reportDTO.getRows()).thenReturn(reportRowCount);
		when(_reportDTO.getId()).thenReturn("A1234");
		when(_reportDTO.getName()).thenReturn(name);
		when(_reportDTO.getStatus()).thenReturn(TaskResult.CompletionStatus.Success.toString());

		when(_attributes.get("numDays")).thenReturn(7);
	}

	@Test
	public void constructorTypesTest() {
		// simulate the call in getReportModel
		String auditName = AuditReportService.ReportPrefix.AUDIT_TYPE_REPORT + name;
		when(_reportDTO.getReportName()).thenReturn(auditName);
		//
		_auditReportDetails = new AuditReportDetails(_reportDTO, _attributes);
		Assert.assertNotNull(_auditReportDetails);
		Assert.assertEquals("by type",_auditReportDetails.getType());

		Assert.assertEquals((long)reportRowCount, (long)_auditReportDetails.getRows());
		Assert.assertEquals("A1234", _auditReportDetails.getId());
		Assert.assertEquals((long)reportDuration , (long)_auditReportDetails.getDuration());
		Assert.assertEquals(testDate, _auditReportDetails.getDate() );
		Assert.assertEquals(_attributes, _auditReportDetails.getAttributes() );
		Assert.assertEquals( auditName, _auditReportDetails.getReportName() );
		Assert.assertEquals(name, _auditReportDetails.getName());
		Assert.assertEquals("Success", _auditReportDetails.getStatus());

	}
	@Test
	public void constructorUserTest() {
		// simulate the call in getReportModel
		String auditName = AuditReportService.ReportPrefix.AUDIT_USER_REPORT + name;
		when(_reportDTO.getReportName()).thenReturn(auditName);
		//
		_auditReportDetails = new AuditReportDetails(_reportDTO, _attributes);
		Assert.assertNotNull(_auditReportDetails);
		Assert.assertEquals("by user",_auditReportDetails.getType());

	}
	@Test
	public void constructorUnknownTest() {
		String auditName = name;
		when(_reportDTO.getReportName()).thenReturn(auditName);
		//
		_auditReportDetails = new AuditReportDetails(_reportDTO, _attributes);
		Assert.assertNotNull(_auditReportDetails);
		Assert.assertEquals("unknown",_auditReportDetails.getType());

	}
	@Test
	public void constructorNoReportName(){
		// From path in run report
		String auditName = name;
		when(_reportDTO.getReportName()).thenReturn(null);
		// pass in null reportName it should be replaced by name.
		_auditReportDetails = new AuditReportDetails(_reportDTO, _attributes);
		Assert.assertNotNull(_auditReportDetails);
		Assert.assertEquals(_auditReportDetails.getName(), _auditReportDetails.getReportName());
	}

}
