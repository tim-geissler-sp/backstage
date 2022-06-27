/*
 * Copyright (c) 2017. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit;

import com.sailpoint.atlas.event.AtlasEventPlugin;
import com.sailpoint.atlas.health.AtlasHealthPlugin;
import com.sailpoint.atlas.task.schedule.service.TaskScheduleClientModule;
import com.sailpoint.mantis.event.MantisEventHandlerModule;
import com.sailpoint.mantis.test.integration.IntegrationTestApplication;
import com.sailpoint.mantis.test.integration.MantisIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class AuditEventPluginTest extends MantisIntegrationTest {
	@Override
	protected IntegrationTestApplication createMantisApplication(){

		return new IntegrationTestApplication(){{
			registerPlugin(new AtlasEventPlugin());
			registerPlugin(new AtlasHealthPlugin());

			registerPlugin(new AuditEventPlugin());

			addServiceModule(new MantisEventHandlerModule());
			addServiceModule(new TaskScheduleClientModule());
		}};
	}

	@Test
	public void test() {
		// Test is the loading of the module.
		Assert.assertNull(null);
	}


}
