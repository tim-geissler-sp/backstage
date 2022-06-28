/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.context.event;

import com.sailpoint.atlas.OrgData;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.discovery.ServiceLocation;
import com.sailpoint.atlas.discovery.ServiceLocator;
import com.sailpoint.atlas.idn.IdnOrgData;
import com.sailpoint.atlas.idn.RestClientProvider;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.mantisclient.BaseRestClient;
import com.sailpoint.notification.context.common.model.GlobalContext;
import com.sailpoint.notification.context.service.GlobalContextService;
import org.apache.commons.logging.Log;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *  Tests for OrgLifecycleEventHandler
 */
public class OrgLifecycleEventHandlerTest {

	private OrgLifecycleEventHandler _orgLifecycleEventHandler;

	@Mock
	private EventHandlerContext _eventHandlerContext;

	@Mock
	private GlobalContextService _globalContextService;

	@Mock
	private ServiceLocator _serviceLocator;

	@Mock
	private RestClientProvider _restClientProvider;

	@Mock
	private BaseRestClient _client;

	@Mock
	private Log _log;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		when(_restClientProvider.getInternalRestClient(any())).thenReturn(_client);
		when(_client.get(any())).thenReturn(null);
		_orgLifecycleEventHandler = new OrgLifecycleEventHandler(_globalContextService, _serviceLocator, _restClientProvider, _log);
		Event dummyEvent = EventBuilder.withTypeAndContent("ORG_UPGRADED", null)
				.addHeader(EventHeaders.ORG, "acme-solar")
				.build();
		when(_eventHandlerContext.getEvent()).thenReturn(dummyEvent);
	}

	@After
	public void cleanup() {
		RequestContext.set(null);
	}

	@Test
	public void testVanityDomainFromOrgDataProvider() throws Exception {
		givenValidOrgData();
		givenNoContextExists();

		_orgLifecycleEventHandler.handleEvent(_eventHandlerContext);
		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
		verify(_globalContextService, times(1)).saveAttribute(anyString(), keyCaptor.capture(), valueCaptor.capture());

		thenVerifyVanityDomain(keyCaptor, valueCaptor);
	}

	@Test
	public void testVanityDomainFromServiceLocator() throws Exception {
		givenOrgDataWithoutVanity();
		givenNoContextExists();
		givenValidServiceLocator();

		_orgLifecycleEventHandler.handleEvent(_eventHandlerContext);
		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
		verify(_globalContextService, times(1)).saveAttribute(anyString(), keyCaptor.capture(), valueCaptor.capture());

		thenVerifyNonVanityDomain(keyCaptor, valueCaptor);
	}

	@Test
	public void testOverwriteGC() throws Exception {
		givenValidOrgData();
		givenValidContextExists();

		_orgLifecycleEventHandler.handleEvent(_eventHandlerContext);
		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
		verify(_globalContextService, times(1)).saveAttribute(anyString(), keyCaptor.capture(), valueCaptor.capture());

		thenVerifyVanityDomain(keyCaptor, valueCaptor);
	}

	@Test
	public void testDelete() throws Exception {
		givenDeleteEvent();

		_orgLifecycleEventHandler.handleEvent(_eventHandlerContext);
		verify(_globalContextService, times(1)).deleteByTenant(any());
	}

	@Test
	public void testCreate() throws Exception {
		givenCreateEvent();
		givenValidOrgData();
		givenValidContextExists();

		_orgLifecycleEventHandler.handleEvent(_eventHandlerContext);
		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
		verify(_globalContextService, times(1)).saveAttribute(anyString(), keyCaptor.capture(), valueCaptor.capture());

		thenVerifyVanityDomain(keyCaptor, valueCaptor);
		verify(_log, times(1)).warn(any());
	}

	@Test
	public void testBrandingConfigReconcile() {
		givenValidOrgData();

		when(_client.get(any())).thenReturn(brandingResponse);
		_orgLifecycleEventHandler.reconcileBrandingConfiguration("acme-solar");
		ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
		verify(_globalContextService, times(1)).saveBrandingAttributes(anyString(), mapCaptor.capture());
		assertTrue(mapCaptor.getValue().containsKey("brand1"));
	}

	@Test
	public void testBrandingConfigReconcileNull() {
		givenValidOrgData();

		when(_client.get(any())).thenReturn(null);
		_orgLifecycleEventHandler.reconcileBrandingConfiguration("acme-solar");
		ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
		verify(_globalContextService, times(0)).saveBrandingAttributes(anyString(), mapCaptor.capture());
	}

	@Test
	public void testBrandingConfigReconcileException() {
		givenValidOrgData();

		Mockito.doThrow(IllegalArgumentException.class).when(_client).get(any());
		_orgLifecycleEventHandler.reconcileBrandingConfiguration("acme-solar");
		ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
		verify(_globalContextService, times(0)).saveBrandingAttributes(anyString(), mapCaptor.capture());
		verify(_log, times(1)).error(anyString(), any());
	}

	private void givenCreateEvent() {
		Event dummyEvent = EventBuilder.withTypeAndContent("ORG_CREATED", null)
				.addHeader(EventHeaders.ORG, "acme-solar")
				.build();
		when(_eventHandlerContext.getEvent()).thenReturn(dummyEvent);
	}

	private void givenDeleteEvent() {
		Event dummyEvent = EventBuilder.withTypeAndContent("ORG_DELETED", null)
				.addHeader(EventHeaders.ORG, "acme-solar")
				.build();
		when(_eventHandlerContext.getEvent()).thenReturn(dummyEvent);
	}

	private void givenValidServiceLocator() {
		ServiceLocation location = new ServiceLocation("CC", "https://product.url");
		when(_serviceLocator.ensureFind(anyString(), anyString())).thenReturn(location);
	}

	private void thenVerifyVanityDomain(ArgumentCaptor<String> keyCaptor, ArgumentCaptor<String> valueCaptor) {
		Assert.assertEquals(OrgLifecycleEventHandler.PRODUCT_URL, keyCaptor.getValue());
		Assert.assertEquals("https://product.url", valueCaptor.getValue());
	}

	private void thenVerifyNonVanityDomain(ArgumentCaptor<String> keyCaptor, ArgumentCaptor<String> valueCaptor) {
		Assert.assertEquals(OrgLifecycleEventHandler.PRODUCT_URL, keyCaptor.getValue());
		Assert.assertEquals("https://product.url/acme-solar", valueCaptor.getValue());
	}

	private void givenNoContextExists() {
		when(_globalContextService.findOneByTenant(anyString())).thenReturn(Optional.empty());
	}

	private void givenValidContextExists() {
		GlobalContext globalContext = new GlobalContext("acme-solar");
		HashMap<String, Object> attributes = new HashMap();
		attributes.put(OrgLifecycleEventHandler.PRODUCT_URL, "old value");
		globalContext.setAttributes(attributes);
		when(_globalContextService.findOneByTenant(anyString())).thenReturn(Optional.of(globalContext));
	}

	private void givenValidOrgData() {
		OrgData orgData = new OrgData();
		IdnOrgData.setVanityDomain(orgData, "product.url");
		RequestContext rc = new RequestContext();
		rc.setOrgData(orgData);
		RequestContext.set(rc);
	}

	private void givenOrgDataWithoutVanity() {
		OrgData orgData = new OrgData();
		IdnOrgData.setVanityDomain(orgData, null);
		RequestContext rc = new RequestContext();
		rc.setOrgData(orgData);
		RequestContext.set(rc);
	}

	final String brandingResponse = "{" +
			"\"items\": [" +
			"{" +
			"\"name\": \"brand1\"," +
			"\"productName\": \"Otto\"," +
			"\"standardLogoURL\": null," +
			"\"narrowLogoURL\": null," +
			"\"navigationColor\": \"FFEE66\"," +
			"\"actionButtonColor\": \"20B2DE\"," +
			"\"activeLinkColor\": \"011E69\"," +
			"\"emailFromAddress\": null," +
			"\"loginInformationalMessage\": null" +
			"}," +
			"{" +
			"\"name\": \"default\"," +
			"\"productName\": \"Nauto2\"," +
			"\"standardLogoURL\": null," +
			"\"narrowLogoURL\": null," +
			"\"navigationColor\": \"011E69\"," +
			"\"actionButtonColor\": \"20B2DE\"," +
			"\"activeLinkColor\": \"20B2DE\"," +
			"\"emailFromAddress\": \"narayanan.srinivasan@sailpoint.com\"," +
			"\"loginInformationalMessage\": null" +
			"}" +
			"]," +
			"\"count\": 2" +
			"}";
}
