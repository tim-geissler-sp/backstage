/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.event;

import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.orgpreferences.repository.TenantPreferencesRepository;
import com.sailpoint.notification.orgpreferences.repository.TenantUserPreferencesRepository;
import com.sailpoint.notification.userpreferences.mapper.UserPreferencesMapper;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OrgLifecycleEventHandler.
 */
@RunWith(MockitoJUnitRunner.class)
public class OrgLifecycleEventHandlerTest {

	@Mock
	UserPreferencesRepository _userPreferencesRepository;

	@Mock
	TenantPreferencesRepository _tenantPreferencesRepository;

	@Mock
	TenantUserPreferencesRepository _tenantUserPreferencesRepository;

	@Mock
	EventHandlerContext _eventHandlerContext;

	@Captor
	ArgumentCaptor<String> _uprArgumentCaptor;

	private final int CALLED_ONCE = 1;

	private final int NEVER_CALLED = 0;

	private OrgLifecycleEventHandler _orgLifecycleEventHandler;

	private String _pod;

	private String _org;

	private Event _orgLifecycleEvent;

	@Before
	public void setUp() {
		_orgLifecycleEventHandler = new OrgLifecycleEventHandler(_userPreferencesRepository, _tenantPreferencesRepository, _tenantUserPreferencesRepository);
	}

	@Test
	public void deleteByTenantTest() {
		givenPodAndOrg("dev", "acme-solar");
		givenOrgLifecycleEvent(EventType.ORG_DELETED);
		givenEventHandlerGetEventMock();

		// When handle event
		_orgLifecycleEventHandler.handleEvent(_eventHandlerContext);

		thenVerifyDeleteByTenant(CALLED_ONCE);
		thenUprArgumentCaptor(UserPreferencesMapper.toHashKey("dev", "acme-solar"));
	}

	@Test
	public void noTenantHeaderTest() {
		givenOrgLifecycleEvent(EventType.ORG_DELETED);
		givenEventHandlerGetEventMock();

		// When handle event
		_orgLifecycleEventHandler.handleEvent(_eventHandlerContext);

		thenVerifyDeleteByTenant(NEVER_CALLED);
	}

	@Test
	public void nonOrgDeleteEventTypeTest() {
		givenPodAndOrg("dev", "acme-solar");
		givenOrgLifecycleEvent("ORG_CREATED");
		givenEventHandlerGetEventMock();

		// When handle event
		_orgLifecycleEventHandler.handleEvent(_eventHandlerContext);

		thenVerifyDeleteByTenant(NEVER_CALLED);
	}

	private void givenOrgLifecycleEvent(String eventType) {
		_orgLifecycleEvent = EventBuilder
				.withTypeAndContentJson(eventType, "{}")
				.addHeader(EventHeaders.POD, _pod)
				.addHeader(EventHeaders.ORG, _org)
				.build();
	}

	private void givenPodAndOrg(String pod, String org) {
		_pod = pod;
		_org = org;
	}

	private void givenEventHandlerGetEventMock() {
		when(_eventHandlerContext.getEvent()). thenReturn(_orgLifecycleEvent);
	}

	private void thenVerifyDeleteByTenant(int invocations) {
		verify(_userPreferencesRepository, times(invocations)).deleteByTenant(_uprArgumentCaptor.capture());
		verify(_tenantPreferencesRepository, times(invocations)).bulkDeleteForTenant(_uprArgumentCaptor.capture());
		verify(_tenantUserPreferencesRepository, times(invocations)).bulkDeleteForTenant(_uprArgumentCaptor.capture());
	}

	private void thenUprArgumentCaptor(String tenant) {
		String capturedTenant = _uprArgumentCaptor.getValue();
		Assert.assertEquals(tenant, capturedTenant);
	}

}
