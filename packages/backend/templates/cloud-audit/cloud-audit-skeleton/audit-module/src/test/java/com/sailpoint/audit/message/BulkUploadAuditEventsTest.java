package com.sailpoint.audit.message;

import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.messaging.client.Payload;
import com.sailpoint.atlas.messaging.server.MessageHandlerContext;
import com.sailpoint.atlas.service.MessageClientService;
import com.sailpoint.atlas.test.MessageHandlerTest;
import com.sailpoint.audit.service.AuditReportService;
import com.sailpoint.audit.service.BulkUploadAuditEventsService;
import com.sailpoint.audit.service.SyncJobManager;
import com.sailpoint.audit.service.model.AuditTimeAndIdsRecord;
import com.sailpoint.audit.util.BulkUploadUtil;
import org.apache.commons.logging.Log;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BulkUploadAuditEventsTest extends MessageHandlerTest<BulkUploadAuditEvents> {

	@Mock
	AtlasConfig _atlasConfig;

	@Mock
	AuditReportService _auditReportService;

	@Mock
	BulkUploadAuditEventsService _bulkUploadAuditEventsService;

	@Mock
	BulkUploadPayload _bulkUploadPayload;

	@Mock
	BulkUploadUtil _bulkUploadUtil;

	@Mock
	MessageHandlerContext _context;

	@Mock
	Log _log;

	@Mock
	MessageClientService _messageClientService;

	@Mock
	SyncJobManager _syncJobManager;

	@Override
	protected BulkUploadAuditEvents createHandler() throws Exception {
		BulkUploadAuditEvents handler = new BulkUploadAuditEvents();

		_bulkUploadPayload = new BulkUploadPayload();
		BulkUploadAuditEvents._log = _log;
		when(_context.getMessageContent(any())).thenReturn(_bulkUploadPayload);

		when(_atlasConfig.getInt("BULK_UPLOAD_BATCH_SIZE", 10000)).thenReturn(10000);
		when(_bulkUploadUtil.getLastUploadStatus(any(BulkUploadAuditEventsService.AuditTableNames.class))).thenReturn(AuditTimeAndIdsRecord.of(0,"id"));

		handler._auditReportService = _auditReportService;
		handler._bulkUploadAuditEventsService = _bulkUploadAuditEventsService;
		handler._messageClientService = _messageClientService;
		handler._atlasConfig = _atlasConfig;
		handler._bulkUploadUtil = _bulkUploadUtil;
		handler._syncJobManager = _syncJobManager;

		return handler;
	}

	@Override
	protected Class<BulkUploadAuditEvents> getHandlerClass() {
		return BulkUploadAuditEvents.class;
	}

	@Test
	public void testPayLoadType() {
		Assert.assertTrue(BulkUploadAuditEvents.PAYLOAD_TYPE.values().length > 0);
		Assert.assertTrue(BulkUploadAuditEvents.PAYLOAD_TYPE.valueOf("BULK_UPLOAD_AUDIT_EVENTS") != null);
	}

	@Test
	public void testUploadWithArguments() throws Exception {
		Map<String, Object> args = new HashMap();
		args.put("fromDate", "2013-01-01");
		args.put("toDate", "2019-12-31");
		_bulkUploadPayload.setArguments(args);

		givenPayload(new Payload(BulkUploadAuditEvents.PAYLOAD_TYPE.BULK_UPLOAD_AUDIT_EVENTS, _bulkUploadPayload));

		whenTheMessageIsHandled();

		verify(_bulkUploadAuditEventsService, times(1)).bulkSyncAudit(any(), any(), any(), anyInt(), anyInt(), any());
	}

	@Test
	public void testUploadWithArgumentsOnetime() throws Exception {
		Map<String, Object> args = new HashMap();
		args.put("fromDate", "2013-01-01");
		args.put("toDate", "2019-12-31");
		args.put("onetimeSync", true);
		_bulkUploadPayload.setArguments(args);

		givenPayload(new Payload(BulkUploadAuditEvents.PAYLOAD_TYPE.BULK_UPLOAD_AUDIT_EVENTS, _bulkUploadPayload));

		whenTheMessageIsHandled();

		verify(_bulkUploadAuditEventsService, times(2)).bulkSyncAudit(any(), any(), any(), anyInt(), anyInt(), any());
		verify(_syncJobManager, times(0)).setStatusComplete(any(), any(), any());
	}

	@Test
	public void testUploadWithArgumentsWithArchive() throws Exception {
		Map<String, Object> args = new HashMap();
		_bulkUploadPayload.setArguments(args);

		givenPayload(new Payload(BulkUploadAuditEvents.PAYLOAD_TYPE.BULK_UPLOAD_AUDIT_EVENTS, _bulkUploadPayload));

		whenTheMessageIsHandled();

		verify(_bulkUploadAuditEventsService, times(1)).bulkSyncAudit(any(), any(), any(), anyInt(), anyInt(), any());
	}

	@Test
	public void testReset() throws Exception  {
		Map<String, Object> args = new HashMap();
		_bulkUploadPayload.setReset(true);
		_bulkUploadPayload.setArguments(args);

		givenPayload(new Payload(BulkUploadAuditEvents.PAYLOAD_TYPE.BULK_UPLOAD_AUDIT_EVENTS, _bulkUploadPayload));

		whenTheMessageIsHandled();

		verify(_bulkUploadAuditEventsService, times(1)).purgeBulkSyncFiles(any(), any());
	}

	@Test
	public void testResetError() throws Exception  {
		Map<String, Object> args = new HashMap();
		_bulkUploadPayload.setReset(true);
		_bulkUploadPayload.setArguments(args);
		doThrow(new RuntimeException()).when(_bulkUploadAuditEventsService).purgeBulkSyncFiles(anyMap(), any());

		givenPayload(new Payload(BulkUploadAuditEvents.PAYLOAD_TYPE.BULK_UPLOAD_AUDIT_EVENTS, _bulkUploadPayload));

		whenTheMessageIsHandled();

		verify(_log, times(1)).error(anyString(), anyObject());
	}

	@Test
	public void testCount() throws Exception {
		Map<String, Object> args = new HashMap();
		_bulkUploadPayload.setCountOnly(true);
		_bulkUploadPayload.setArguments(args);

		givenPayload(new Payload(BulkUploadAuditEvents.PAYLOAD_TYPE.BULK_UPLOAD_AUDIT_EVENTS, _bulkUploadPayload));

		whenTheMessageIsHandled();

		verify(_bulkUploadAuditEventsService, times(1)).countAuditRecords(any());
		verify(_bulkUploadAuditEventsService, times(1)).countAllAuditRecords(any());

	}

	@Test
	public void testCountError() throws Exception {
		Map<String, Object> args = new HashMap();
		_bulkUploadPayload.setCountOnly(true);
		_bulkUploadPayload.setArguments(args);
		doThrow(new RuntimeException()).when(_bulkUploadAuditEventsService).countAuditRecords(any());

		givenPayload(new Payload(BulkUploadAuditEvents.PAYLOAD_TYPE.BULK_UPLOAD_AUDIT_EVENTS, _bulkUploadPayload));

		whenTheMessageIsHandled();

		verify(_log, times(1)).error(anyString(), anyObject());
	}

	@Test
	public void testCreateIndex() {
		Map<String, Object> args = new HashMap();
		_bulkUploadPayload.setCreateAuditArchiveIndex(true);
		_bulkUploadPayload.setArguments(args);

		givenPayload(new Payload(BulkUploadAuditEvents.PAYLOAD_TYPE.BULK_UPLOAD_AUDIT_EVENTS, _bulkUploadPayload));

		whenTheMessageIsHandled();

		verify(_bulkUploadAuditEventsService, times(1)).createArchiveIndex();
	}

	@Test
	public void testCreateIndexError() {
		Map<String, Object> args = new HashMap();
		_bulkUploadPayload.setCreateAuditArchiveIndex(true);
		_bulkUploadPayload.setArguments(args);
		doThrow(new RuntimeException()).when(_bulkUploadAuditEventsService).createArchiveIndex();

		givenPayload(new Payload(BulkUploadAuditEvents.PAYLOAD_TYPE.BULK_UPLOAD_AUDIT_EVENTS, _bulkUploadPayload));

		try {
			whenTheMessageIsHandled();
		} catch (RuntimeException re) {
		}

		verify(_log, times(1)).error(anyString(), anyObject());
	}
}
