/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.message;

import com.sailpoint.atlas.messaging.client.Payload;
import com.sailpoint.atlas.test.MessageHandlerTest;
import com.sailpoint.audit.service.BulkSyncS3AuditEventsService;
import com.sailpoint.audit.service.SyncJobManager;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BulkSyncS3AuditEventsTest extends MessageHandlerTest<BulkSyncS3AuditEvents> {

	@Mock
	BulkSyncS3AuditEventsService _bulkSyncS3AuditEventsService;

	@Mock
	SyncJobManager _syncJobManager;

	Map<String, Object> _arguments;

	@Override
	protected Class<BulkSyncS3AuditEvents> getHandlerClass() {
		return BulkSyncS3AuditEvents.class;
	}

	@Override
	protected BulkSyncS3AuditEvents createHandler() {
		BulkSyncS3AuditEvents handler = new BulkSyncS3AuditEvents();

		handler._bulkSyncS3AuditEventsService = _bulkSyncS3AuditEventsService;
		handler._syncJobManager = _syncJobManager;

		return handler;
	}

	@Test
	public void callHandler() {
		BulkSyncPayload bulkSyncPayload = new BulkSyncPayload();

		_arguments = new HashMap<>();
		_arguments.put("fromDate", "2020-01-01");
		_arguments.put("toDate", "2020-01-01");
		_arguments.put("batchSize", 500);

		bulkSyncPayload.setArguments(_arguments);

		givenPayload(new Payload(BulkSyncS3AuditEvents.PAYLOAD_TYPE.BULK_SYNCHRONIZE_S3_AUDIT_EVENTS, bulkSyncPayload));

		whenTheMessageIsHandled();

		verify(_bulkSyncS3AuditEventsService).sync(any(BulkSyncPayload.class), any());
	}

	@Test
	public void callHandlerReset() {
		BulkSyncPayload bulkSyncPayload = new BulkSyncPayload();

		bulkSyncPayload.setReset(true);

		givenPayload(new Payload(BulkSyncS3AuditEvents.PAYLOAD_TYPE.BULK_SYNCHRONIZE_S3_AUDIT_EVENTS, bulkSyncPayload));

		whenTheMessageIsHandled();

		verify(_syncJobManager, times(2)).resetStatusComplete(any(), any(), any());
	}

	@Test
	public void callHandlerForCountOnly() {
		BulkSyncPayload bulkSyncPayload = new BulkSyncPayload();

		_arguments = new HashMap<>();
		_arguments.put("fromDate", "2020-01-01");
		_arguments.put("toDate", "2020-01-01");
		_arguments.put("batchSize", 500);

		bulkSyncPayload.setArguments(_arguments);
		bulkSyncPayload.setCountOnly(true);

		givenPayload(new Payload(BulkSyncS3AuditEvents.PAYLOAD_TYPE.BULK_SYNCHRONIZE_S3_AUDIT_EVENTS, bulkSyncPayload));

		whenTheMessageIsHandled();

		verify(_bulkSyncS3AuditEventsService).sync(any(BulkSyncPayload.class), any());
	}

}
