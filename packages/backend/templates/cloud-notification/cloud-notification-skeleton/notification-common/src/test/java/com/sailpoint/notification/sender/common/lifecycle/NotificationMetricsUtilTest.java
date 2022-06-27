/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.lifecycle;

import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.event.AtlasEventPlugin;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.client.GlobalTopic;
import com.sailpoint.iris.server.EventHandlerContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.when;

/**
 * Test class for testing helper class NotificationMetricsUtil
 */
public class NotificationMetricsUtilTest {

	@Mock
	AtlasConfig _atlasConfig;

	@Mock
	EventHandlerContext _context;

	@Mock
	Event _event;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		when(_atlasConfig.isProduction()).thenReturn(true);

		when(_event.getHeader(EventHeaders.POD))
				.thenReturn(Optional.of("echo"));
		when(_event.getHeader(EventHeaders.ORG))
				.thenReturn(Optional.of("acme_solar"));
		when(_event.getHeader(EventHeaders.ATTEMPT_NUMBER))
				.thenReturn(Optional.empty());
		when(_event.getHeader(EventHeaders.GROUP_ID))
				.thenReturn(Optional.of("hermes"));
		when(_event.getType())
				.thenReturn("NOTIFICATION");
		when(_context.getTopic())
				.thenReturn(new GlobalTopic(IdnTopic.NOTIFICATION.getName()));
		when(_context.getEvent()).thenReturn(_event);
	}

	@Test
	public void metricsUtilTestProd() {
		whenProduction();
		NotificationMetricsUtil metricsUtil = new NotificationMetricsUtil(_atlasConfig);
		Map<String, String> tags = metricsUtil.getTags(_context, Optional.empty());

		Assert.assertEquals("echo", tags.get(EventHeaders.POD));
		Assert.assertEquals("acme_solar", tags.get(EventHeaders.ORG));
		Assert.assertNull(tags.get(EventHeaders.ATTEMPT_NUMBER));
		Assert.assertEquals("hermes", tags.get(EventHeaders.GROUP_ID));
		Assert.assertEquals("NOTIFICATION", tags.get("eventType"));
		Assert.assertEquals("sp_prod", tags.get("env"));
		Assert.assertNull(tags.get("exception_class"));
	}

	@Test
	public void metricsUtilTestDev() {
		whenDev();
		NotificationMetricsUtil metricsUtil = new NotificationMetricsUtil(_atlasConfig);
		Map<String, String> tags = metricsUtil.getTags(_context, Optional.of("AWS Error"));

		Assert.assertEquals("echo", tags.get(EventHeaders.POD));
		Assert.assertEquals("acme_solar", tags.get(EventHeaders.ORG));
		Assert.assertNull(tags.get(EventHeaders.ATTEMPT_NUMBER));
		Assert.assertEquals("hermes", tags.get(EventHeaders.GROUP_ID));
		Assert.assertEquals("NOTIFICATION", tags.get("eventType"));
		Assert.assertEquals("sp_dev", tags.get("env"));
		Assert.assertEquals("AWS Error", tags.get("exception_class"));
	}

	private void whenProduction() {
		when(_atlasConfig.getBoolean(AtlasEventPlugin.ATLAS_IRIS_CONFIG_SERVER_IS_IN_PRODUCTION, false))
				.thenReturn(true);
	}
	private void whenDev() {
		when(_atlasConfig.getBoolean(AtlasEventPlugin.ATLAS_IRIS_CONFIG_SERVER_IS_IN_PRODUCTION, false))
				.thenReturn(false);
	}
}
