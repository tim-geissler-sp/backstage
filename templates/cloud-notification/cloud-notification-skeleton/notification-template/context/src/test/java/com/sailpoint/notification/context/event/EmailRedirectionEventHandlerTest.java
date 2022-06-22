/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.context.event;

import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.context.common.model.BrandConfig;
import com.sailpoint.notification.context.common.model.GlobalContext;
import com.sailpoint.notification.context.service.GlobalContextDebugService;
import com.sailpoint.notification.context.service.GlobalContextService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.sailpoint.notification.context.service.GlobalContextService.EMAIL_OVERRIDE;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *  Tests for BrandingChangedEventHandler
 */
public class EmailRedirectionEventHandlerTest {

	private EmailRedirectionEventHandler _emailRedirectionEventHandler;

	@Mock
	EventHandlerContext _eventHandlerContext;

	@Mock
	GlobalContextService _globalContextService;

	@Mock
	GlobalContextDebugService _globalContextDebugService;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		_emailRedirectionEventHandler = new EmailRedirectionEventHandler(_globalContextService, _globalContextDebugService);
	}

	@Test
	public void testEmailRedirectionEnabled() throws Exception {
		withEmailRedirectionEnabled();
		_emailRedirectionEventHandler.handleEvent(_eventHandlerContext);
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(_globalContextService, times(1)).saveAttribute(anyString(), anyString(), captor.capture());
		verify(_globalContextDebugService, times(1)).writeToStore(anyString(), anyString());

		String emailOverride = captor.getValue();
		Assert.assertNotNull(emailOverride);
		Assert.assertEquals("test@mail.com", emailOverride);
	}

	@Test
	public void testEmailRedirectionEnabledNoDebug() throws Exception {
		withEmailRedirectionEnabledNoDebug();
		_emailRedirectionEventHandler.handleEvent(_eventHandlerContext);
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(_globalContextService, times(1)).saveAttribute(anyString(), anyString(), captor.capture());
		verify(_globalContextDebugService, times(0)).writeToStore(anyString(), anyString());

		String emailOverride = captor.getValue();
		Assert.assertNotNull(emailOverride);
		Assert.assertEquals("test@mail.com", emailOverride);
	}

	@Test
	public void testEmailRedirectionDisabled() throws Exception {
		withEmailRedirectionDisabled();
		_emailRedirectionEventHandler.handleEvent(_eventHandlerContext);
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(_globalContextService, times(1)).removeAttribute(anyString(), captor.capture());
		verify(_globalContextDebugService, times(1)).writeToStore(anyString(), anyString());

		Assert.assertEquals(EMAIL_OVERRIDE, captor.getValue());
	}

	@Test
	public void testEmailRedirectionDisbledNoDebug() throws Exception {
		withEmailRedirectionDisabledNoDebug();
		_emailRedirectionEventHandler.handleEvent(_eventHandlerContext);

		verify(_globalContextService, times(1)).removeAttribute(anyString(), anyString());
		verify(_globalContextDebugService, times(0)).writeToStore(anyString(), anyString());
	}

	@Test(expected =  IllegalStateException.class)
	public void testInvalidEventNoOrgHeader() {
		when(_eventHandlerContext.getEvent()).thenReturn(EventBuilder.withTypeAndContent(EventType.EMAIL_REDIRECTION_ENABLED, "test").build());
		_emailRedirectionEventHandler.handleEvent(_eventHandlerContext);
	}

	private void withEmailRedirectionEnabled() {
		when(_eventHandlerContext.getEvent()).thenReturn(getDummyEventDebug(EventType.EMAIL_REDIRECTION_ENABLED, "test@mail.com"));
	}

	private void withEmailRedirectionEnabledNoDebug() {
		when(_eventHandlerContext.getEvent()).thenReturn(getDummyEvent(EventType.EMAIL_REDIRECTION_ENABLED, "test@mail.com"));
	}

	private void withEmailRedirectionDisabled() {
		when(_eventHandlerContext.getEvent()).thenReturn(getDummyEventDebug(EventType.EMAIL_REDIRECTION_DISABLED, null));
	}

	private void withEmailRedirectionDisabledNoDebug() {
		when(_eventHandlerContext.getEvent()).thenReturn(getDummyEvent(EventType.EMAIL_REDIRECTION_DISABLED, "test"));
	}
	private Event getDummyEvent(String type, Object content) {
		return EventBuilder.withTypeAndContent(type, content)
				.addHeader(EventHeaders.ORG, "acme-solar")
				.build();
	}

	private Event getDummyEventDebug(String type, Object content) {
		return EventBuilder.withTypeAndContent(type, content)
				.addHeader(EventHeaders.ORG, "acme-solar")
				.addHeader(GlobalContextDebugService.REDIS_CONTEXT_DEBUG_KEY, "xyz")
				.build();
	}
}
