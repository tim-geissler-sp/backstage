package com.sailpoint.audit.writer;

import com.sailpoint.atlas.service.AtomicMessageService;
import com.sailpoint.audit.service.BulkUploadAuditEventsService;
import com.sailpoint.audit.service.model.AuditUploadStatus;
import com.sailpoint.audit.util.BulkUploadUtil;
import com.sailpoint.mantis.core.service.ConfigService;
import com.sailpoint.mantis.platform.service.search.SearchUtil;
import com.sailpoint.mantis.platform.service.search.SyncTransformer;
import org.apache.commons.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sailpoint.object.SailPointObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BulkSearchWriterTest {

	@Mock
	AtomicMessageService _atomicMessageService;

	@Mock
	AuditUploadStatus _auditUploadStatus;

	@Mock
	BulkUploadUtil _bulkUploadUtil;

	@Mock
	ConfigService _configService;

	@Mock
	File _tempUploadFile;

	@Mock
	Log _log;

	@Mock
	PrintWriter _printWriter;

	@Mock
	SailPointObject _sailPointObject;

	@Mock
	SearchUtil _searchUtil;

	@Mock
	SyncTransformer _syncTransformer;

	BulkSearchWriter _bulkSearchWriter;

	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		_bulkSearchWriter = new BulkSearchWriter(_atomicMessageService, _bulkUploadUtil, _configService,
				_searchUtil, _syncTransformer);
		when(_configService.getString(anyString())).thenReturn("s3Bucket");
		when(_auditUploadStatus.getBatchUploaded()).thenReturn(10);
		when(_syncTransformer.transform(anyObject())).thenReturn("{}");
		BulkSearchWriter._log = _log;
	}

	@Test
	public void testWriteLine() throws Exception {
		_bulkSearchWriter._printWriter = _printWriter;
		_bulkSearchWriter._tempUploadFile = _tempUploadFile;
		_bulkSearchWriter.writeLine(_sailPointObject, "acme-solar", BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, true, _auditUploadStatus);

		verify(_printWriter, times(1)).println(anyString());
	}

	@Test
	public void testWriteLineException() throws Exception {
		when(_syncTransformer.transform(anyObject())).thenThrow(new Exception());

		_bulkSearchWriter._printWriter = _printWriter;
		_bulkSearchWriter._tempUploadFile = _tempUploadFile;
		_bulkSearchWriter.writeLine(_sailPointObject, "acme-solar", BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, true, _auditUploadStatus);

		verify(_log, times(1)).error(anyString(), anyObject());
	}

	@Test
	public void testWriteLineNullPrintWriter() throws Exception {
		_bulkSearchWriter.writeLine(_sailPointObject, "acme-solar", BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, true, _auditUploadStatus);

		verify(_auditUploadStatus, times(1)).incrementUploaded();
	}

	@Test
	public void testSendBatch() throws Exception {
		_bulkSearchWriter._printWriter = _printWriter;
		_bulkSearchWriter.sendBatch("acem-solar", BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, true, _auditUploadStatus);

		verify(_searchUtil, times(1)).uploadSyncFile(any(), any(), any(), any());
	}

	@Test
	public void testSendBatchOnetimeSync() throws Exception {
		_bulkSearchWriter._printWriter = _printWriter;
		when(_auditUploadStatus.isOnetimeSync()).thenReturn(true);
		_bulkSearchWriter.sendBatch("acem-solar", BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, true, _auditUploadStatus);

		verify(_searchUtil, times(1)).uploadSyncFile(any(), any(), any(), any());
		verify(_bulkUploadUtil, times(0)).setCurrentSyncToSearchStatus(any(), any());
	}

	@Test
	public void testSendBatchException() throws Exception {
		try {
			when(_searchUtil.uploadSyncFile(any(), any(), any(), any())).thenThrow(new IOException());
		} catch (IOException e) {
		}

		_bulkSearchWriter.sendBatch("acem-solar", BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, true, _auditUploadStatus);

		verify(_log, times(4)).error(anyString());
	}

	@Test
	public void testPurge() {
		_bulkSearchWriter.purge("acme-solar");
	}

	@Test
	public void testAreLocalDatesSame() {
		boolean actualResponse = _bulkSearchWriter.areLocalDatesSame(1644533734805L, 0);
		assertFalse(actualResponse);
	}

	@Test
	public void testAreLocalDatesSameWithMonthDifferent() {
		Instant now = Instant.now();
		Instant minus = now.minus(35, ChronoUnit.DAYS);

		_bulkSearchWriter.areLocalDatesSame(now.toEpochMilli(), minus.toEpochMilli());
	}
}
