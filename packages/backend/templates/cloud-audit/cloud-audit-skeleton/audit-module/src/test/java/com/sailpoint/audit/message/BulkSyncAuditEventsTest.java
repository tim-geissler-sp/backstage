/*
 *
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 *
 */

package com.sailpoint.audit.message;

import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.messaging.client.Payload;
import com.sailpoint.atlas.messaging.server.MessageHandlerContext;
import com.sailpoint.atlas.search.model.event.Event;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.atlas.test.MessageHandlerTest;
import com.sailpoint.audit.service.BulkUploadAuditEventsService;
import com.sailpoint.audit.service.EventNormalizerService;
import com.sailpoint.audit.service.SyncJobManager;
import com.sailpoint.audit.service.model.AuditTimeAndIdsRecord;
import com.sailpoint.audit.service.model.JobTypes;
import com.sailpoint.audit.util.BulkUploadUtil;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BulkSyncAuditEventsTest extends MessageHandlerTest<BulkSyncAuditEvents> {
	@Mock
	AtlasConfig _atlasConfig;

	@Mock
	BulkSyncPayload _bulkSyncPayload;

	@Mock
	BulkUploadAuditEventsService _bulkUploadAuditEventsService;

	@Mock
	BulkUploadUtil _bulkUploadUtil;

	@Mock
	MessageHandlerContext _context;

	@Mock
	EventNormalizerService _eventNormalizerService;

	@Mock
	FeatureFlagService _featureFlagService;

	@Mock
	SyncJobManager _syncJobManager;

	@Override
	protected BulkSyncAuditEvents createHandler() {
		BulkSyncAuditEvents handler = new BulkSyncAuditEvents();

		Map<String, Object> arguments = new HashMap<>();
		arguments.put("fromDate", "2020-01-01");
		arguments.put("toDate", "2020-01-01");
		_bulkSyncPayload = new BulkSyncPayload();
		_bulkSyncPayload.setArguments(arguments);
		when(_context.getMessageContent(any())).thenReturn(_bulkSyncPayload);

		Event event = Event.builder()
				.withCreated(new Date())
				.withId("id")
				.withAction("TEST")
				.withType("NONE")
				.build();

		when(_eventNormalizerService.normalize(any())).thenReturn(event);
		when(_atlasConfig.getInt("BULK_UPLOAD_BATCH_SIZE", 10000)).thenReturn(10000);
		when(_bulkUploadUtil.getLastUploadStatus(any(BulkUploadAuditEventsService.AuditTableNames.class))).thenReturn(AuditTimeAndIdsRecord.of(0,"id"));
		when(_syncJobManager.isJobRunning(anyObject(), eq(JobTypes.sync_to_search), anyString(), anyString())).thenReturn(false);

		handler._atlasConfig = _atlasConfig;
		handler._bulkUploadAuditEventsService = _bulkUploadAuditEventsService;
		handler._bulkUploadUtil = _bulkUploadUtil;
		handler._eventNormalizerService = _eventNormalizerService;
		handler._featureFlagService = _featureFlagService;
		handler._syncJobManager = _syncJobManager;

		return handler;
	}

	@Override
	protected Class<BulkSyncAuditEvents> getHandlerClass() {
		return BulkSyncAuditEvents.class;
	}

	@Test
	public void testPayLoadType() {
		Assert.assertTrue(BulkSyncAuditEvents.PAYLOAD_TYPE.values().length > 0);
		Assert.assertTrue(BulkSyncAuditEvents.PAYLOAD_TYPE.valueOf("BULK_SYNCHRONIZE_AUDIT_EVENTS") != null);
	}

	@Test
	public void callHandlerSyncArguments() throws Exception {
		Map<String, Object> args = new HashMap();
		args.put("fromDate", "2013-01-01");
		args.put("toDate", "2019-12-31");
		args.put("batchSize", 10000);
		args.put("toDate", 1000000);

		_bulkSyncPayload.setArguments(args);

		givenPayload(new Payload(BulkSyncAuditEvents.PAYLOAD_TYPE.BULK_SYNCHRONIZE_AUDIT_EVENTS, _bulkSyncPayload));

		whenTheMessageIsHandled();

		verify(_bulkUploadAuditEventsService, times(1)).bulkSyncAudit(any(), any(), any(), anyInt(), anyInt(), any());
	}

	@Test
	public void callHandlerSyncArgumentsOnetime() throws Exception {
		Map<String, Object> args = new HashMap();
		args.put("fromDate", "2013-01-01");
		args.put("toDate", "2019-12-31");
		args.put("batchSize", 10000);
		args.put("toDate", 1000000);
		args.put("onetimeSync", true);

		_bulkSyncPayload.setArguments(args);

		givenPayload(new Payload(BulkSyncAuditEvents.PAYLOAD_TYPE.BULK_SYNCHRONIZE_AUDIT_EVENTS, _bulkSyncPayload));

		whenTheMessageIsHandled();

		verify(_bulkUploadAuditEventsService, times(2)).bulkSyncAudit(any(), any(), any(), anyInt(), anyInt(), any());
		verify(_syncJobManager, times(1)).setStatusComplete(any(), any(), any());
	}

	@Test
	public void callHandlerReset() throws Exception {
		BulkSyncPayload bulkSyncPayload = new BulkSyncPayload();
		Map<String, Object> args = new HashMap();
		args.put("purgeOrg", true);

		bulkSyncPayload.setReset(true);
		bulkSyncPayload.setArguments(args);

		givenPayload(new Payload(BulkSyncAuditEvents.PAYLOAD_TYPE.BULK_SYNCHRONIZE_AUDIT_EVENTS, bulkSyncPayload));

		whenTheMessageIsHandled();

		verify(_bulkUploadAuditEventsService, times(1)).purgeSearchIndex(any());
	}


	@Test
	public void callHandlerCount() throws Exception {
		BulkSyncPayload bulkSyncPayload = new BulkSyncPayload();
		bulkSyncPayload.setCountOnly(true);

		givenPayload(new Payload(BulkSyncAuditEvents.PAYLOAD_TYPE.BULK_SYNCHRONIZE_AUDIT_EVENTS, bulkSyncPayload));

		whenTheMessageIsHandled();

		verify(_bulkUploadAuditEventsService, times(1)).countAuditRecords(any());
	}
}
