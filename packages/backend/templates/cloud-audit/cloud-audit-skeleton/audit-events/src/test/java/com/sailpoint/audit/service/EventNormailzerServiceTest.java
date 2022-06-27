/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sailpoint.atlas.search.model.event.Event;
import com.sailpoint.audit.AuditActions;
import com.sailpoint.audit.AuditEventConstants;
import com.sailpoint.audit.service.mapping.DomainAuditEventsUtil;
import com.sailpoint.audit.service.normalizer.BaseNormalizer;
import com.sailpoint.audit.service.normalizer.NonWhitelistedNormalizer;
import com.sailpoint.audit.service.normalizer.NormalizerFactory;
import com.sailpoint.audit.utils.TestUtils;
import com.sailpoint.mantis.core.service.model.AuditEventActions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sailpoint.object.Attributes;
import sailpoint.object.AuditEvent;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class EventNormailzerServiceTest {

	@Mock
	ObjectMapper _mapper;

	@Mock
	ObjectNode _objectNode;

	@Mock
	NormalizerFactory _normalizerFactory;

	@Mock
	DomainAuditEventsUtil _domainAuditEventsUtil;

	EventNormalizerService _eventNormalizerService;

	@Before
	public void setup(){
		MockitoAnnotations.initMocks(this);
		TestUtils.setDummyRequestContext();

		_eventNormalizerService = new EventNormalizerService();
		_eventNormalizerService._mapper = _mapper;
		_eventNormalizerService._normalizerFactory = _normalizerFactory;
		_eventNormalizerService._domainEventActions = _domainAuditEventsUtil;

		when(_normalizerFactory.getNormalizer(any(AuditEvent.class))).thenReturn(new BaseNormalizer());

		when(_mapper.createObjectNode()).thenReturn(_objectNode);
		when(_objectNode.put(anyString(), anyString())).thenReturn(_objectNode);
		when(_objectNode.toString()).thenReturn("A valid json string");
	}

	@Test
	public void testAuthTypeEvents(){
		AuditEvent ae = new AuditEvent("Actor", "AUTHENTICATION-103");
		ae.setCreated(new Date());
		ae.setInstance("AUTH");
		ae.setApplication("[tpe] foobar [source-12345]");
		Map<String,Object> attrs = new HashMap<>();
		attrs.put("oldValue",123);
		attrs.put("newValue",true);
		attrs.put("reviewer.comments", "I like turtles");
		ae.setAttributes(new Attributes<>(attrs));

		Event event = _eventNormalizerService.normalize(ae);

		assertNotNull(event);
		assertEquals("AUTH", event.getType());
		assertEquals("123", event.getAttributes().get("oldValue"));
		assertEquals("true", event.getAttributes().get("newValue"));
		assertEquals("I like turtles", event.getAttributes().get("reviewer.comments"));
		assertEquals("foobar", event.getAttributes().get("sourceName"));
		assertEquals("source-12345", event.getAttributes().get("sourceId"));
		assertEquals("tpe", event.getStack());
	}

	@Test
	public void testUnRecognizedType() {
		AuditEvent ae = new AuditEvent("Actor", "Action");
		ae.setCreated(new Date());
		ae.setInstance("RANDOM");
		when(_normalizerFactory.getNormalizer(any(AuditEvent.class))).thenReturn(new NonWhitelistedNormalizer());
		Event event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);

		ae.setInstance("");
		event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
	}

	@Test
	public void testSourceTypeEvents(){
		AuditEvent ae = new AuditEvent("Actor", "SOURCE_CREATE");
		ae.setCreated(new Date());
		ae.setInstance("NONE");
		ae.setApplication("[CC] foobar [source-12345]");

		Event event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
		assertEquals("SOURCE_MANAGEMENT", event.getType());
		assertEquals("SOURCE_CREATE_PASSED", event.getTechnicalName());
		assertEquals("Create Source Passed", event.getName());
	}

	@Test
	public void testServiceDeskIntegrationEvents(){
		AuditEvent ae = new AuditEvent("Actor", "SERVICE_DESK_INTEGRATION_CREATED");
		ae.setCreated(new Date());
		ae.setInstance("NONE");
		ae.setApplication("[CC] foobar [source-12345]");

		Event event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
		assertEquals("SOURCE_MANAGEMENT", event.getType());
		assertEquals("SERVICE_DESK_INTEGRATION_CREATE_PASSED", event.getTechnicalName());
		assertEquals("Create Service Desk Integration Passed", event.getName());

		ae = new AuditEvent("Actor", "SERVICE_DESK_INTEGRATION_UPDATED");

		event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
		assertEquals("SOURCE_MANAGEMENT", event.getType());
		assertEquals("SERVICE_DESK_INTEGRATION_UPDATE_PASSED", event.getTechnicalName());
		assertEquals("Update Service Desk Integration Passed", event.getName());

		ae = new AuditEvent("Actor", "SERVICE_DESK_INTEGRATION_DELETED");

		event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
		assertEquals("SOURCE_MANAGEMENT", event.getType());
		assertEquals("SERVICE_DESK_INTEGRATION_DELETE_PASSED", event.getTechnicalName());
		assertEquals("Delete Service Desk Integration Passed", event.getName());
	}

	@Test
	public void testServiceDeskIntegrationEventsFailed(){
		AuditEvent ae = new AuditEvent("Actor", "SERVICE_DESK_INTEGRATION_CREATE_FAILED");
		ae.setCreated(new Date());
		ae.setInstance("NONE");
		ae.setApplication("[CC] foobar [source-12345]");

		Event event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
		assertEquals("SOURCE_MANAGEMENT", event.getType());
		assertEquals("SERVICE_DESK_INTEGRATION_CREATE_FAILED", event.getTechnicalName());
		assertEquals("Create Service Desk Integration Failed", event.getName());

		ae = new AuditEvent("Actor", "SERVICE_DESK_INTEGRATION_UPDATE_FAILED");

		event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
		assertEquals("SOURCE_MANAGEMENT", event.getType());
		assertEquals("SERVICE_DESK_INTEGRATION_UPDATE_FAILED", event.getTechnicalName());
		assertEquals("Update Service Desk Integration Failed", event.getName());

		ae = new AuditEvent("Actor", "SERVICE_DESK_INTEGRATION_DELETE_FAILED");

		event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
		assertEquals("SOURCE_MANAGEMENT", event.getType());
		assertEquals("SERVICE_DESK_INTEGRATION_DELETE_FAILED", event.getTechnicalName());
		assertEquals("Delete Service Desk Integration Failed", event.getName());
	}

	@Test
	public void testCertificationsEventsEnabled(){
		AuditEvent ae = new AuditEvent("Actor", AuditActions.CERT_CAMPAIGN_COMPLETE.toString());
		ae.setCreated(new Date());
		ae.setInstance("NONE");
		ae.setApplication("[wps] foobar [source-12345]");
		Event event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
		assertEquals("CERTIFICATION", event.getType());
		assertEquals("CERTIFICATION_CAMPAIGN_COMPLETE_PASSED", event.getTechnicalName());
		assertEquals("Complete Certification Campaign Passed", event.getName());
	}

	@Test
	public void testAccessRequestEvents(){
		AuditEvent ae = new AuditEvent("Actor", "AccessRequestRequested");
		ae.setCreated(new Date());
		ae.setInstance("NONE");
		ae.setApplication("[wps] foobar [source-12345]");

		Event event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
		assertEquals("ACCESS_REQUEST", event.getType());
		assertEquals("ACCESS_REQUEST_STARTED", event.getTechnicalName());
		assertEquals("Request Access Started", event.getName());
	}

	@Test
	public void testUserManangementEvents(){
		AuditEvent ae = new AuditEvent("Actor", AuditActions.USER_ACTIVATE.toString());
		ae.setCreated(new Date());
		ae.setInstance("NONE");
		ae.setApplication("[CC] foobar [source-12345]");

		Event event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
		assertEquals("USER_MANAGEMENT", event.getType());
		assertEquals("USER_ACTIVATE_PASSED", event.getTechnicalName());
		assertEquals("Activate User Passed", event.getName());
	}

	@Test
	public void testPasswordInterceptAction(){
		AuditEvent ae = new AuditEvent("Actor", AuditActions.SOURCE_PASSWORD_INTERCEPT_IGNORED.toString());
		ae.setCreated(new Date());
		ae.setInstance("NONE");
		ae.setApplication("[CC] foobar [source-12345]");

		Event event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
		assertEquals("PASSWORD_ACTIVITY", event.getType());

		ae = new AuditEvent("Actor", AuditActions.SOURCE_PASSWORD_INTERCEPT_PROCESSED.toString());
		ae.setCreated(new Date());
		ae.setInstance("NONE");
		ae.setApplication("[CC] foobar [source-12345]");

		event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
		assertEquals("PASSWORD_ACTIVITY", event.getType());
		assertEquals("SOURCE_PASSWORD_INTERCEPT_PROCESSED", event.getTechnicalName());
		assertEquals("Intercept Source Password Processed", event.getName());
	}

	@Test
	public void testProvisioningEvents(){
		AuditEvent ae = new AuditEvent("Actor", AuditEvent.ManualChange);
		ae.setCreated(new Date());
		ae.setInstance("NONE");
		ae.setApplication("[wps] foobar [source-12345]");

		Event event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
		assertTrue(event.getType().contains("PROVISIONING"));
		assertEquals("ACCOUNT_MANUAL_CHANGE_COMPLETE_PASSED", event.getTechnicalName());
		assertEquals("Complete Account Manual Change Passed", event.getName());
	}

	@Test
	public void testAccessItemEvents(){
		AuditEvent ae = new AuditEvent("Actor", AuditEventActions.AddEntitlement);
		ae.setCreated(new Date());
		ae.setInstance("NONE");
		ae.setApplication("[wps] foobar [source-12345]");

		Event event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
		assertTrue(event.getType().contains("ACCESS_ITEM"));
		assertEquals("ENTITLEMENT_ADD_PASSED", event.getTechnicalName());
		assertEquals("Add Entitlement Passed", event.getName());
	}

	@Test
	public void testGenericCrudEvents(){
		AuditEvent ae = new AuditEvent("Actor", "create");
		ae.setCreated(new Date());
		ae.setInstance("NONE");
		ae.setApplication("[mantis] foobar [source-12345]");
		ae.setTarget("Cloud Role:Demo Role 1");

		Event event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
		assertTrue(event.getType().contains("ACCESS_ITEM"));
		assertEquals("ROLE_CREATE_PASSED", event.getTechnicalName());
		assertEquals("Create Role Passed", event.getName());

		ae.setAction("update");
		ae.setTarget("Lifecycle State:Inactive");
		event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
		assertTrue(event.getType().contains("ACCESS_ITEM"));
		assertEquals("LIFECYCLE_STATE_UPDATE_PASSED", event.getTechnicalName());
		assertEquals("Update Lifecycle State Passed", event.getName());

		ae.setAction("delete");
		ae.setTarget("Access Profile:Duo TestAP");
		event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
		assertTrue(event.getType().contains("ACCESS_ITEM"));
		assertEquals("ACCESS_PROFILE_DELETE_PASSED", event.getTechnicalName());
		assertEquals("Delete Access Profile Passed", event.getName());

		ae.setAction("delete");
		ae.setTarget("Invalid:invalid");
		event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
	}

	@Test
	public void tesIdentityManagementEvents(){
		AuditEvent ae = new AuditEvent("Actor", AuditActions.IdentityDirectCreateFailure.toString());
		ae.setCreated(new Date());
		ae.setInstance("NONE");
		ae.setApplication("[tpe] foobar [source-12345]");

		Event event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
		assertTrue(event.getType().contains("IDENTITY_MANAGEMENT"));
		assertEquals("API_IDENTITY_CREATE_FAILED", event.getTechnicalName());
		assertEquals("Create Api Identity Failed", event.getName());
	}

	@Test
	public void tesSystemConfigEvents(){
		AuditEvent ae = new AuditEvent("Actor", AuditActions.BRANDING_CREATE.toString());
		ae.setCreated(new Date());
		ae.setInstance("NONE");
		ae.setApplication("[tpe] foobar [source-12345]");

		Event event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
		assertTrue(event.getType().contains("SYSTEM_CONFIG"));
		assertEquals("BRANDING_CREATE_PASSED", event.getTechnicalName());
		assertEquals("Create Branding Passed", event.getName());
	}

	@Test
	public void tesAuthEvents(){
		AuditEvent ae = new AuditEvent("Actor", AuditEventConstants.AUTHENTICATION_REQUEST_PASSED);
		ae.setCreated(new Date());
		ae.setInstance("NONE");
		ae.setApplication("[CC] foobar [source-12345]");

		Event event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
		assertTrue(event.getType().contains("AUTH"));
		assertEquals("AUTHENTICATION_REQUEST_PASSED", event.getTechnicalName());
		assertEquals("Request Authentication Passed", event.getName());
	}

	@Test
	public void tesSSOEvents(){
		AuditEvent ae = new AuditEvent("Actor", AuditActions.APP_LAUNCH_SAML.toString());
		ae.setCreated(new Date());
		ae.setInstance("LAUNCH");
		ae.setApplication("[CC] foobar [source-12345]");

		Event event = _eventNormalizerService.normalize(ae);
		assertNotNull(event);
		assertTrue(event.getType().contains("SSO"));
		assertEquals("APP_SAML_LAUNCH_PASSED", event.getTechnicalName());
		assertEquals("Launch App Saml Passed", event.getName());
	}
}
