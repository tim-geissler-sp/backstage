/*
 * Copyright (C) 2017-19 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit;

import com.sailpoint.atlas.event.AtlasEventPlugin;
import com.sailpoint.atlas.health.AtlasHealthPlugin;
import com.sailpoint.atlas.metrics.AtlasMetricsPlugin;
import com.sailpoint.audit.service.model.AuditReportDetails;
import com.sailpoint.mantis.core.service.AuditorFactory;
import com.sailpoint.mantis.core.service.CrudService;
import com.sailpoint.mantis.core.service.SailPointAuditorFactory;
import com.sailpoint.mantis.core.service.model.AuditEventActions;
import com.sailpoint.mantis.test.integration.IntegrationTestApplication;
import com.sailpoint.mantis.test.integration.MantisIntegrationTest;
import com.sailpoint.mantisclient.Params;
import com.sailpoint.utilities.JsonParameterizedType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sailpoint.object.AuditEvent;

import java.util.List;

import static com.sailpoint.atlas.idn.ServiceNames.RDE;

public class AuditModulePluginTest extends MantisIntegrationTest {

	@Before
	public void setup() {
		insertAuditEvent();

		stubService(RDE, "POST", "reporting/reports/result", "{}");
		stubService(RDE, "POST", "reporting/reports/run", "{\"name\":\"\"}");
	}

	@Override
	protected IntegrationTestApplication createMantisApplication(){
		return new IntegrationTestApplication(){{
			registerPlugin(new AuditModulePlugin());
			registerPlugin(new AtlasEventPlugin());
			registerPlugin(new AtlasHealthPlugin());
			registerPlugin(new AtlasMetricsPlugin());
		}};
	}

	@Test
	public void testAuditReportsList() {
		Params params = new Params();
		params.query("days", 7 );
		params.query("reportType", "types");

		List<AuditReportDetails> results = _mantisClient.getJson(
				new JsonParameterizedType<>(List.class, AuditReportDetails.class),
				"/audit/auditReports/list/types", params);

		Assert.assertNotNull(results);
		Assert.assertEquals(results.size(), 7);
	}

	private void insertAuditEvent() {
		runWithContext(context -> {
			CrudService crudService = new CrudService(() -> context);
			AuditorFactory auditorFactory = new SailPointAuditorFactory();

			AuditEvent auditEvent = new AuditEvent();
			auditEvent.setAction(AuditEventActions.ACTION_STATE_CHANGE);
			auditEvent.setTarget("jon.lees");
			auditEvent.setSource("bx (1.90.4.2)");
			auditEvent.setInstance("SSO");
			auditEvent.setApplication("[CC] MLB [src-111]");
			auditEvent.setString1("localhost");
			auditEvent.setString2("127.0.0.1");
			auditEvent.setString3("7c5ac42af980344b01");
			auditEvent.setString4("NONE");

			auditorFactory.log(auditEvent);
			crudService.commit();
			return true;
		});
	}

}
