/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.interest.matcher.event;

import com.google.inject.Provider;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.client.OrgTopic;
import com.sailpoint.iris.client.Topic;
import com.sailpoint.iris.client.TopicDescriptor;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.interest.matcher.interest.Interest;
import com.sailpoint.notification.interest.matcher.repository.InterestRepository;
import com.sailpoint.notification.sender.common.event.interest.matching.dto.NotificationInterestMatched;
import com.sailpoint.notification.interest.matcher.repository.impl.json.InterestRepositoryJsonImpl;
import com.sailpoint.notification.interest.matcher.repository.impl.json.InterestRepositoryJsonImplTest;
import com.sailpoint.notification.interest.matcher.service.InterestMatcherDebugService;
import com.sailpoint.notification.sender.common.test.TestUtil;
import com.sailpoint.notification.sender.slack.service.SlackService;
import com.sailpoint.notification.sender.teams.service.TeamsService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;

import static com.sailpoint.notification.interest.matcher.event.InterestMatcherEventHandler.HERMES_SLACK_NOTIFICATION_ENABLED;
import static com.sailpoint.notification.interest.matcher.event.InterestMatcherEventHandler.HERMES_TEAMS_NOTIFICATION_ENABLED;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InterestMatcherEventHandlerTest {

	@Mock
	InterestMatcherDebugService _interestMatcherDebugService;

	@Mock
	EventHandlerContext _context;

	@Mock
	Provider<EventService> _esProvider;

	@Mock
	EventService _eventService;

	@Mock
	public FeatureFlagService _featureFlagService;

	@Mock
	public TeamsService _teamsService;

	@Mock
	public SlackService _slackService;

	private InterestMatcherEventHandler _interestMatcherEventHandler;
	private InterestRepository _repo;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		Topic topic = new OrgTopic("notification", "dev", "acme-solar");

		when(_context.getTopic())
				.thenReturn(topic);

		when(_esProvider.get())
				.thenReturn(_eventService);

		when(_featureFlagService.getBoolean(HERMES_SLACK_NOTIFICATION_ENABLED, false))
				.thenReturn(true);

		when(_featureFlagService.getBoolean(HERMES_TEAMS_NOTIFICATION_ENABLED, false))
				.thenReturn(true);

		_repo = new InterestRepositoryJsonImpl(AtlasConfig.loadConfig(Collections.EMPTY_MAP));
		_interestMatcherEventHandler = new InterestMatcherEventHandler(_interestMatcherDebugService,
				_repo, _esProvider, _featureFlagService, _teamsService, _slackService);
	}

	@Test
	public void handleEventDebugTest() {
		withDebugEvent();
		Interest interest = _repo.getInterests().stream().filter(i->i.getNotificationKey().equals("approval_request")).findAny().get();
		_interestMatcherEventHandler.setInterest(interest);

		_interestMatcherEventHandler.handleEvent(_context);

		verify(_interestMatcherDebugService, times(1)).writeToStore("testID",
				"314cf125-f892-4b16-bcbb-bfe4afb01f8570e7cde5-3473-46ea-94ea-90bc8c605a6cjames.smithjane.doe");

		verify(_esProvider, times(0)).get();
	}

	@Test
	public void handleEventTest() {
		withEvent("ACCESS_APPROVAL_REQUESTED", InterestRepositoryJsonImplTest.JSON_EVENT);
		Interest interest = _repo.getInterests().stream().filter(i->i.getNotificationKey().equals("approval_request")).findAny().get();
		_interestMatcherEventHandler.setInterest(interest);
		_interestMatcherEventHandler.handleEvent(_context);

		verify(_interestMatcherDebugService, times(0)).writeToStore(any(String.class),
				any(String.class));

		verify(_esProvider, times(2)).get();

		ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<TopicDescriptor> topicDescriptorCaptor = ArgumentCaptor.forClass(TopicDescriptor.class);

		verify(_eventService, times(2)).publish(topicDescriptorCaptor.capture(),
				eventTypeCaptor.capture(), any(NotificationInterestMatched.class));

		Assert.assertEquals(EventType.NOTIFICATION_INTEREST_MATCHED, eventTypeCaptor.getValue());
		Assert.assertEquals(IdnTopic.NOTIFICATION, topicDescriptorCaptor.getValue());
	}

	@Test
	public void handleEventTestWithFilters() {
		Interest approved = _repo.getInterests().stream().filter(i->i.getNotificationKey().equals("access_request_reviewed_approved")).findAny().get();
		Interest denied = _repo.getInterests().stream().filter(i->i.getNotificationKey().equals("access_request_reviewed_denied")).findAny().get();

		withEvent("ACCESS_REQUEST_REVIEWED", InterestRepositoryJsonImplTest.JSON_ACCESS_REQUEST_REVIEWED_EVENT_APPROVED);
		withSlackTenants("acme-solar", "acme-lunar");
		_interestMatcherEventHandler.setInterest(approved);
		_interestMatcherEventHandler.handleEvent(_context);

		ArgumentCaptor<NotificationInterestMatched> contentCaptor = ArgumentCaptor.forClass(NotificationInterestMatched.class);

		verify(_eventService, times(1)).publish(any(),
				any(), contentCaptor.capture());

		Assert.assertEquals("access_request_reviewed_approved", contentCaptor.getValue().getNotificationKey());

		withEvent("ACCESS_REQUEST_REVIEWED", InterestRepositoryJsonImplTest.JSON_ACCESS_REQUEST_REVIEWED_EVENT_DENIED);
		_interestMatcherEventHandler.setInterest(denied);
		_interestMatcherEventHandler.handleEvent(_context);

		verify(_eventService, times(2)).publish(any(),
				any(), contentCaptor.capture());

		Assert.assertEquals("access_request_reviewed_denied", contentCaptor.getValue().getNotificationKey());
	}

	@Test
	public void handleEventTestWhenFilterNotMatch() {
		Interest iac = _repo.getInterests().stream().filter(i->i.getNotificationKey().equals("iac_test")).findAny().get();

		withEvent("IdentityAttributesChangedEvent", InterestRepositoryJsonImplTest.JSON_IDENTITY_ATTRIBUTES_CHANGED_NOISY);
		_interestMatcherEventHandler.setInterest(iac);
		_interestMatcherEventHandler.handleEvent(_context);

		verify(_eventService, times(0)).publish(any(),
				any(), any());
	}

	@Test
	public void handleEventTestTeamsTenantCheckPass() {
		Interest iac = _repo.getInterests().stream().filter(i->i.getNotificationKey().equals("iac_test")).findAny().get();

		withEvent("IdentityAttributesChangedEvent", InterestRepositoryJsonImplTest.JSON_IDENTITY_ATTRIBUTES_CHANGED_EVENT);
		withTeamsTenants("acme-solar", "acme-lunar");
		_interestMatcherEventHandler.setInterest(iac);

		// Handle event twice
		_interestMatcherEventHandler.handleEvent(_context);
		_interestMatcherEventHandler.handleEvent(_context);

		// Verify event service published Interest Matched twice
		verify(_eventService, times(2)).publish(any(),
				any(), any());

		// Verify memoization, i.e. teams service should be called just once
		verify(_teamsService, times(1)).getTeamsTenants();
	}

	@Test
	public void handleEventTestTeamsTenantCheckFail() {
		Interest iac = _repo.getInterests().stream().filter(i->i.getNotificationKey().equals("iac_test")).findAny().get();

		withEvent("IdentityAttributesChangedEvent", InterestRepositoryJsonImplTest.JSON_IDENTITY_ATTRIBUTES_CHANGED_EVENT);
		withTeamsTenants("acme-jovian", "acme-lunar");
		_interestMatcherEventHandler.setInterest(iac);

		// Handle event twice
		_interestMatcherEventHandler.handleEvent(_context);
		_interestMatcherEventHandler.handleEvent(_context);

		// Verify event service did not publish Interest Matched
		verify(_eventService, times(0)).publish(any(),
				any(), any());

		// Verify memoization, i.e. teams service should be called just once
		verify(_teamsService, times(1)).getTeamsTenants();
	}

	@Test
	public void handleEventTestSlackTenantCheckPass() {
		Interest iac = _repo.getInterests().stream().filter(i -> "iac_slack_test".equals(i.getNotificationKey())).findAny().get();

		withEvent("IdentityAttributesChangedEvent", InterestRepositoryJsonImplTest.JSON_IDENTITY_ATTRIBUTES_CHANGED_EVENT);
		withSlackTenants("acme-solar", "acme-lunar");
		_interestMatcherEventHandler.setInterest(iac);

		// Handle event twice
		_interestMatcherEventHandler.handleEvent(_context);
		_interestMatcherEventHandler.handleEvent(_context);

		// Verify event service published Interest Matched twice
		verify(_eventService, times(2)).publish(any(),
				any(), any());

		// Verify memoization, i.e. teams service should be called just once
		verify(_slackService, times(1)).getSlackTenants();
	}

	@Test
	public void handleEventTestSlackTenantCheckFail() {
		Interest iac = _repo.getInterests().stream().filter(i-> "iac_slack_test".equals(i.getNotificationKey())).findAny().get();

		withEvent("IdentityAttributesChangedEvent", InterestRepositoryJsonImplTest.JSON_IDENTITY_ATTRIBUTES_CHANGED_EVENT);
		withSlackTenants("acme-jovian", "acme-lunar");
		_interestMatcherEventHandler.setInterest(iac);

		// Handle event twice
		_interestMatcherEventHandler.handleEvent(_context);
		_interestMatcherEventHandler.handleEvent(_context);

		// Verify event service did not publish Interest Matched
		verify(_eventService, times(0)).publish(any(),
				any(), any());

		// Verify memoization, i.e. teams service should be called just once
		verify(_slackService, times(1)).getSlackTenants();
	}

	@Test
	public void handleEventTestTeamsAttributesCheckFail() {
		Interest iac = _repo.getInterests().stream().filter(i->i.getNotificationKey().equals("iac_test")).findAny().get();

		withEvent("IdentityAttributesChangedEvent", InterestRepositoryJsonImplTest.JSON_IDENTITY_ATTRIBUTES_CHANGED_NOT_SUPPORTED_ATTRIBUTES);
		withTeamsTenants("acme-solar", "acme-lunar");
		_interestMatcherEventHandler.setInterest(iac);

		_interestMatcherEventHandler.handleEvent(_context);

		// Verify event service published Interest Matched twice
		verify(_eventService, never()).publish(any(), any(), any());

		// Verify memoization, i.e. teams service should be called just once
		verify(_teamsService, times(1)).getTeamsTenants();
	}

	@Test
	public void handleEventTestIdentityAttributeChangedType() {
		Interest iac = _repo.getInterests().stream().filter(i->i.getNotificationKey().equals("iac_test")).findAny().get();

		withEvent("IdentityAttributesChangedEvent", InterestRepositoryJsonImplTest.JSON_IDENTITY_ATTRIBUTES_CHANGED_EVENT_NOT_STRING);
		withTeamsTenants("acme-solar", "acme-lunar");
		_interestMatcherEventHandler.setInterest(iac);

		_interestMatcherEventHandler.handleEvent(_context);

		// Verify event service published Interest properly
		verify(_eventService, times(1)).publish(any(),
				any(), any());

	}

	private void withDebugEvent() {
		Event event = EventBuilder.withTypeAndContentJson("ACCESS_APPROVAL_REQUESTED", InterestRepositoryJsonImplTest.JSON_EVENT)
				.addHeader(InterestMatcherDebugService.REDIS_INTEREST_DEBUG_KEY, "testID")
				.addHeader(EventHeaders.POD, "dev")
				.addHeader(EventHeaders.ORG, "acme-solar")
				.build();

		when(_context.getEvent())
				.thenReturn(event);
	}

	private void withEvent(String type, String json) {
		TestUtil.setDummyRequestContext();
		Event event = EventBuilder.withTypeAndContentJson(type, json)
				.addHeader(EventHeaders.POD, "dev")
				.addHeader(EventHeaders.ORG, "acme-solar")
				.build();

		when(_context.getEvent())
				.thenReturn(event);
	}

	private void withTeamsTenants(String ... tenants) {
		when(_teamsService.getTeamsTenants())
				.thenReturn(Arrays.asList(tenants));
	}

	private void withSlackTenants(String... tenants) {
		when(_slackService.getSlackTenants())
				.thenReturn(Arrays.asList(tenants));
	}
}
