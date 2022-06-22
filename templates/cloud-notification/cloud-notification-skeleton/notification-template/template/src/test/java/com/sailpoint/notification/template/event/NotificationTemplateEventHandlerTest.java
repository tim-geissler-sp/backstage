/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.event;

import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.api.event.RecipientBuilder;
import com.sailpoint.notification.api.event.dto.NotificationRendered;
import com.sailpoint.notification.api.event.dto.SlackNotificationRendered;
import com.sailpoint.notification.api.event.dto.TeamsNotificationRendered;
import com.sailpoint.notification.sender.common.event.userpreferences.UserPreferencesMatchedBuilder;
import com.sailpoint.notification.sender.common.event.userpreferences.dto.UserPreferencesMatched;
import com.sailpoint.notification.context.service.GlobalContextService;
import com.sailpoint.notification.template.context.TemplateContext;
import com.sailpoint.notification.template.context.impl.velocity.TemplateContextVelocity;
import com.sailpoint.notification.template.service.NotificationTemplateDebugService;
import com.sailpoint.notification.template.service.NotificationTemplateService;
import org.apache.commons.logging.Log;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.sailpoint.notification.context.service.GlobalContextService.EMAIL_OVERRIDE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for NotificationTemplateEventHandler
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationTemplateEventHandlerTest {

	public static final String RECIPIENT_NAME = "james";

	public static final String RECIPIENT_PHONE = "123-3456-7890";

	public static final String RECIPIENT_EMAIL = "james@domain.com";

	public static final String RECIPIENT_ID = "recipientId";

	public static final String TEMPLATE_BODY_PATTERN = "Phone %s - Email: %s";

	public static final String TEMPLATE_SUBJECT_PATTERN = "Welcome %s";

	public static final String USER_PREFERENCE_MEDIUM_EMAIL = "email";

	public static final String USER_PREFERENCE_MEDIUM_SLACK = "slack";

	public static final String USER_PREFERENCE_MEDIUM_TEAMS = "teams";

	public static final String NOTIFICATION_KEY = "notificationKey";

	@Mock
	NotificationTemplateService _notificationTemplateService;

	@Mock
	NotificationTemplateDebugService _notificationTemplateDebugService;

	@Mock
	GlobalContextService _globalContextService;

	@Mock
	EventHandlerContext _eventHandlerContext;

	@Mock
	Log _log;

	@Captor
	ArgumentCaptor<Event> _eventArgumentCaptor;

	private NotificationTemplateEventHandler _notificationTemplateEventHandler;

	private UserPreferencesMatched _userPreferencesMatched;

	private Event _userPreferencesMatchedEvent;

	@Before
	public void setUp() {
		_notificationTemplateEventHandler = new NotificationTemplateEventHandler(_notificationTemplateService, _notificationTemplateDebugService, _globalContextService);
		NotificationTemplateEventHandler._log = _log;
	}

	@Test
	public void publishEventTest() {

		// Given incoming event and mocks
		givenUserPreferencesMatched(USER_PREFERENCE_MEDIUM_EMAIL);
		givenUserPreferencesMatchedEvent();
		givenEventHandlerGetEventMock();
		givenNotificationTemplateServiceGetTemplateContextMock();
		givenNotificationTemplateServiceRenderNotificationKeyMock();

		// When handle event
		_notificationTemplateEventHandler.handleEvent(_eventHandlerContext);

		// Then verify event Published
		verify(_notificationTemplateService).publishEvent(_eventArgumentCaptor.capture());

		Event publishedEvent = _eventArgumentCaptor.getValue();
		Assert.assertNotNull(publishedEvent);

		NotificationRendered renderedEvent = publishedEvent.getContent(NotificationRendered.class);
		Assert.assertNotNull(renderedEvent);

		// And verify notification rendered
		Assert.assertEquals(USER_PREFERENCE_MEDIUM_EMAIL, renderedEvent.getMedium());
		Assert.assertEquals(renderStringFormat(TEMPLATE_SUBJECT_PATTERN, RECIPIENT_NAME), renderedEvent.getSubject());
		Assert.assertEquals(renderStringFormat(TEMPLATE_BODY_PATTERN, RECIPIENT_PHONE, RECIPIENT_EMAIL), renderedEvent.getBody());
		Assert.assertEquals(RECIPIENT_NAME, renderedEvent.getRecipient().getName());
		Assert.assertEquals(RECIPIENT_PHONE, renderedEvent.getRecipient().getPhone());
		Assert.assertEquals(RECIPIENT_EMAIL, renderedEvent.getRecipient().getEmail());
		Assert.assertNotNull(renderedEvent);
	}

	@Test
	public void publishEventWithKeyTest() {

		// Given incoming event and mocks
		givenUserPreferencesMatched(USER_PREFERENCE_MEDIUM_EMAIL);
		givenUserPreferencesMatchedEvent();
		givenEventHandlerGetEventMock();
		givenNotificationTemplateServiceGetTemplateContextMock();
		givenNotificationTemplateServiceRenderNotificationKeyMock();

		// When handle event
		_notificationTemplateEventHandler.handleEvent(_eventHandlerContext);

		// Then verify event Published
		verify(_notificationTemplateService).publishEvent(_eventArgumentCaptor.capture());

		Event publishedEvent = _eventArgumentCaptor.getValue();
		Assert.assertNotNull(publishedEvent);

		NotificationRendered renderedEvent = publishedEvent.getContent(NotificationRendered.class);
		Assert.assertNotNull(renderedEvent);

		// And verify notification rendered
		Assert.assertEquals(USER_PREFERENCE_MEDIUM_EMAIL, renderedEvent.getMedium());
		Assert.assertEquals(renderStringFormat(TEMPLATE_SUBJECT_PATTERN, RECIPIENT_NAME), renderedEvent.getSubject());
		Assert.assertEquals(renderStringFormat(TEMPLATE_BODY_PATTERN, RECIPIENT_PHONE, RECIPIENT_EMAIL), renderedEvent.getBody());
		Assert.assertEquals(RECIPIENT_NAME, renderedEvent.getRecipient().getName());
		Assert.assertEquals(RECIPIENT_PHONE, renderedEvent.getRecipient().getPhone());
		Assert.assertEquals(RECIPIENT_EMAIL, renderedEvent.getRecipient().getEmail());
		Assert.assertNotNull(renderedEvent);
	}

	@Test
	public void publishSlackEventWithKeyTest() {

		// Given incoming event and mocks
		givenUserPreferencesMatched(USER_PREFERENCE_MEDIUM_SLACK);
		givenUserPreferencesMatchedEvent();
		givenEventHandlerGetEventMock();
		givenNotificationTemplateServiceGetTemplateContextMock();
		givenNotificationTemplateServiceRenderNotificationKeyMock();

		// When handle event
		_notificationTemplateEventHandler.handleEvent(_eventHandlerContext);

		// Then verify event Published
		verify(_notificationTemplateService).publishEvent(_eventArgumentCaptor.capture());

		Event publishedEvent = _eventArgumentCaptor.getValue();
		Assert.assertNotNull(publishedEvent);

		SlackNotificationRendered renderedEvent = publishedEvent.getContent(SlackNotificationRendered.class);
		Assert.assertNotNull(renderedEvent);

		// And verify notification rendered
		Assert.assertEquals(renderStringFormat(TEMPLATE_SUBJECT_PATTERN, RECIPIENT_NAME), renderedEvent.getText());
		Assert.assertEquals(renderStringFormat(TEMPLATE_BODY_PATTERN, RECIPIENT_PHONE, RECIPIENT_EMAIL), renderedEvent.getBlocks());
		Assert.assertEquals(RECIPIENT_NAME, renderedEvent.getRecipient().getName());
		Assert.assertEquals(RECIPIENT_PHONE, renderedEvent.getRecipient().getPhone());
		Assert.assertEquals(RECIPIENT_EMAIL, renderedEvent.getRecipient().getEmail());
		Assert.assertNotNull(renderedEvent);
	}

	@Test
	public void publishTeamsEventWithKeyTest() {

		// Given incoming event and mocks
		givenUserPreferencesMatched(USER_PREFERENCE_MEDIUM_TEAMS);
		givenUserPreferencesMatchedEvent();
		givenEventHandlerGetEventMock();
		givenNotificationTemplateServiceGetTemplateContextMock();
		givenNotificationTemplateServiceRenderNotificationKeyMock();

		// When handle event
		_notificationTemplateEventHandler.handleEvent(_eventHandlerContext);

		// Then verify event Published
		verify(_notificationTemplateService).publishEvent(_eventArgumentCaptor.capture());

		Event publishedEvent = _eventArgumentCaptor.getValue();
		Assert.assertNotNull(publishedEvent);

		TeamsNotificationRendered renderedEvent = publishedEvent.getContent(TeamsNotificationRendered.class);
		Assert.assertNotNull(renderedEvent);

		// And verify notification rendered
		Assert.assertEquals(renderStringFormat(TEMPLATE_SUBJECT_PATTERN, RECIPIENT_NAME), renderedEvent.getText());
		Assert.assertEquals(renderStringFormat(TEMPLATE_BODY_PATTERN, RECIPIENT_PHONE, RECIPIENT_EMAIL), renderedEvent.getMessageJSON());
		Assert.assertEquals(RECIPIENT_NAME, renderedEvent.getRecipient().getName());
		Assert.assertEquals(RECIPIENT_PHONE, renderedEvent.getRecipient().getPhone());
		Assert.assertEquals(RECIPIENT_EMAIL, renderedEvent.getRecipient().getEmail());
		Assert.assertNotNull(renderedEvent);
	}

	@Test
	public void noMediumTest() {

		// Given incoming event and mocks
		givenUserPreferencesMatchedNoMedium();
		givenUserPreferencesMatchedEvent();
		givenEventHandlerGetEventMock();

		// When handle event
		_notificationTemplateEventHandler.handleEvent(_eventHandlerContext);

		// Then verify event Published hasn't been called
		verify(_notificationTemplateService, times(0)).publishEvent(_eventArgumentCaptor.capture());
		Assert.assertEquals(Collections.EMPTY_LIST, _eventArgumentCaptor.getAllValues());
	}

	@Test(expected = IllegalStateException.class)
	public void noNotificationRenderedTest() {

		List<String> mediums = Arrays.asList(USER_PREFERENCE_MEDIUM_EMAIL, USER_PREFERENCE_MEDIUM_SLACK, USER_PREFERENCE_MEDIUM_TEAMS);
		for(String medium : mediums) {
			// Given incoming event and mocks
			givenUserPreferencesMatchedNoNotificationKey(medium);
			givenUserPreferencesMatchedEvent();
			givenEventHandlerGetEventMock();
			givenNotificationTemplateServiceGetTemplateContextMock();
			givenNotificationTemplateServiceRenderNotificationKeyMock();

			// When handle event
			_notificationTemplateEventHandler.handleEvent(_eventHandlerContext);

			// Then verify event Published hasn't been called
			verify(_notificationTemplateService, times(0)).publishEvent(_eventArgumentCaptor.capture());
			Assert.assertEquals(Collections.EMPTY_LIST, _eventArgumentCaptor.getAllValues());
		}
	}

	@Test
	public void debugHeaderTest() {
		// Given incoming event and mocks
		givenUserPreferencesMatched(USER_PREFERENCE_MEDIUM_EMAIL);
		givenUserPreferencesMatchedDebugEvent();
		givenEventHandlerGetEventMock();
		givenNotificationTemplateServiceGetTemplateContextMock();
		givenNotificationTemplateServiceRenderNotificationKeyMock();
		ArgumentCaptor<String> headerArgumentCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> subjectArgumentCaptor = ArgumentCaptor.forClass(String.class);

		// When handle event
		_notificationTemplateEventHandler.handleEvent(_eventHandlerContext);

		// Then verify event Published
		verify(_notificationTemplateDebugService)
				.writeToStore(headerArgumentCaptor.capture(), subjectArgumentCaptor.capture());

		String headerValue = headerArgumentCaptor.getValue();
		String subjectValue = subjectArgumentCaptor.getValue();

		Assert.assertNotNull(headerValue);
		Assert.assertNotNull(subjectValue);
		Assert.assertEquals("redisValue", headerValue);
		Assert.assertEquals("Welcome james", subjectValue);

	}

	@Test
	public void missedKeyTest() {
		givenUserPreferencesMatchedWithError( true);
		givenUserPreferencesMatchedEvent();
		givenEventHandlerGetEventMock();

		_notificationTemplateEventHandler.handleEvent(_eventHandlerContext);

		verify(NotificationTemplateEventHandler._log, times(1)).error(any(), isA(Throwable.class));
	}

	@Test
	public void missedMediumTest() {
		givenUserPreferencesMatchedWithError( true);
		givenUserPreferencesMatchedEvent();
		givenEventHandlerGetEventMock();

		_notificationTemplateEventHandler.handleEvent(_eventHandlerContext);

		verify(NotificationTemplateEventHandler._log, times(1)).error(any(), isA(Throwable.class));
	}

	@Test
	public void emailOverrideTest() {
		givenUserPreferencesMatched(USER_PREFERENCE_MEDIUM_EMAIL);
		givenUserPreferencesMatchedEvent();
		givenNotificationTemplateServiceGlobalContextMock();
		givenEventHandlerGetEventMock();
		givenNotificationTemplateServiceGetTemplateContextMock();
		givenNotificationTemplateServiceRenderNotificationKeyMock();

		_notificationTemplateEventHandler.handleEvent(_eventHandlerContext);

		// Then verify event Published
		verify(_notificationTemplateService).publishEvent(_eventArgumentCaptor.capture());

		Event publishedEvent = _eventArgumentCaptor.getValue();
		Assert.assertNotNull(publishedEvent);

		NotificationRendered renderedEvent = publishedEvent.getContent(NotificationRendered.class);
		Assert.assertNotNull(renderedEvent);

		Assert.assertEquals("test@forward.com", renderedEvent.getRecipient().getEmail());
	}

	private void givenUserPreferencesMatched(String medium) {
		UserPreferencesMatchedBuilder builder = new UserPreferencesMatchedBuilder()
				.withMedium(medium)
				.withNotificationKey(NOTIFICATION_KEY)
				.withDomainEvent(EventBuilder.withTypeAndContentJson("ACCESS_APPROVAL_REQUESTED", "{}").build())
				.withRecipient(new RecipientBuilder()
						.withId(RECIPIENT_ID)
						.withPhone(RECIPIENT_PHONE)
						.withName(RECIPIENT_NAME)
						.withEmail(RECIPIENT_EMAIL)
						.build());

		_userPreferencesMatched = builder.build();
	}

	private void givenUserPreferencesMatchedWithError(boolean noMedia) {
		UserPreferencesMatchedBuilder builder = new UserPreferencesMatchedBuilder()
				.withDomainEvent(EventBuilder.withTypeAndContentJson("ACCESS_APPROVAL_REQUESTED", "{}").build())
				.withRecipient(new RecipientBuilder()
						.withId(RECIPIENT_ID)
						.withPhone(RECIPIENT_PHONE)
						.withName(RECIPIENT_NAME)
						.withEmail(RECIPIENT_EMAIL)
						.build());

		if(!noMedia) {
			builder.withMedium(USER_PREFERENCE_MEDIUM_EMAIL);
		}
		_userPreferencesMatched = new UserPreferencesMatched(builder);
	}

	private void givenUserPreferencesMatchedNoNotificationKey(String medium) {
		_userPreferencesMatched = new UserPreferencesMatchedBuilder()
				.withMedium(medium)
				.withDomainEvent(EventBuilder.withTypeAndContentJson("ACCESS_APPROVAL_REQUESTED", "{}").build())
				.withRecipient(new RecipientBuilder()
						.withId(RECIPIENT_ID)
						.withPhone(RECIPIENT_PHONE)
						.withName(RECIPIENT_NAME)
						.withEmail(RECIPIENT_EMAIL)
						.build())
				.build();
	}


	private void givenUserPreferencesMatchedNoMedium() {
		_userPreferencesMatched = new UserPreferencesMatchedBuilder()
				.withMedium(USER_PREFERENCE_MEDIUM_EMAIL)
				.withNotificationKey(NOTIFICATION_KEY)
				.withDomainEvent(EventBuilder.withTypeAndContentJson("ACCESS_APPROVAL_REQUESTED", "{}").build())
				.withRecipient(new RecipientBuilder()
						.withId(RECIPIENT_ID)
						.withPhone(RECIPIENT_PHONE)
						.withName(RECIPIENT_NAME)
						.withEmail(RECIPIENT_EMAIL)
						.build())
				.build();
	}

	private void givenUserPreferencesMatchedEvent() {
		_userPreferencesMatchedEvent = EventBuilder
				.withTypeAndContent(EventType.NOTIFICATION_USER_PREFERENCES_MATCHED, _userPreferencesMatched)
				.addHeader(EventHeaders.ORG, "acme-solar")
				.build();
	}

	private void givenUserPreferencesMatchedDebugEvent () {
		_userPreferencesMatchedEvent = EventBuilder
				.withTypeAndContent(EventType.NOTIFICATION_USER_PREFERENCES_MATCHED, _userPreferencesMatched)
				.addHeader(NotificationTemplateDebugService.REDIS_TEMPLATE_DEBUG_KEY, "redisValue")
				.addHeader(EventHeaders.ORG, "acme-solar")
				.build();
	}

	private void givenEventHandlerGetEventMock() {
		when(_eventHandlerContext.getEvent()). thenReturn(_userPreferencesMatchedEvent);
	}

	private void givenNotificationTemplateServiceGetTemplateContextMock() {
		when(_notificationTemplateService.getTemplateContextV2(isNull(TemplateContext.class), anyString(), any())).thenAnswer(invocation -> {
			TemplateContext templateContext = new TemplateContextVelocity();
			templateContext.put("event.recipient.name", RECIPIENT_NAME);
			templateContext.put("event.recipient.phone", RECIPIENT_PHONE);
			templateContext.put("event.recipient.email", RECIPIENT_EMAIL);


			return templateContext;
		});

		when(_notificationTemplateService.getTemplateContextV2(notNull(TemplateContext.class), anyString(), any())).thenAnswer(invocation -> {
			TemplateContext templateContext = new TemplateContextVelocity();
			templateContext.put("event.recipient.name", RECIPIENT_NAME);
			templateContext.put("event.recipient.phone", RECIPIENT_PHONE);
			templateContext.put("event.recipient.email", RECIPIENT_EMAIL);

			return templateContext;
		});
	}

	private void givenNotificationTemplateServiceRenderNotificationKeyMock() {
		when(_notificationTemplateService.renderByNotificationKey(anyString(), anyString(), anyString(), any(), any()))
				.thenAnswer(invocation -> {
					Object[] args = invocation.getArguments();
					String medium = (String) args[2];
					TemplateContext templateContext = (TemplateContext) args[4];

					String subject = renderStringFormat(TEMPLATE_SUBJECT_PATTERN, templateContext.get("event.recipient.name"));
					String body = renderStringFormat(
							TEMPLATE_BODY_PATTERN,
							templateContext.get("event.recipient.phone"),
							templateContext.get("event.recipient.email"));


					NotificationRendered notificationRendered = NotificationRendered.builder()
							.subject(subject)
							.body(body)
							.medium(medium)
							.build();

					return notificationRendered;
				});

		when(_notificationTemplateService.renderSlackByNotificationKey(anyString(), anyString(), anyString(), any(), any()))
				.thenAnswer(invocation -> {
					Object[] args = invocation.getArguments();
					String medium = (String) args[2];
					TemplateContext templateContext = (TemplateContext) args[4];

					String text  = renderStringFormat(TEMPLATE_SUBJECT_PATTERN, templateContext.get("event.recipient.name"));
					String block = renderStringFormat(
							TEMPLATE_BODY_PATTERN,
							templateContext.get("event.recipient.phone"),
							templateContext.get("event.recipient.email"));


					SlackNotificationRendered notificationRendered = SlackNotificationRendered.builder()
							.text(text)
							.blocks(block)
							.build();

					return notificationRendered;
				});

		when(_notificationTemplateService.renderTeamsMessageByNotificationKey(anyString(), anyString(), anyString(), any(), any()))
				.thenAnswer(invocation -> {
					Object[] args = invocation.getArguments();
					String medium = (String) args[2];
					TemplateContext templateContext = (TemplateContext) args[4];

					String title  = renderStringFormat(TEMPLATE_SUBJECT_PATTERN, templateContext.get("event.recipient.name"));
					String text  = renderStringFormat(TEMPLATE_SUBJECT_PATTERN, templateContext.get("event.recipient.name"));
					String block = renderStringFormat(
							TEMPLATE_BODY_PATTERN,
							templateContext.get("event.recipient.phone"),
							templateContext.get("event.recipient.email"));


					TeamsNotificationRendered notificationRendered = TeamsNotificationRendered.builder()
							.title(title)
							.text(text)
							.messageJSON(block)
							.build();

					return notificationRendered;
				});

	}

	private void givenNotificationTemplateServiceGlobalContextMock() {
		when(_globalContextService.getDefaultContext(anyString()))
				.thenReturn(Collections.singletonMap(EMAIL_OVERRIDE, "test@forward.com"));
	}

	private static String renderStringFormat(String pattern,  Object... args) {
		return String.format(pattern, args);
	}
}
