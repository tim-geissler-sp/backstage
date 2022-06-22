/*
 * Copyright (C) 2020. SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.rest;

import com.sailpoint.audit.service.AuditReportService;
import com.sailpoint.audit.service.model.AuditReportDetails;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sailpoint.tools.GeneralException;

import java.util.List;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for AuditReportsResource .
 *  /audit/auditReports/list/{types}
 */
public class AuditReportsResourceTest {

	@Mock
	AuditReportService _auditReportService;

	@Mock
	AuditReportDetails _auditReportDetails;

	AuditReportsResource _resource;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		_resource = new AuditReportsResource();
		_resource._auditService = _auditReportService;

	}
	@Test
	public void listFailureTest() {
		try {
			List<AuditReportDetails> _details = _resource.list(7, "support", "no_foos");
		}
		catch (GeneralException genException) {
			System.out.println("\"no_foos\" not found : expected");
			Assert.assertNotNull(genException);
		}
	}
	@Test
	public void listTypesTest() {
		try {
			List<AuditReportDetails> _details = _resource.list(7, null, "types");
			verify(_auditReportService).listTypeReports(eq(7));
		}
		catch (GeneralException genException) {
			Assert.assertNull(genException);
		}
	}

	@Test
	public void listUserTest() {
		try {
			List<AuditReportDetails> _details = _resource.list(7, "support", "user");
			verify(_auditReportService).getUserReport(eq(7), eq("support"));
		}
		catch (GeneralException genException) {
			Assert.assertNull(genException);
		}
	}

}
