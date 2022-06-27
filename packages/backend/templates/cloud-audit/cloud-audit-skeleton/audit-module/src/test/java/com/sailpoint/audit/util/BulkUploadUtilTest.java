/*
 * Copyright (C) 2020. SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.util;

import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.OrgData;
import com.sailpoint.atlas.OrgDataProvider;
import com.sailpoint.atlas.search.model.event.Event;
import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.audit.service.BulkUploadAuditEventsService;
import com.sailpoint.audit.service.EventNormalizerService;
import com.sailpoint.audit.service.model.AuditTimeAndIdsRecord;
import com.sailpoint.audit.service.model.AuditUploadStatus;
import com.sailpoint.audit.service.normalizer.BaseNormalizer;
import com.sailpoint.audit.service.util.AuditUtil;
import com.sailpoint.audit.utils.TestUtils;
import com.sailpoint.mantis.core.service.ConfigService;
import com.sailpoint.mantis.core.service.CrudService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.collections.Sets;
import sailpoint.api.SailPointContext;
import sailpoint.object.AuditEvent;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BulkUploadUtilTest {

	@Mock
	AuditEvent _auditEvent;

	@Mock
	AtlasConfig _atlasConfig;

	@Mock
	ConfigService _configService;

	@Mock
	CrudService _crudService;

	@Mock
	EventNormalizerService _eventNormalizerService;

	@Mock
	FeatureFlagService _featureFlagService;

	@Mock
	OrgDataProvider _orgDataProvider;

	@Mock
	OrgData _orgData;

	@Mock
	SailPointContext _context;

	@Mock
	HashSet _set;

	@Mock
	AuditUtil _util;

	Event _event;

	BulkUploadUtil _bulkUploadUtil;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		_bulkUploadUtil = new BulkUploadUtil();

		_bulkUploadUtil._eventNormalizerService = _eventNormalizerService;
		_bulkUploadUtil._featureFlagService = _featureFlagService;
		_bulkUploadUtil._configService = _configService;
		_bulkUploadUtil._orgDataProvider = _orgDataProvider;
		_bulkUploadUtil._crudService = _crudService;
		_bulkUploadUtil._atlasConfig = _atlasConfig;
		_bulkUploadUtil._auditUtil = _util;

		when(_crudService.getContext()).thenReturn(_context);
		when(_orgDataProvider.find(eq("acme-solar"))).thenReturn(Optional.of(_orgData));
		when(_orgData.getAttribute(eq(BulkUploadUtil.S3_BUCKET_ATTRIBUTE_NAME)))
				.thenReturn("arn:aws:s3:::spt-audit-data-uswest2");

		TestUtils.setDummyRequestContext();

		_auditEvent = getTestAuditEvent();

		BaseNormalizer normalizer = new BaseNormalizer();
		_event = normalizer.normalize(_auditEvent, _auditEvent.getInstance());
		when(_eventNormalizerService.normalize(any(AuditEvent.class), eq(true))).thenReturn(_event);

		when(_util.isAlwaysAllowAudit(any(AuditEvent.class))).thenReturn(true);

	}

	@Test
	public void test() {
		//Nonwhitelisted
		String auditEvent = _bulkUploadUtil.auditTransform(_auditEvent);
		assertNotNull(auditEvent);

		//Whitelisted
		AuditEvent newAuditEvent = getTestAuditEvent();
		newAuditEvent.setAction("SchemaDeleted");

		BaseNormalizer normalizer = new BaseNormalizer();
		Event event = normalizer.normalize(newAuditEvent, _auditEvent.getInstance());
		when(_eventNormalizerService.normalize(any(AuditEvent.class), eq(true))).thenReturn(event);

		auditEvent = _bulkUploadUtil.auditTransform(newAuditEvent);
		assertNotNull(auditEvent);
	}

	@Test
	public void testGetCurrentUploadStatus() {
		_bulkUploadUtil.getLastUploadStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT);
		verify(_configService, times(1))
				.getString(BulkUploadAuditEventsService.PhaseStatus.BULK_UPLOAD_STATUS.getPhaseStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT));

		_bulkUploadUtil.getLastSyncToSearchStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT);
		verify(_configService, times(1))
				.getString(BulkUploadAuditEventsService.PhaseStatus.SEARCH_SYNC.getPhaseStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT));
	}

	@Test
	public void testSetCurrentUploadStatus() throws Exception{
		_bulkUploadUtil.setCurrentUploadStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT,
				AuditTimeAndIdsRecord.of(0, "id"));
		verify(_configService, times(1))
				.put(eq(BulkUploadAuditEventsService.PhaseStatus.BULK_UPLOAD_STATUS.getPhaseStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT))
						, any());

		_bulkUploadUtil.setCurrentSyncToSearchStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT,
				AuditTimeAndIdsRecord.of(0, "id"));
		verify(_configService, times(1))
				.put(eq(BulkUploadAuditEventsService.PhaseStatus.SEARCH_SYNC.getPhaseStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT))
						, any());
	}

	@Test
	public void testGetCurrentUploadStatusArchive() {
		_bulkUploadUtil.getLastUploadStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT_ARCHIVE);
		verify(_configService, times(1))
				.getString(BulkUploadAuditEventsService.PhaseStatus.BULK_UPLOAD_STATUS.getPhaseStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT_ARCHIVE));

		_bulkUploadUtil.getLastSyncToSearchStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT_ARCHIVE);
		verify(_configService, times(1))
				.getString(BulkUploadAuditEventsService.PhaseStatus.SEARCH_SYNC.getPhaseStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT_ARCHIVE));
	}

	@Test
	public void testSetCurrentUploadStatusArchive() throws Exception {
		_bulkUploadUtil.setCurrentUploadStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT_ARCHIVE,
				AuditTimeAndIdsRecord.of(0, "id"));
		verify(_configService, times(1))
				.put(eq(BulkUploadAuditEventsService.PhaseStatus.BULK_UPLOAD_STATUS.
								getPhaseStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT_ARCHIVE))
						, any());

		_bulkUploadUtil.setCurrentSyncToSearchStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT_ARCHIVE,
				AuditTimeAndIdsRecord.of(0, "id"));
		verify(_configService, times(1))
				.put(eq(BulkUploadAuditEventsService.PhaseStatus.SEARCH_SYNC.
								getPhaseStatus(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT_ARCHIVE))
						, any());
	}

	@Test
	public void testGetSyncToSearchStart() {
		_bulkUploadUtil.getSyncToSearchStart();
		verify(_configService, times(1))
				.getString(_bulkUploadUtil.SEARCH_TO_SYNC_STATUS);
	}

	@Test
	public void testSetSyncToSearchStart() throws Exception {
		_bulkUploadUtil.setSyncToSearchStart(AuditTimeAndIdsRecord.of(0, "id"));
		verify(_configService, times(1))
				.put(anyString(), any());
	}

	@Test
	public void testGetStartMessage() {
		AuditUploadStatus status = new AuditUploadStatus();
		String startMessage = _bulkUploadUtil.getStartMessage(status, 0, 0);
		assertEquals("starting bulk upload, lastCreated 0, lastIds , toDate 0, total remaining records to be processed: 0, remaining records to be processed this session: 0", startMessage);
	}

	@Test
	public void testGetStatusMessage() {
		AuditUploadStatus status = new AuditUploadStatus();
		String statusMessage = _bulkUploadUtil.getStatusString("message", status, 0L);
		assertEquals("message, lastCreated 0, lastIds , whitelisted/uploadToSearch: true, total uploaded 0, total processed 0, batch uploaded 0, batch processed 0, session errors 0, session skipped 0, session uploaded 0, session processed 0, session limit 0, duration 0", statusMessage);
	}

	@Test
	public void testGetString() {
		when(_atlasConfig.getString(anyString(), anyString())).thenReturn("value");
		AuditUploadStatus status = new AuditUploadStatus();
		String value = _bulkUploadUtil.getString("audit.start.date");
		assertEquals("value", value);
	}

	@Test
	public void testToCSV() {
		String ids = _bulkUploadUtil.getIdsCSV(Sets.newSet("1","2","3"));
		assertEquals("'1','2','3'", ids);
	}

	@Test
	public void testGetStatus() {
		AuditUploadStatus status = new AuditUploadStatus();
		AuditTimeAndIdsRecord auditTimeAndIdsRecord = _bulkUploadUtil.getStatus(status);
		assertEquals("{\"timestamp\":0,\"totalProcessed\":0,\"totalUploaded\":0,\"ids\":[],\"completed\":false}", JsonUtils.toJson(auditTimeAndIdsRecord));

		when(_configService.getString(anyString())).thenReturn(JsonUtils.toJson(AuditTimeAndIdsRecord.of(0, "id")));
		status = new AuditUploadStatus();
		auditTimeAndIdsRecord = _bulkUploadUtil.getStatus(status);
		assertEquals("{\"timestamp\":0,\"totalProcessed\":0,\"totalUploaded\":0,\"ids\":[],\"completed\":false}", JsonUtils.toJson(auditTimeAndIdsRecord));
	}

	@Test
	public void testGetS3BucketAttributeName() throws Exception {
		String bucket = _bulkUploadUtil.getS3BucketAttributeName("acme-solar");
		assertEquals("spt-audit-data-uswest2", bucket);
	}

	@Test
	public void testConvertToEvent() {
		when(_eventNormalizerService.normalize(anyObject(), eq(true))).thenReturn(new Event());
		Event event = _bulkUploadUtil.convertToEvent(_auditEvent);
		assertNotNull(event);
	}

	@Test
	public void testAuditTransformWhitelisted() {
		Event testEvent = new Event();
		//when(_eventNormalizerService.normalize(getTestAuditEvent(), true)).thenReturn(testEvent);
		when(_util.isAlwaysAllowAudit(any(AuditEvent.class))).thenReturn(false);
		String event = _bulkUploadUtil.auditTransformWhitelisted(getTestAuditEvent());

		//No action in event means, the whitelisted audit event look up will fail
		assertNull(event);

		when(_util.isAlwaysAllowAudit(any(AuditEvent.class))).thenReturn(true);

		//testEvent.setAction("SchemaDeleted");//whitelisted action
		AuditEvent auditEvent = getTestAuditEvent();
		auditEvent.setAction("SchemaDeleted");//whitelisted action
		event = _bulkUploadUtil.auditTransformWhitelisted(auditEvent);
		assertNotNull(event);

		when(_set.contains(anyString())).thenReturn(true);
		auditEvent.setAction("SUBSCRIPTION_EXECUTE_STARTED");//domain audit event action
		event = _bulkUploadUtil.auditTransformWhitelisted(auditEvent);
		assertNotNull(event);

		when(_util.isAlwaysAllowAudit(any(AuditEvent.class))).thenReturn(false);

		when(_set.contains(anyString())).thenReturn(false);
		testEvent.setAction("dummy");//domain audit event action
		event = _bulkUploadUtil.auditTransformWhitelisted(getTestAuditEvent());
		assertNull(event);
	}

	@Test
	public void testAuditTransformWhitelistedBulkUpload() {
		AuditEvent auditEvent = getTestAuditEvent();
		auditEvent.setId("2c91808876de02f50176de7b81a50759");
		auditEvent.setCreated(Date.from(Instant.ofEpochMilli(1610050404225L)));
		auditEvent.setAction("update");
		auditEvent.setSource("156554");
		auditEvent.setTarget("Access Profile:ZSU_SD_SD_PRC_COND_MNT_WSTLK_ECC");
		auditEvent.setString3("d756be02cfdc4108a3e05945c5aeb031");


		String event = _bulkUploadUtil.auditTransformWhitelisted(auditEvent);
		Assert.assertNotNull(event);
	}

	private AuditEvent getTestAuditEvent() {
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
		return _auditEvent;
	}
}
