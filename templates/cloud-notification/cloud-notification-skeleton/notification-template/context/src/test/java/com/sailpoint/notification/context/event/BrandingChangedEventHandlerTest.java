/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *  Tests for BrandingChangedEventHandler
 */
public class BrandingChangedEventHandlerTest {

	private BrandingChangedEventHandler _brandingChangedEventHandler;

	@Mock
	EventHandlerContext _eventHandlerContext;

	@Mock
	GlobalContextService _globalContextService;

	@Mock
	GlobalContextDebugService _globalContextDebugService;

	private BrandConfig _config;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		_brandingChangedEventHandler = new BrandingChangedEventHandler(_globalContextService, _globalContextDebugService);
		withDummyBrandConfig();
	}

	@Test
	public void testBrandingCreate() throws Exception {
		withBrandingCreate();
		_brandingChangedEventHandler.handleEvent(_eventHandlerContext);
		ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
		verify(_globalContextService, times(1)).saveBrandingAttributes(anyString(), captor.capture());
		verify(_globalContextDebugService, times(1)).writeToStore(anyString(), anyString());

		Map<String, Object> attributes = captor.getValue();
		Assert.assertNotNull(attributes);
		Assert.assertEquals("actionButtonColor", ((Map<String, Object>) attributes.get("test")).get("actionButtonColor"));
	}

	@Test
	public void testBrandingUpdate() throws Exception {
		withBrandingUpdateNoDebug();
		_brandingChangedEventHandler.handleEvent(_eventHandlerContext);
		ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
		verify(_globalContextService, times(1)).saveBrandingAttributes(anyString(), captor.capture());
		verify(_globalContextDebugService, times(0)).writeToStore(anyString(), anyString());

		Map<String, Object> attributes = captor.getValue();
		Assert.assertNotNull(attributes);
		Assert.assertEquals("actionButtonColor", ((Map<String, Object>) attributes.get("test")).get("actionButtonColor"));
	}

	@Test
	public void testBrandingDelete() throws Exception {
		withBrandingDelete();
		_brandingChangedEventHandler.handleEvent(_eventHandlerContext);
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(_globalContextService, times(1)).deleteBranding(anyString(), anyString());
		verify(_globalContextDebugService, times(1)).writeToStore(anyString(), captor.capture());

		String name = captor.getValue();
		Assert.assertNotNull(name);
		Assert.assertEquals("test", name);
	}

	@Test
	public void testBrandingDeleteNoDebug() throws Exception {
		withBrandingDeleteNoDebug();
		_brandingChangedEventHandler.handleEvent(_eventHandlerContext);

		verify(_globalContextService, times(1)).deleteBranding(anyString(), anyString());
		verify(_globalContextDebugService, times(0)).writeToStore(anyString(), anyString());
	}

	private void withDummyBrandConfig() {
		_config = new BrandConfig();
		_config.setActionButtonColor("actionButtonColor");
		_config.setName("test");
	}

	private void withBrandingCreate() {
		when(_eventHandlerContext.getEvent()).thenReturn(getDummyEventDebug(EventType.BRANDING_UPDATED, _config));
		when(_globalContextService.findOneByTenant(anyString())).thenReturn(Optional.of(new GlobalContext("acme-solar")));
	}

	private void withBrandingUpdateNoDebug() {
		when(_eventHandlerContext.getEvent()).thenReturn(getDummyEvent(EventType.BRANDING_UPDATED, _config));
		when(_globalContextService.findOneByTenant(anyString())).thenReturn(Optional.of(new GlobalContext("acme-solar")));
	}

	private void withBrandingDelete() {
		when(_eventHandlerContext.getEvent()).thenReturn(getDummyEventDebug(EventType.BRANDING_DELETED, "test"));
		when(_globalContextService.findOneByTenant(anyString())).thenReturn(Optional.of(getGlobalContext()));
	}

	private void withBrandingDeleteNoDebug() {
		when(_eventHandlerContext.getEvent()).thenReturn(getDummyEvent(EventType.BRANDING_DELETED, "test"));
		when(_globalContextService.findOneByTenant(anyString())).thenReturn(Optional.of(getGlobalContext()));
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

	private GlobalContext getGlobalContext() {
		GlobalContext dummyContext = new GlobalContext("acme-solar");
		Map<String, Object> dummyAttributes = new HashMap<>();
		dummyAttributes.put("test", new HashMap<String, String>());
		dummyContext.setAttributes(dummyAttributes);
		return dummyContext;
	}

}
