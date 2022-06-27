/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.event;

import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.audit.service.AuditKafkaEventType;
import com.sailpoint.audit.service.FeatureFlags;
import com.sailpoint.audit.service.FirehoseService;
import com.sailpoint.audit.utils.TestUtils;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.PodTopic;
import com.sailpoint.iris.server.EventHandlerContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuditEventS3HandlerTest {

	@Mock
	FirehoseService _firehoseService;

	@Mock
	FeatureFlagService _featureFlagService;

	@Mock
	private EventHandlerContext _eventHandlerContext;

	AuditEventS3Handler _sut;

	@Before
	public void setup() {
		RequestContext requestContext = TestUtils.setDummyRequestContext();

		when(_eventHandlerContext.getTopic()).thenReturn(new PodTopic(IdnTopic.AUDIT.getName(), requestContext.getPod()));
		when(_eventHandlerContext.getEvent()).thenReturn(new Event(AuditKafkaEventType.AUDIT_WHITELISTED.toString(), Collections.emptyMap(),
				"{\"org\":\"acme-solar\",\"pod\":\"dev\",\"created\":\"2019-11-19T16:50:35.676Z\"," +
						"\"id\":\"afd03965-5723-453c-b0ac-536cac77e13d\",\"action\":\"USER_STEP_UP_AUTH\"," +
						"\"type\":\"USER_MANAGEMENT\",\"actor\":{\"name\":\"support\"},\"target\":{\"name\":\"support\"},\"" +
						"stack\":\"CC\",\"ipAddress\":\"207.189.160.255\"," +
						"\"attributes\":{\"sourceName\":\"System\",\"info\":\"KBA\"}," +
						"\"objects\":[\"USER\",\"AUTHENTICATION\",\"STEP_UP\"],\"operation\":\"SETUP\"," +
						"\"status\":\"PASSED\",\"technicalName\":\"USER_AUTHENTICATION_STEP_UP_SETUP_PASSED\"," +
						"\"name\":\"Setup User Authentication Step_up Passed\"}"));

		_sut = new AuditEventS3Handler();
		_sut._featureFlagService = _featureFlagService;
		_sut._firehoseService = _firehoseService;
	}

	@Test
	public void testFeatureEnabled() {
		when(_featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, false)).thenReturn(true);
		try {
			_sut.handleEvent(_eventHandlerContext);
			verify(_firehoseService, times(1))
					.sendToFirehose(any(com.sailpoint.atlas.search.model.event.Event.class));
		} catch (Exception e) {}
	}

	@Test
	public void testFeatureDisabled() {
		when(_featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, false)).thenReturn(false);
		try {
			_sut.handleEvent(_eventHandlerContext);
			verify(_firehoseService, times(0))
					.sendToFirehose(any(com.sailpoint.atlas.search.model.event.Event.class));
		} catch (Exception e) {}
	}

}
