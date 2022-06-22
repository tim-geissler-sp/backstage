/*
 * Copyright (C) 2017-2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit;

import com.sailpoint.atlas.CachedOrgDataProvider;
import com.sailpoint.atlas.chronicle.AtlasChroniclePlugin;
import com.sailpoint.atlas.event.AtlasEventPlugin;
import com.sailpoint.atlas.health.AtlasHealthPlugin;
import com.sailpoint.atlas.logging.AtlasDynamicLoggingPlugin;
import com.sailpoint.atlas.metrics.AtlasMetricsPlugin;
import com.sailpoint.atlas.profiling.AtlasDynamicProfilingPlugin;
import com.sailpoint.atlas.task.schedule.service.TaskScheduleClientModule;
import com.sailpoint.atlas.tracing.plugin.AtlasTracingPlugin;
import com.sailpoint.atlas.usage.plugin.AtlasUsagePlugin;
import com.sailpoint.audit.event.DomainEventPlugin;
import com.sailpoint.mantis.event.MantisEventHandlerModule;
import com.sailpoint.mantis.platform.MantisApplication;
import com.sailpoint.mantis.platform.PlatformMantisModule;

/**
 * Base Application.
 * Main service for all the audit related parts
 * Audit List and Reports REST endpoints
 * Audit Event handler
 */
public class AuditServerApplication extends MantisApplication {

	public AuditServerApplication() {
		setStack("aer");

		registerPlugin(new AtlasChroniclePlugin());
		registerPlugin(new AtlasEventPlugin());
		registerPlugin(new AtlasHealthPlugin());
		registerPlugin(new AtlasMetricsPlugin());
		registerPlugin(new AtlasUsagePlugin());
		registerPlugin(new AuditEventPlugin());
		registerPlugin(new AuditModulePlugin());
		registerPlugin(new DomainEventPlugin());
		registerPlugin(new AtlasDynamicLoggingPlugin());
		registerPlugin(new AtlasTracingPlugin());
		registerPlugin(new AtlasDynamicProfilingPlugin());

		addServiceModule(new MantisEventHandlerModule());
		addServiceModule(new TaskScheduleClientModule());

		loadModule(new PlatformMantisModule());
	}

	@Override
	protected CachedOrgDataProvider createOrgDataProvider() {
		return new CachedOrgDataProvider(new AuditOrgDataProvider(_config));
	}

	public static void main(String[] args) {
		MantisApplication.run(AuditServerApplication.class, args);
	}
}
