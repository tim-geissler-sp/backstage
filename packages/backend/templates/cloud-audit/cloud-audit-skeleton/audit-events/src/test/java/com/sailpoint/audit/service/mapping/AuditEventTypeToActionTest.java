/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service.mapping;

import com.sailpoint.audit.AuditEventConstants;
import com.sailpoint.mantis.core.service.model.AuditEventActions;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Test class for AuditEventTypeToAction.
 * Tests involving this class has been covered in AuditReportServiceTest. Since, it is in a different module,
 * coverage for this is not accounted.
 */
public class AuditEventTypeToActionTest {

	@Test
	public void testPasswordChangeList() {
		List<String> passwordChangeEventList = AuditEventConstants.PASSWORD_ACTIVITY_ACTION_LIST;
		Assert.assertNotNull(passwordChangeEventList);
		Assert.assertTrue(passwordChangeEventList.contains("USER_PASSWORD_UPDATE_PASSED"));
	}

	@Test
	public void testAccessRequestList() {
		List<String> accessRequestList = AuditEventConstants.ACCESS_REQUEST_ACTION_LIST;
		Assert.assertNotNull(accessRequestList);
		Assert.assertTrue(accessRequestList.contains("escalate"));
	}

	@Test
	public void testProvisioningList() {
		List<String> provisioningList = AuditEventConstants.PROVISIONING_ACTION_LIST;
		Assert.assertNotNull(provisioningList);
		Assert.assertTrue(provisioningList.contains("App Request Rejected"));
	}

	@Test
	public void testSourceList() {
		List<String> sourceList = AuditEventConstants.SOURCE_MANAGEMENT_ACTION_LIST;
		Assert.assertNotNull(sourceList);
		Assert.assertTrue(sourceList.contains(AuditEventActions.SOURCE_CREATE));
	}

	@Test
	public void testServiceDeskIntegration() {
		List<String> sourceList = AuditEventConstants.SOURCE_MANAGEMENT_ACTION_LIST;
		Assert.assertNotNull(sourceList);
		Assert.assertTrue(sourceList.contains(AuditEventActions.SERVICE_DESK_INTEGRATION_CREATED));
		Assert.assertTrue(sourceList.contains(AuditEventActions.SERVICE_DESK_INTEGRATION_CREATE_FAILED));
		Assert.assertTrue(sourceList.contains(AuditEventActions.SERVICE_DESK_INTEGRATION_UPDATED));
		Assert.assertTrue(sourceList.contains(AuditEventActions.SERVICE_DESK_INTEGRATION_UPDATE_FAILED));
		Assert.assertTrue(sourceList.contains(AuditEventActions.SERVICE_DESK_INTEGRATION_DELETED));
		Assert.assertTrue(sourceList.contains(AuditEventActions.SERVICE_DESK_INTEGRATION_DELETE_FAILED));
	}

}
