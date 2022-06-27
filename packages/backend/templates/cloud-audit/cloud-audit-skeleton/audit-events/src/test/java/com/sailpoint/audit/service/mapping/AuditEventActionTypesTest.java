/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service.mapping;

import org.junit.Assert;
import org.junit.Test;

public class AuditEventActionTypesTest {

	@Test
	public void testCisAuditReportType() {
		Assert.assertNotNull(AuditEventActionTypes.AuditActionType.values());

		Assert.assertNotNull(AuditEventActionTypes.CISAuditReportType.values());

		Assert.assertNotNull(AuditEventActionTypes.AuditType.values());
	}

}