/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.event;

import com.google.inject.Module;
import com.sailpoint.atlas.event.EventRegistry;
import com.sailpoint.atlas.plugin.PluginConfigurationContext;
import com.sailpoint.atlas.plugin.PluginDeploymentContext;
import com.sailpoint.audit.event.model.EventCatalog;
import com.sailpoint.audit.event.model.EventTemplates;
import com.sailpoint.audit.event.util.ResourceUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DomainEventPluginTest {

	@Mock
	PluginConfigurationContext _configurationContext;

	@Mock
	PluginDeploymentContext _deploymentContext;

	@Mock
	EventRegistry _eventRegistry;

	DomainEventPlugin _plugin;

	@Before
	public void setUp() {
		_plugin = new DomainEventPlugin();
	}

	@Test
	public void configurePlugin() {
		_plugin.configure(_configurationContext);

		verify(_configurationContext, times(1)).addGuiceModule(any(Module.class));
	}

	@Test
	public void deploy() {
		ResourceUtils resourceUtils = new ResourceUtils();
		EventCatalog eventCatalog = new EventCatalog(resourceUtils, new EventTemplates(resourceUtils));
		when(_deploymentContext.getInstance(eq(EventCatalog.class))).thenReturn(eventCatalog);
		when(_deploymentContext.getInstance(eq(EventRegistry.class))).thenReturn(_eventRegistry);

		_plugin.deploy(_deploymentContext);

		eventCatalog.stream().forEach(eventDescriptor -> {
			verify(_eventRegistry).register(eq(eventDescriptor.getTopic()), eq(eventDescriptor.getEventType()), eq(DomainEventHandler.class));
		});
	}
}
