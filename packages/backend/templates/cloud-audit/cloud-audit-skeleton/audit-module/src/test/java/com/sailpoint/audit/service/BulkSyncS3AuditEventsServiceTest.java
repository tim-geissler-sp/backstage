/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.inject.Provider;
import com.sailpoint.atlas.OrgData;
import com.sailpoint.atlas.OrgDataProvider;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.atlas.service.AtomicMessageService;
import com.sailpoint.atlas.service.RemoteFileService;
import com.sailpoint.audit.message.BulkSyncPayload;
import com.sailpoint.audit.persistence.S3PersistenceManager;
import com.sailpoint.audit.service.model.AuditTypeDTO;
import com.sailpoint.audit.util.BulkUploadUtil;
import com.sailpoint.audit.utils.TestUtils;
import com.sailpoint.mantis.core.service.ConfigService;
import com.sailpoint.mantis.platform.service.search.SearchUtil;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import sailpoint.object.AuditEvent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class BulkSyncS3AuditEventsServiceTest {
	@Mock
	Provider<AtomicMessageService> _atomicMessageServiceProvider;

	@Mock
	AtomicMessageService _atomicMessageService;

	@Mock
	AuditEventService _auditEventService;

	@Mock
	BulkUploadUtil _bulkUploadUtil;

	@Mock
	ConfigService _configService;

	@Mock
	EventNormalizerService _eventNormalizerService;

	@Mock
	HttpRequestBase _httpRequestBase;

	@Mock
	ObjectListing _objectListing;

	@Mock
	OrgData _orgData;

	@Mock
	OrgDataProvider _orgDataProvider;

	@Mock
	RemoteFileService _remoteFileService;

	@Mock
	AmazonS3 _s3Client;

	@Mock
	SearchUtil _searchUtil;

	Map<String, Object> _arguments;

	BulkSyncS3AuditEventsService _bulkSyncS3AuditEventsService;

	File _uploadFile;

	@Before
	public void setUp() throws Exception {
		RequestContext requestContext = TestUtils.setDummyRequestContext();

		_bulkSyncS3AuditEventsService = new BulkSyncS3AuditEventsService();

		_bulkSyncS3AuditEventsService._atomicMessageService = _atomicMessageServiceProvider;
		_bulkSyncS3AuditEventsService._configService = _configService;
		_bulkSyncS3AuditEventsService._eventNormalizerService = _eventNormalizerService;
		_bulkSyncS3AuditEventsService._remoteFileService = _remoteFileService;
		_bulkSyncS3AuditEventsService._searchUtil = _searchUtil;
		_bulkSyncS3AuditEventsService._bulkUploadUtil = _bulkUploadUtil;
		_bulkSyncS3AuditEventsService._orgDataProvider = _orgDataProvider;
		_bulkSyncS3AuditEventsService._auditEventService = _auditEventService;

		when(_atomicMessageServiceProvider.get()).thenReturn(_atomicMessageService);
		when(_configService.getString(BulkSyncS3AuditEventsService.S3BUCKET)).thenReturn("test bucket");
		when(_remoteFileService.listFiles(anyString(), anyString())).thenReturn(Arrays.asList("file1"));
		when(_orgDataProvider.ensureFind(anyString())).thenReturn(_orgData);
		when(_orgData.getTenantId()).thenReturn(Optional.of("tenantId"));

		_uploadFile = File.createTempFile("bulk_audit_sync", ".json");
		when(_searchUtil.uploadSyncFile(any(), any(), any(), any())).thenReturn(_uploadFile);
		when(_bulkUploadUtil.getS3BucketAttributeName(any())).thenReturn("test bucket");
		when(_bulkUploadUtil.getString(eq("audit.start.date"))).thenReturn("2013-01-01T00:00");

		Answer<FileInputStream> answer = new Answer<FileInputStream>() {
			public FileInputStream answer(InvocationOnMock invocation) throws Throwable {
				return new FileInputStream(generateTestFile(500));
			}
		};
		when(_remoteFileService.openFileStream(any(), any())).thenAnswer(answer);

		S3ObjectSummary s3ObjectSummary = new S3ObjectSummary();
		s3ObjectSummary.setKey("tenantId/2020/01/02/03/04/auditId");
		when(_objectListing.getObjectSummaries()).thenReturn(Collections.singletonList(s3ObjectSummary));
		when(_s3Client.listObjects(any(ListObjectsRequest.class))).thenReturn(_objectListing);
		when(_s3Client.listNextBatchOfObjects(any(ObjectListing.class))).thenReturn(_objectListing);
		when(_s3Client.doesObjectExist(anyString(), eq("tenantId/2020/01/02/03/04/auditId"))).thenReturn(true);
		S3Object s3Object = new S3Object();
		AuditEvent auditEvent = new AuditEvent();
		String auditEventJson = JsonUtils.toJson(auditEvent);
		s3Object.setObjectContent(new S3ObjectInputStream(new ByteArrayInputStream(auditEventJson.getBytes()), _httpRequestBase));
		when(_s3Client.getObject(anyString(), eq("tenantId/2020/01/02/03/04/auditId"))).thenReturn(s3Object);

		S3PersistenceManager.overrideS3Client(_s3Client);
	}

	@After
	public void cleanUp() {
		_uploadFile.delete();
	}

	@Test
	public void testSyncOneDay() {
		_arguments = new HashMap<>();
		_arguments.put("fromDate", "2020-03-01T00:00");
		_arguments.put("toDate", "2020-03-02T00:00");

		BulkSyncPayload bulkSyncPayload = new BulkSyncPayload();
		bulkSyncPayload.setArguments(_arguments);

		_bulkSyncS3AuditEventsService.sync(bulkSyncPayload, () -> false);

		verify(_auditEventService, times(25)).normalizeAndEmit(any());
	}

	@Test
	public void testSyncThreeHours() {
		_arguments = new HashMap<>();
		_arguments.put("fromDate", "2022-02-03T20:00");
		_arguments.put("toDate", "2022-02-03T22:00");

		BulkSyncPayload bulkSyncPayload = new BulkSyncPayload();
		bulkSyncPayload.setArguments(_arguments);

		_bulkSyncS3AuditEventsService.sync(bulkSyncPayload, () -> false);

		verify(_auditEventService, times(3)).normalizeAndEmit(any());
	}

	@Test
	public void testSync30Days() {
		_arguments = new HashMap<>();
		_arguments.put("fromDate", "2020-03-01T00:00");
		_arguments.put("toDate", "2020-03-30T00:00");

		BulkSyncPayload bulkSyncPayload = new BulkSyncPayload();
		bulkSyncPayload.setArguments(_arguments);

		_bulkSyncS3AuditEventsService.sync(bulkSyncPayload, () -> false);

		verify(_auditEventService, times(697)).normalizeAndEmit(any());
	}

	private File generateTestFile(int numberOfEvents) throws IOException {
		File tempFile = File.createTempFile("bulk_audit_sync", ".json");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(tempFile.toPath()))) {
			AuditTypeDTO auditTypeDTO = new AuditTypeDTO();
			for (int i = 0; i < numberOfEvents; i++) {
				auditTypeDTO.setType("PROVISIONING");
				pw.println(JsonUtils.toJson(auditTypeDTO));
			}
		}
		tempFile.deleteOnExit();
		return tempFile;
	}
}
