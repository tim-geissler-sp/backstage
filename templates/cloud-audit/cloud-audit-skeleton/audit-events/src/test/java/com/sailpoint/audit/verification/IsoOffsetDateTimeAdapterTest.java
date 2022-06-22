/*
 * Copyright (C) 2022 SailPoint Technologies, Inc.  All rights reserved.
 */

package com.sailpoint.audit.verification;

import com.google.gson.annotations.JsonAdapter;
import com.sailpoint.utilities.JsonUtil;
import org.junit.Assert;
import org.junit.Test;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;

public class IsoOffsetDateTimeAdapterTest {

	public static final String AUDIT_ID = "whatever";
	public static final String AUDIT_POD = "some pod";
	public static final String AUDIT_ORG = "some org";
	public static final String AUDIT_TENANT_ID = "tenant id";
	public static final Date AUDIT_CREATED = new Date();
	public static final ArrayList<AuditVerificationRequest.VerificationTarget> AUDIT_VERIFY_IN = new ArrayList<>();

	@Test
	public void testSerializedDateFormat() {
		Date d = new Date();
		DateTimeFormatter df = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
		String expectedDateString = df.format(d.toInstant().atZone(ZoneId.of("UTC")));
		String expectedJson = "{\"date\":\"" + expectedDateString + "\"}";
		DummyClass mock = new DummyClass(d);
		String json = JsonUtil.toJson(mock);
		Assert.assertEquals(expectedJson, json);
	}

	@Test
	public void testJSONSerializeAuditVerificationRequest() {
		// AuditVerificationRequest contains a field annotated with IsoOffsetDateTimeAdapter
		// so serialize to json and re-hydrate to indicate that the back and forth work
		String json = JsonUtil.toJson(AuditVerificationRequest.builder()
			.id(AUDIT_ID)
			.pod(AUDIT_POD)
			.org(AUDIT_ORG)
			.tenantId(AUDIT_TENANT_ID)
			.verifyIn(AUDIT_VERIFY_IN)
			.created(AUDIT_CREATED)
			.build());

		AuditVerificationRequest request = JsonUtil.parse(AuditVerificationRequest.class, json);
		Assert.assertEquals(AUDIT_ID, request.getId());
		Assert.assertEquals(AUDIT_POD, request.getPod());
		Assert.assertEquals(AUDIT_ORG, request.getOrg());
		Assert.assertEquals(AUDIT_CREATED, request.getCreated());
		Assert.assertEquals(AUDIT_VERIFY_IN.size(), request.getVerifyIn().size());
		Assert.assertTrue(request.getVerifyIn().containsAll(AUDIT_VERIFY_IN));
	}

	class DummyClass {
		@JsonAdapter(IsoOffsetDateTimeAdapter.class)
		public Date date;

		public DummyClass(Date date) {
			this.date = date;
		}
	}
}
