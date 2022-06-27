/*
 * Copyright (c) 2017. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Provider;
import com.sailpoint.atlas.OrgData;
import com.sailpoint.atlas.OrgDataCache;
import com.sailpoint.atlas.OrgDataProvider;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.messaging.server.MessageHandlerContext;
import com.sailpoint.atlas.search.model.event.Event;
import com.sailpoint.atlas.service.AtomicMessageService;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.audit.AuditActions;
import com.sailpoint.audit.message.AuditEventPayload;
import com.sailpoint.audit.persistence.S3PersistenceManager;
import com.sailpoint.audit.service.model.AuditEventDTO;
import com.sailpoint.audit.service.model.AuditTimeAndIdsRecord;
import com.sailpoint.audit.service.normalizer.BaseNormalizer;
import com.sailpoint.audit.service.util.AuditUtil;
import com.sailpoint.audit.utils.TestUtils;
import com.sailpoint.audit.verification.AuditVerificationRequest;
import com.sailpoint.audit.verification.AuditVerificationService;
import com.sailpoint.mantis.core.service.CrudService;
import com.sailpoint.mantis.core.service.XmlService;
import com.sailpoint.utilities.JsonUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sailpoint.object.AuditEvent;
import sailpoint.object.Configuration;
import sailpoint.object.SailPointObject;

import java.util.Date;
import java.util.Optional;
import java.util.function.Consumer;

import static com.sailpoint.audit.event.AuditEventS3Handler.AUDITEVENT_PARQUET_TIMESTAMP;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/**
 * Created by mark.boyle on 10/2/17.
 */
public class AuditEventServiceTest {

	static Log log = LogFactory.getLog(AuditEventServiceTest.class);

	@Mock
	CrudService _crudService;

	@Mock
	XmlService _xmlService;

	@Mock
	AuditEvent _auditEvent;

	@Mock
	AuditEventPayload _auditEventPayload;

	@Mock
	EventNormalizerService _eventNormalizerService;

	@Mock
	AtomicMessageService _atomicMessageService;

	@Mock
	Provider<AtomicMessageService> _amsProvider;

	@Mock
	EventService _eventService;

	@Mock
	Provider<EventService> _esProvider;

	@Mock
	FeatureFlagService _featureFlagService;

	@Mock
	ObjectMapper _mapper;

	@Mock
	ObjectNode _objectNode;

	@Mock
	OrgDataCache _orgDataCache;

	@Mock
	OrgDataProvider _orgDataProvider;

	@Mock
	OrgData _orgData;

	@Mock
	Configuration _config;

	@Mock
	DeletedOrgsCacheService _deletedOrgsCache;

	@Mock
	MessageHandlerContext _messageHandlerContext;

	@Mock
	AuditUtil _util;

	@Mock
	AuditVerificationService _auditVerificationService;

	AuditEventService _sut;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		TestUtils.setDummyRequestContext();

		_auditEvent = new AuditEvent("TEST", "TEST-ACTION", "TEST-SRC");
		_auditEvent.setCreated(new Date());
		_auditEvent.setId("1234-DEAD-BEEF");
		_auditEvent.setLock("true");
		_auditEvent.setTrackingId("1234");
		_auditEvent.setApplication("[CC] application");
		_auditEvent.setExtended1("FOO");
		_auditEvent.setInstance("TEST");
		_auditEvent.setString1("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"+
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"+
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"+
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"+
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"+
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"+
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"+
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"+
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"+
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"+
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
		_auditEvent.setAccountName("CN=abc 370903,OU=house-lig-157860534510,OU=slpt-automation-dynamic,DC=TestAutomationAD,DC=local");
		_auditEvent.setInterface("appRequest");
		_auditEvent.setAttributeName("memberOf");
		_auditEvent.setAttributeValue("CN=ApprovalsBaseTest_entitlement_242,OU=lighthouse-lig-157860534510,OU=slpt-automation-dynamic,DC=TestAutomationAD,DC=local");
		_auditEvent.setString1("test.hostname.com");

		when(_xmlService.parse(any())).thenReturn(_auditEvent);
		when(_auditEventPayload.isUseAerStorage()).thenReturn(false);

		when(_amsProvider.get()).thenReturn(_atomicMessageService);
		when(_esProvider.get()).thenReturn(_eventService);

		_sut = new AuditEventService();
		_sut._crudService = _crudService;
		_sut._xmlService = _xmlService;
		_sut._featureFlagService = _featureFlagService;
		_sut._eventNormalizerService = _eventNormalizerService;
		_sut._atomicMessageService = _amsProvider;
		_sut._eventService = _esProvider;
		_sut._mapper = _mapper;
		_sut._orgDataCache = _orgDataCache;
		_sut._orgDataProvider = _orgDataProvider;
		_sut._deletedOrgsCache = _deletedOrgsCache;
		_sut._util = _util;
		_sut._auditVerificationService = _auditVerificationService;
		when(_mapper.createObjectNode()).thenReturn(_objectNode);
		when(_objectNode.put(anyString(), anyString())).thenReturn(_objectNode);
		when(_objectNode.toString()).thenReturn("A valid json string");

		when(_messageHandlerContext.getAttemptNumber()).thenReturn(10);

		// Disable persistence to S3 for unit tests of the Audit Event Service.
		when(_featureFlagService.getBoolean(eq(FeatureFlags.PLTDP_PERSIST_TO_CIS_MYSQL.name()), anyBoolean()))
				.thenReturn(true);
		when(_featureFlagService.getBoolean(eq(FeatureFlags.PLTDP_PERSIST_TO_S3_BY_TIME.name()), anyBoolean()))
				.thenReturn(false);

		// Build an S3 client for validation during unit tests.
		final int CLIENT_EXECUTION_TIMEOUT = 300000;
		AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
				.withClientConfiguration(
						new ClientConfiguration().withClientExecutionTimeout(CLIENT_EXECUTION_TIMEOUT)
				).build();
		S3PersistenceManager.overrideS3Client(s3Client);

		when(_crudService.findByName(any(), eq("CloudConfiguration"))).thenReturn(Optional.of(_config));
	}
	@Test
	public void testXmlMessage() {
		try {
			when(_auditEventPayload.hasXml()).thenReturn(true);
			when(_auditEventPayload.getAuditEventXml()).thenReturn(_auditEvent.toXml());
			_sut.processAuditMessage(_auditEventPayload, _messageHandlerContext);

			_auditEvent.setApplication("[CC] [sourceId123] application");
			_sut.processAuditMessage(_auditEventPayload, _messageHandlerContext);
		} catch (Exception ex) {
			fail("Exception in testingXmlMessage", ex);
		}
	}
	@Test
	public void testJsonMessage(){
		AuditEventDTO auditEventDTO = new AuditEventDTO(_auditEvent);
		 when(_auditEventPayload.hasXml()).thenReturn(false);
		 when(_auditEventPayload.getAuditEventJson()).thenReturn(auditEventDTO);
		 _sut.processAuditMessage(_auditEventPayload, _messageHandlerContext);
		 verify(_auditVerificationService).submitForVerification(any(AuditVerificationRequest.class));
	}

	@Test
	public void testStoreAuditEvent() {
		when(_crudService.save(anyObject())).thenAnswer(invocation -> invocation.getArgumentAt(0, SailPointObject.class));
		BaseNormalizer normalizer = new BaseNormalizer();
		Event event = normalizer.normalize(_auditEvent, _auditEvent.getInstance());
		AuditEvent auditEvent = _sut.storeAuditEvent(event);
		verify(_crudService, times(1)).save(anyObject());
	}

	@Test
	public void testJsonMessageWithPersistence(){
		AuditEventDTO auditEventDTO = new AuditEventDTO(_auditEvent);
		auditEventDTO.setApplication("application");
		auditEventDTO.setStack("CC");
		try {
			when(_auditEventPayload.isUseAerStorage()).thenReturn(true);
			when(_auditEventPayload.hasXml()).thenReturn(false);
			when(_auditEventPayload.getAuditEventJson()).thenReturn(auditEventDTO);
			when(_crudService.save(any(AuditEvent.class))).thenReturn(_auditEvent);

			_sut.processAuditMessage(_auditEventPayload, _messageHandlerContext);

			auditEventDTO.setApplication("sourceId [sourceId123]");
			_sut.processAuditMessage(_auditEventPayload, _messageHandlerContext);
		}
		catch(Exception ex){
			fail("Exception in testingJsonMessage", ex);
		}
	}

	@Test
	public void testJsonMessageWithNoApplicationWithPersistence(){
		AuditEventDTO auditEventDTO = new AuditEventDTO(_auditEvent);
		auditEventDTO.setApplication(null);
		auditEventDTO.setStack("CC");
		try {
			when(_auditEventPayload.isUseAerStorage()).thenReturn(true);
			when(_auditEventPayload.hasXml()).thenReturn(false);
			when(_auditEventPayload.getAuditEventJson()).thenReturn(auditEventDTO);
			when(_crudService.save(any(AuditEvent.class))).thenReturn(_auditEvent);
			_sut.processAuditMessage(_auditEventPayload, _messageHandlerContext);

			//second test
			auditEventDTO.setApplication("[application]");
			_sut.processAuditMessage(_auditEventPayload, _messageHandlerContext);
		}
		catch(Exception ex){
			fail("Exception in testingJsonMessage", ex);
		}
	}

	@Test
	public void testFeatureFlag(){

		try {
			when(_auditEventPayload.hasXml()).thenReturn(true);
			when(_auditEventPayload.getAuditEventXml()).thenReturn(_auditEvent.toXml());
			when(_auditEventPayload.isUseAerStorage()).thenReturn(true);
			AuditEvent savedEvent = new AuditEvent();
			savedEvent.setId("mock-audit-event");
			when(_crudService.save(anyObject())).thenReturn(savedEvent);
		} catch(Exception ex){
			fail("Exception in testFeatureFlag", ex);
		}

		_sut.processAuditMessage(_auditEventPayload, _messageHandlerContext);
		verify(_eventNormalizerService).normalize(any(AuditEvent.class));
	}

	@Test
	public void storeAuditEventNoOrg(){

		when(_auditEventPayload.hasXml()).thenReturn(false);
		when(_auditEventPayload.getAuditEventJson()).thenReturn(new AuditEventDTO(_auditEvent));
		when(_auditEventPayload.isUseAerStorage()).thenReturn(true);
		when(_crudService.save(any(AuditEvent.class))).thenThrow(new IllegalArgumentException("Error: Table 'spt_audit_event' doesn't exist in the DB"));
		when(_orgDataProvider.find(eq("acme-solar"))).thenReturn(Optional.empty());

		_sut.processAuditMessage(_auditEventPayload, _messageHandlerContext);

		verify(_orgDataCache).evict(eq("acme-solar"));
		verify(_eventNormalizerService, never()).normalize(any(AuditEvent.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void storeAuditEventError(){

		when(_auditEventPayload.hasXml()).thenReturn(false);
		when(_auditEventPayload.getAuditEventJson()).thenReturn(new AuditEventDTO(_auditEvent));
		when(_auditEventPayload.isUseAerStorage()).thenReturn(true);
		when(_crudService.save(any(AuditEvent.class))).thenThrow(new IllegalArgumentException(""));
		when(_orgDataProvider.find(eq("acme-solar"))).thenReturn(Optional.of(new OrgData()));

		_sut.processAuditMessage(_auditEventPayload, _messageHandlerContext);

		verify(_orgDataCache, never()).evict(eq("acme-solar"));
	}

	@Test(expected = NullPointerException.class)
	public void logItError() {

		AuditEventDTO auditEventDTO = new AuditEventDTO(_auditEvent);
		auditEventDTO.setApplication("application");
		auditEventDTO.setStack("CC");

		when(_auditEventPayload.hasXml()).thenReturn(false);
		when(_auditEventPayload.getAuditEventJson()).thenReturn(auditEventDTO);

		when(_mapper.createObjectNode()).thenReturn(null);

		_sut.processAuditMessage(_auditEventPayload, _messageHandlerContext);
	}

	@Test
	public void testFirehoseRetry() {
		Date d = new Date();

		Event event = new Event();
		event.setId("id0");
		event.setCreated(d);
		event.setAction(AuditActions.ACCESS_PROFILE_CREATE_PASSED.toString());
		when(_eventNormalizerService.normalize(any())).thenReturn(event);

		when(_orgDataProvider.find(eq("acme-solar"))).thenReturn(Optional.of(_orgData));
		when(_orgData.getAttribute(AuditEventService.FIREHOSE_ATTRIBUTE_NAME)).thenReturn("audit-events-acme-solar");

		when(_config.getId()).thenReturn("id0");

		when(_orgDataProvider.find(anyString())).thenThrow(new RuntimeException());
		when(_featureFlagService.getBoolean(any())).thenReturn(true);

		_sut.normalizeAndEmit(_auditEvent);

		verify(_util, times(1)).publishAuditEvent(any(Event.class),anyBoolean());
	}

	@Test
	public void testSkipPublishWhenUseAerStorageIsFalse() {
		BaseNormalizer normalizer = new BaseNormalizer();
		Event event = normalizer.normalize(_auditEvent, _auditEvent.getInstance());
		when(_eventNormalizerService.normalize(any())).thenReturn(event);

		AuditEventDTO auditEventDTO = new AuditEventDTO(_auditEvent);

		when(_auditEventPayload.hasXml()).thenReturn(false);
		when(_auditEventPayload.getAuditEventJson()).thenReturn(auditEventDTO);

		//Test
		_sut.processAuditMessage(_auditEventPayload, _messageHandlerContext);

		//Verification
		verify(_util, times(0)).publishAuditEvent(any(Event.class),anyBoolean());
	}

	@Test
	public void testPublishAllAuditEvents() {
		BaseNormalizer normalizer = new BaseNormalizer();
		Event event = normalizer.normalize(_auditEvent, _auditEvent.getInstance());
		when(_eventNormalizerService.normalize(any())).thenReturn(event);

		AuditEventDTO auditEventDTO = new AuditEventDTO(_auditEvent);

		when(_auditEventPayload.hasXml()).thenReturn(false);
		when(_auditEventPayload.getAuditEventJson()).thenReturn(auditEventDTO);
		when(_auditEventPayload.isUseAerStorage()).thenReturn(true);

		AuditEvent savedEvent = new AuditEvent();
		savedEvent.setId("mock-audit-event");
		when(_crudService.save(anyObject())).thenReturn(savedEvent);

		//Test
		_sut.processAuditMessage(_auditEventPayload, _messageHandlerContext);

		//Verification
		verify(_util, times(1)).publishAuditEvent(any(Event.class),anyBoolean());
	}

	@Test
	public void testAddCheckpoint() {
		when(_config.getString(eq(AUDITEVENT_PARQUET_TIMESTAMP))).thenReturn(JsonUtil.toJson(new AuditTimeAndIdsRecord()));
		_sut.addCheckpoint(new Date().getTime(), "id0", AUDITEVENT_PARQUET_TIMESTAMP);
		verify(_crudService, times(0)).withTransactionLock(any(), anyString(), any());

		when(_config.getString(eq(AUDITEVENT_PARQUET_TIMESTAMP))).thenReturn(null);
		_sut.addCheckpoint(new Date().getTime(), "id0", AUDITEVENT_PARQUET_TIMESTAMP);

		verify(_crudService, times(1)).withTransactionLock(any(), anyString(), any());
	}

	@Test
	public void testUpdateCheckpoint() {
		when(_config.getString(eq(AUDITEVENT_PARQUET_TIMESTAMP))).thenReturn(JsonUtil.toJson(new AuditTimeAndIdsRecord()));

		_sut.updateCheckpoint(new Date().getTime(), "id0", AUDITEVENT_PARQUET_TIMESTAMP);
		verify(_crudService, times(1)).withTransactionLock(any(), anyString(), any());
	}

	@Test
	public void testWriteFirehoseStatus() {
		long created = new Date().getTime();

		Consumer<Configuration> config = _sut.writeFirehoseStatus(created, "id0", AUDITEVENT_PARQUET_TIMESTAMP);

		when(_config.getString(eq(AUDITEVENT_PARQUET_TIMESTAMP))).thenReturn(JsonUtil.toJson(new AuditTimeAndIdsRecord()));
		config.accept(_config);
		verify(_crudService, times(1)).save(any());
	}

	@Test
	public void testSaveFirehoseStatus() {
		long created = new Date().getTime();

		Consumer<Configuration> config = _sut.saveFirehoseStatus(created, "id0", AUDITEVENT_PARQUET_TIMESTAMP);

		when(_config.getString(eq(AUDITEVENT_PARQUET_TIMESTAMP))).thenReturn(JsonUtil.toJson(new AuditTimeAndIdsRecord()));
		config.accept(_config);
		verify(_crudService, times(0)).save(any());

		when(_config.getString(eq(AUDITEVENT_PARQUET_TIMESTAMP))).thenReturn(null);
		config.accept(_config);
		verify(_crudService, times(1)).save(any());

		when(_config.getString(eq(AUDITEVENT_PARQUET_TIMESTAMP)))
				.thenReturn(JsonUtil.toJson(AuditTimeAndIdsRecord.of(created, "id1")));
		config.accept(_config);
		verify(_crudService, times(2)).save(any());

	}

	@Test
	public void testSaveToCisAndS3Persistence() {

		when(_featureFlagService.getBoolean(anyString(), anyBoolean())).thenReturn(true);
		when(_featureFlagService.getBoolean(Matchers.any(FeatureFlags.class), anyBoolean())).thenReturn(true);

		_auditEvent = new AuditEvent("TEST", "TEST-ACTION", "TEST-SRC");
		_auditEvent.setCreated(new Date());
		_auditEvent.setId("1234-DEAD-BEEF");
		_auditEvent.setLock("true");
		_auditEvent.setTrackingId("1234");
		_auditEvent.setApplication("[CC] application");
		_auditEvent.setExtended1("FOO");
		_auditEvent.setInstance("TEST");

		when(_crudService.save(anyObject())).thenReturn(_auditEvent);

		AuditEvent storedAuditEvent = _sut.saveToDurablePersistence(_auditEvent);
		assertNotNull(storedAuditEvent, "Should get an AuditEvent back.");

		RequestContext.ensureGet().getTenantId().ifPresent((tenantId) -> {
			S3PersistenceManager.deleteAuditEventsForTenant(tenantId);
		});

	}

	@Test
	public void testSaveToOnlyS3Persistence() {

		when(_featureFlagService.getBoolean(eq(FeatureFlags.PLTDP_PERSIST_TO_CIS_MYSQL), anyBoolean())).thenReturn(false);
		when(_featureFlagService.getBoolean(eq(FeatureFlags.PLTDP_PERSIST_TO_CIS_MYSQL.name()), anyBoolean())).thenReturn(false);

		when(_featureFlagService.getBoolean(eq(FeatureFlags.PLTDP_PERSIST_TO_S3_BY_TIME), anyBoolean())).thenReturn(true);
		when(_featureFlagService.getBoolean(eq(FeatureFlags.PLTDP_PERSIST_TO_S3_BY_TIME.name()), anyBoolean())).thenReturn(true);

		_auditEvent = new AuditEvent("TEST", "TEST-ACTION", "TEST-SRC");
		_auditEvent.setCreated(new Date());
		_auditEvent.setId("1234-DEAD-BEEF");
		_auditEvent.setLock("true");
		_auditEvent.setTrackingId("1234");
		_auditEvent.setApplication("[CC] application");
		_auditEvent.setExtended1("FOO");
		_auditEvent.setInstance("TEST");

		when(_crudService.save(anyObject())).thenReturn(_auditEvent);

		AuditEvent storedAuditEvent = _sut.saveToDurablePersistence(_auditEvent);
		assertNotNull(storedAuditEvent, "Should get an AuditEvent back.");

		RequestContext.ensureGet().getTenantId().ifPresent((tenantId) -> {
			S3PersistenceManager.deleteAuditEventsForTenant(tenantId);
		});

	}


}
