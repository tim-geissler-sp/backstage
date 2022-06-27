/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.event;

import com.google.inject.Provider;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.client.OrgTopic;
import com.sailpoint.iris.client.Topic;
import com.sailpoint.iris.client.TopicDescriptor;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.api.event.RecipientBuilder;
import com.sailpoint.notification.api.event.dto.NotificationMedium;
import com.sailpoint.notification.sender.common.event.interest.matching.NotificationInterestMatchedBuilder;
import com.sailpoint.notification.sender.common.event.userpreferences.dto.UserPreferencesMatched;
import com.sailpoint.notification.sender.common.test.TestUtil;
import com.sailpoint.notification.orgpreferences.repository.TenantPreferencesRepository;
import com.sailpoint.notification.orgpreferences.repository.dto.PreferencesDto;
import com.sailpoint.notification.userpreferences.dto.UserPreferences;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;
import com.sailpoint.notification.userpreferences.service.UserPreferencesDebugService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test UserPreferencesEventHandler class. Debug route and Productions.
 */
public class UserPreferencesEventHandlerTest {

	@Mock
	UserPreferencesRepository _userPreferencesRepository;

	@Mock
	UserPreferencesDebugService _userPreferencesDebugService;

	@Mock
	EventHandlerContext _context;

	@Mock
	Provider<EventService> _esProvider;

	@Mock
	EventService _eventService;

	@Mock
	TenantPreferencesRepository _tenantPreferencesRepository;

	private UserPreferencesEventHandler _userPreferencesEventHandler;

	private final String RECIPIENT_ID = "70e7cde5-3473-46ea-94ea-90bc8c605a6c";

	private final String TEST_EMAIL = "test@acme-solar.com";

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		Topic topic = new OrgTopic("notification", "dev", "acme-solar");

		when(_context.getTopic())
				.thenReturn(topic);

		when(_esProvider.get())
				.thenReturn(_eventService);

		UserPreferences userPreferences = new UserPreferences.UserPreferencesBuilder()
				.withRecipient(new RecipientBuilder()
					.withId(RECIPIENT_ID)
					.withEmail("jane.doe@sailpoint.com")
					.build())
				.build();

		when(_userPreferencesRepository.findByRecipientId(any(String.class))).thenReturn(userPreferences);
		when(_userPreferencesDebugService.getUserPreferences(any(String.class))).thenReturn(userPreferences);

		RequestContext.set(TestUtil.setDummyRequestContext());
		when(_tenantPreferencesRepository.findOneForTenantAndKey(any(String.class), any(String.class))).thenReturn(null);

		_userPreferencesEventHandler = new UserPreferencesEventHandler(_userPreferencesDebugService,
				_userPreferencesRepository,
				_esProvider,
				_tenantPreferencesRepository);
	}

	@Test
	public void handleEventDebugTest() {
		withDebugEvent();
		_userPreferencesEventHandler.handleEvent(_context);

		verify(_userPreferencesDebugService, times(1)).writeToStore("testID:EMAIL",
				"jane.doe@sailpoint.com");

		verify(_esProvider, times(0)).get();
	}

	@Test
	public void handleEventTest() {
		withEvent();
		_userPreferencesEventHandler.handleEvent(_context);

		verify(_userPreferencesDebugService, times(0)).writeToStore(any(String.class),
				any(String.class));

		verify(_esProvider, times(1)).get();

		ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<TopicDescriptor> topicDescriptorCaptor = ArgumentCaptor.forClass(TopicDescriptor.class);
		verify(_eventService, times(1)).publish(topicDescriptorCaptor.capture(),
				eventTypeCaptor.capture(), any(UserPreferencesMatched.class));
		Assert.assertEquals(EventType.NOTIFICATION_USER_PREFERENCES_MATCHED, eventTypeCaptor.getValue());
		Assert.assertEquals(IdnTopic.NOTIFICATION, topicDescriptorCaptor.getValue());
	}

	@Test
	public void handleEventWithOverrideEmailTest() {
		withDifferentEmailEvent();
		_userPreferencesEventHandler.handleEvent(_context);

		verify(_userPreferencesDebugService, times(0)).writeToStore(any(String.class),
				any(String.class));

		verify(_esProvider, times(1)).get();

		ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<TopicDescriptor> topicDescriptorCaptor = ArgumentCaptor.forClass(TopicDescriptor.class);
		ArgumentCaptor<UserPreferencesMatched> userPreferencesCaptor =  ArgumentCaptor.forClass(UserPreferencesMatched.class);

		verify(_eventService, times(1)).publish(topicDescriptorCaptor.capture(),
				eventTypeCaptor.capture(), userPreferencesCaptor.capture());
		Assert.assertEquals(EventType.NOTIFICATION_USER_PREFERENCES_MATCHED, eventTypeCaptor.getValue());
		Assert.assertEquals(IdnTopic.NOTIFICATION, topicDescriptorCaptor.getValue());
		Assert.assertEquals(TEST_EMAIL, userPreferencesCaptor.getValue().getRecipient().getEmail());
	}

	@Test
	public void optOutTest() {
		withEnabledEvent();

		PreferencesDto preferencesDto = new PreferencesDto();
		preferencesDto.setMediums(null);
		when(_tenantPreferencesRepository.findOneForTenantAndKey(any(String.class), any(String.class))).thenReturn(preferencesDto);

		_userPreferencesEventHandler.handleEvent(_context);
		verify(_eventService, never()).publish(any(), any(), any());
	}

	@Test
	public void noPreferencesTest() {
		withDisabledEvent();
		_userPreferencesEventHandler.handleEvent(_context);
		verify(_eventService, never()).publish(any(), any(), any());
	}

	@Test
	public void optInTest() {
		withDisabledEvent();
		PreferencesDto preferencesDto = new PreferencesDto();
		preferencesDto.setMediums(Arrays.asList(NotificationMedium.EMAIL));
		when(_tenantPreferencesRepository.findOneForTenantAndKey(any(String.class), any(String.class))).thenReturn(preferencesDto);

		_userPreferencesEventHandler.handleEvent(_context);
		verify(_eventService, times(1)).publish(any(), any(), any());
	}


	private void withDebugEvent() {
		Event event = createEventBuilder(null)
				.addHeader(UserPreferencesDebugService.REDIS_USER_DEBUG_KEY, "testID").build();

		when(_context.getEvent())
				.thenReturn(event);
	}

	private void withEvent() {
		Event event = createEventBuilder(null).build();

		when(_context.getEvent())
				.thenReturn(event);
	}

	private void withDifferentEmailEvent() {
		Event event = createEventBuilder(TEST_EMAIL).build();

		when(_context.getEvent())
				.thenReturn(event);
	}

	private void withDisabledEvent()
	{
		Event event = createEventBuilder(null, false).build();

		when(_context.getEvent())
				.thenReturn(event);
	}

	private void withEnabledEvent()
	{
		Event event = createEventBuilder(null, true).build();

		when(_context.getEvent())
				.thenReturn(event);
	}

	private EventBuilder createEventBuilder(String email) {
		return createEventBuilder(email, true);
	}

	private EventBuilder createEventBuilder(String email, boolean enabled) {
		Event eventOriginal = EventBuilder.withTypeAndContentJson("ACCESS_APPROVAL_REQUESTED", JSON_EVENT)
				.addHeader(EventHeaders.POD, "dev")
				.addHeader(EventHeaders.ORG, "acme-solar")
				.build();

		NotificationInterestMatchedBuilder builder = new NotificationInterestMatchedBuilder(
				UUID.randomUUID().toString(), eventOriginal)
				.withRecipientId(RECIPIENT_ID)
				.withNotificationKey("approval_request")
				.withCategoryName("email")
				.withInterestName("Access Approval Request")
				.withEnabled(enabled);

		if(email != null) {
			builder.withRecipientEmail(email);
		}

		return EventBuilder.withTypeAndContent(EventType.NOTIFICATION_INTEREST_MATCHED, builder.build())
				.addHeader(EventHeaders.POD, "dev")
				.addHeader(EventHeaders.ORG, "acme-solar");
	}

	public final static String JSON_EVENT = "{\"content\": {  \n"+
			"  \"approvers\": [{  \n"+
			"      \"id\": \"314cf125-f892-4b16-bcbb-bfe4afb01f85\",  \n"+
			"      \"name\": \"james.smith\"  \n"+
			"  }, {  \n"+
			"      \"id\": \"70e7cde5-3473-46ea-94ea-90bc8c605a6c\",  \n"+
			"      \"name\": \"jane.doe\"  \n"+
			"  }],  \n"+
			"  \"requester_id\": \"46ec3058-eb0a-41b2-8df8-1c3641e4d771\",  \n"+
			"  \"requester_name\": \"boss.man\",  \n"+
			"  \"accessItems\": [{  \n"+
			"      \"type\": \"ROLE\",  \n"+
			"      \"name\": \"Engineering Administrator\"  \n"+
			"  }]  \n"+
			"}}";
}
