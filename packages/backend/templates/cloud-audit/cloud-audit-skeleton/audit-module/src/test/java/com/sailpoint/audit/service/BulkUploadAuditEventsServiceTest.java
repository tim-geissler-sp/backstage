/*
 *
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 *
 */
package com.sailpoint.audit.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.atlas.service.RemoteFileService;
import com.sailpoint.audit.service.mapping.DomainAuditEventsUtil;
import com.sailpoint.audit.service.model.AuditTimeAndIdsRecord;
import com.sailpoint.audit.util.BulkUploadUtil;
import com.sailpoint.audit.utils.TestUtils;
import com.sailpoint.audit.writer.BulkFirehoseWriter;
import com.sailpoint.audit.writer.BulkSearchWriter;
import com.sailpoint.audit.writer.BulkWriterFactory;
import com.sailpoint.mantis.core.service.ConfigService;
import com.sailpoint.mantis.core.service.CrudService;
import com.sailpoint.mantis.platform.api.interfaces.Persistence;
import com.sailpoint.mantis.platform.service.search.SearchUtil;
import com.sailpoint.mantis.platform.service.search.SyncTransformer;
import org.apache.commons.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import sailpoint.api.SailPointContext;
import sailpoint.object.QueryOptions;
import sailpoint.tools.GeneralException;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BulkUploadAuditEventsServiceTest {
	@Mock
	AtlasConfig _atlasConfig;

	@Mock
	BulkUploadUtil _bulkUploadUtil;

	@Mock
	BulkWriterFactory _bulkWriterFactory;

	@Mock
	BulkFirehoseWriter _bulkFirehoseWriter;

	@Mock
	BulkSearchWriter _bulkSearchWriter;

	@Mock
	SailPointContext _context;

	@Mock
	CrudService _crudService;

	@Mock
	ConfigService _configService;

	@Mock
	Connection _connection;

	@Mock
	FeatureFlagService _featureFlagService;

	@Mock
	File _file;

	@Mock
	AuditTimeAndIdsRecord _firehoseStatus;

	@Mock
	Iterator _iterator;

	@Mock
	Log _log;

	@Mock
	MySQLSyntaxErrorException _mySQLSyntaxErrorException;

	@Mock
	Persistence _persistence;

	@Mock
	Statement _statement;

	@Mock
	SyncTransformer _syncTransformer;

	@Mock
	RemoteFileService _remoteFileService;

	@Mock
	SearchUtil _searchUtil;

	@Mock
	AmazonS3 _s3Client;

	@Mock
	DomainAuditEventsUtil _domainAuditEventsUtil;

	@Mock
	AuditEventService _auditEventService;

	Date _date;

	Date _lastUploaded;

	QueryOptions _qo;

	Map<String, Object> _arguments;

	BulkUploadAuditEventsService _sut;

	@Before
	public void setUp() throws Exception {
		_sut = new BulkUploadAuditEventsService();
		_qo = new QueryOptions();
		TestUtils.setDummyRequestContext();

		_date = new Date();

		_sut._crudService = _crudService;
		_sut._configService = _configService;
		_sut._searchUtil = _searchUtil;
		_sut._atlasConfig = _atlasConfig;
		_sut._featureFlagService = _featureFlagService;
		_sut._bulkUploadUtil = _bulkUploadUtil;
		_sut._remoteFileService = _remoteFileService;
		_sut._persistence = _persistence;
		_sut._bulkWriterFactory = _bulkWriterFactory;
		_sut._domainAuditEventsUtil = _domainAuditEventsUtil;
		_sut._auditEventService = _auditEventService;

		_sut._s3Client = _s3Client;
		BulkUploadAuditEventsService._log = _log;

		when(_persistence.getContext()).thenReturn(_context);
		when(_context.getJdbcConnection()).thenReturn(_connection);
		when(_connection.createStatement()).thenReturn(_statement);

		_arguments = new HashMap<>();
		_arguments.put("fromDate", "2019-11-01");
		_arguments.put("toDate", "2020-01-01");

		when(_searchUtil.uploadUncompressedFile(any(), any(), any(), any())).thenReturn(_file);
		when(_bulkUploadUtil.getS3BucketAttributeName(eq("acme-solar")))
				.thenReturn("arn:aws:s3:::spt-audit-data-uswest2");
		when(_bulkUploadUtil.getString(eq("audit.start.date"))).thenReturn("2013-01-01T00:00");

		when(_firehoseStatus.getTimestamp()).thenReturn(new Date().getTime());
		when(_bulkUploadUtil.getFirstAuditParquetCheckpoint()).thenReturn(_firehoseStatus);
		when(_bulkWriterFactory.getWriter(any(), eq(false))).thenReturn(_bulkFirehoseWriter);
		when(_bulkWriterFactory.getWriter(any(),eq(true))).thenReturn(_bulkSearchWriter);

		_lastUploaded = new Date();
		String configKey = "bulkUpload_"+"1575158400000" + "_" + "1575158400000";
		String lastUploadString1 = "{ \"timestamp\": "+_lastUploaded.getTime() + ", \"ids\": null, \"totalUploaded\": 0, \"totalProcessed\": 0}";
		String lastUploadString2 = "{ \"timestamp\": "+_lastUploaded.getTime() + ", \"ids\": null, \"totalUploaded\": 3, \"totalProcessed\": 3}";
		when(_configService.getString(eq(configKey))).thenReturn(lastUploadString1, lastUploadString2);

		when(_bulkUploadUtil.getLastUploadStatus(any())).thenReturn(AuditTimeAndIdsRecord.of(_lastUploaded.getTime(), "id_12345"));
		when(_bulkUploadUtil.getLastSyncToSearchStatus(any())).thenReturn(AuditTimeAndIdsRecord.of(_lastUploaded.getTime(), "id_12345"));

		when(_crudService.getContext()).thenReturn(_context);

		ObjectListing objectListing = mock((ObjectListing.class));
		when(_s3Client.listObjects(any(ListObjectsRequest.class))).thenReturn(objectListing);

		when((objectListing.getCommonPrefixes())).thenReturn(Arrays.asList("type=TYPE1/", "type=TYPE2/"),
				Arrays.asList("date=2020-01-01/", "date=2020-01-02/"));
	}

	/**
	 * Test to validate records are bulk synced when "created" field is present in records.
	 *
	 * There are a total of 5 records expected to be bulk synced. They need to be done in 2 batches
	 */
	@Test
	public void testBulkSync() throws Exception {
		setupSearch();

		_sut.bulkSyncAudit(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, object -> "",
				() -> false, 5, 3, _arguments);
		verify(_context, times(1)).search(contains("count"), anyMap(), any());
		verify(_context, times(2)).search(contains("select id"), anyMap(), any());
		//Total number of records processed will be 5. Batch size is 3. So, 2 files have to be sent.

		verify(_bulkFirehoseWriter, times(5)).writeLine(any(), anyString(), anyObject(), anyBoolean(), any());
		verify(_bulkFirehoseWriter, times(2)).sendBatch(anyString(), any(), anyBoolean(), any());
	}


	/**
	 * Test to validate records are bulk synced when "created" field is present in records.
	 *
	 * There are a total of 5 records expected to be bulk synced. They need to be done in 2 batches
	 */
	@Test
	public void testBulkSyncOnetimeSync() throws Exception {
		setupSearch();

		Map<String, Object> arguments = new HashMap<>();
		arguments.put("fromDate", "2019-11-01");
		arguments.put("toDate", "2020-01-01");
		arguments.put("onetimeSync", true);

		_sut.bulkSyncAudit(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, object -> "",
				() -> false, 5, 3, arguments);
		verify(_context, times(1)).search(contains("count"), anyMap(), any());
		verify(_context, times(2)).search(contains("select id"), anyMap(), any());
		//Total number of records processed will be 5. Batch size is 3. So, 2 files have to be sent.

		verify(_bulkFirehoseWriter, times(5)).writeLine(any(), anyString(), anyObject(), anyBoolean(), any());
		verify(_bulkFirehoseWriter, times(2)).sendBatch(anyString(), any(), anyBoolean(), any());
		verify(_bulkUploadUtil, times(0)).setSyncToSearchStart(any());

	}

	/**
	 * Test to validate records are bulk synced when PLTDP_CRUD_DOMAIN_BULKSYNC FF is enabled
	 */
	@Test
	public void testBulkSyncOnetimeSyncForCrudAndDomainEvents() throws Exception {
		setupSearch();

		Map<String, Object> arguments = new HashMap<>();
		arguments.put("fromDate", "2019-11-01");
		arguments.put("toDate", "2020-01-01");
		arguments.put("syncToSearch", true);
		arguments.put("onetimeSync", true);

		when(_featureFlagService.getBoolean(FeatureFlags.PLTDP_CRUD_DOMAIN_BULKSYNC, false)).thenReturn(true);
		when(_domainAuditEventsUtil.getDomainEventActions())
				.thenReturn(new HashSet<>(Arrays.asList("update", "create", "delete", "SAVED_SEARCH_UPDATE_PASSED")));

		_sut.bulkSyncAudit(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, object -> "",
				() -> false, 5, 3, arguments);
		verify(_context, times(1)).search(contains("count"), anyMap(), any());
		verify(_context, times(2)).search(contains("select id"), anyMap(), any());

		//Once in the count query. 1 time each in select query for 2 batches. Total 3
		verify(_context, times(3)).search(contains("action IN"), anyMap(), any());
	}

	@Test
	public void testBulkSyncWhitelisted() throws Exception {
		setupSearch();

		_sut.bulkSyncAudit(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, object -> "",
				() -> false, 5, 3, _arguments);
		verify(_context, times(1)).search(contains("count"), anyMap(), any());
		verify(_context, times(2)).search(contains("select id"), anyMap(), any());
		//Total number of records processed will be 5. Batch size is 3. So, 2 files have to be sent.

		verify(_bulkFirehoseWriter, times(5)).writeLine(any(), anyString(), anyObject(), anyBoolean(), any());
		verify(_bulkFirehoseWriter, times(2)).sendBatch(anyString(), any(), anyBoolean(), any());
	}

	@Test
	public void testBulkSynTocSearch() throws Exception {
		setupSearch();

		_arguments.put("syncToSearch", true);
		_arguments.put("useCompression", true);

		_sut.bulkSyncAudit(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, object -> "",
				() -> false, 5, 3, _arguments);
		verify(_context, times(1)).search(contains("count"), anyMap(), any());
		verify(_context, times(2)).search(contains("select id"), anyMap(), any());
		//Total number of records processed will be 5. Batch size is 3. So, 2 files have to be sent.

		verify(_bulkSearchWriter, times(5)).writeLine(any(), anyString(), anyObject(), anyBoolean(), any());
		verify(_bulkSearchWriter, times(2)).sendBatch(anyString(), any(), anyBoolean(), any());
	}

	@Test
	public void testBulkSynTocSearchNullStatus() throws Exception {
		setupSearch();
		when(_bulkUploadUtil.getLastUploadStatus(any())).thenReturn(null).thenReturn(AuditTimeAndIdsRecord.of(_lastUploaded.getTime(), "id_12345"));
		when(_bulkUploadUtil.getLastSyncToSearchStatus(any())).thenReturn(null).thenReturn(AuditTimeAndIdsRecord.of(_lastUploaded.getTime(), "id_12345"));

		_arguments.put("syncToSearch", true);
		_arguments.put("useCompression", true);

		_sut.bulkSyncAudit(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, object -> "",
				() -> false, 5, 3, _arguments);
		verify(_context, times(1)).search(contains("count"), anyMap(), any());
		verify(_context, times(2)).search(contains("select id"), anyMap(), any());
		//Total number of records processed will be 5. Batch size is 3. So, 2 files have to be sent.

		verify(_bulkSearchWriter, times(5)).writeLine(any(), anyString(), anyObject(), anyBoolean(), any());
		verify(_bulkSearchWriter, times(2)).sendBatch(anyString(), any(), anyBoolean(), any());
	}

	/**
	 * Test to validate records are bulk synced when "created" field is present in records using archive table.
	 *
	 * There are a total of 5 records expected to be bulk synced. They need to be done in 2 batches
	 */
	@Test
	public void testBulkSyncArchive() throws Exception {
		setupSearch();

		when(_context.search(contains("limit 1"), anyMap(), any())).thenReturn(_iterator);

		_sut.bulkSyncAudit(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT_ARCHIVE, object -> "",
				() -> false, 5, 3, _arguments);

		verify(_context, times(1)).search(contains("count"), anyMap(), any());
		verify(_context, times(3)).search(contains("select id"), anyMap(), any());
		verify(_bulkFirehoseWriter, times(5)).writeLine(any(), anyString(), anyObject(), anyBoolean(), any());
		verify(_bulkFirehoseWriter, times(2)).sendBatch(anyString(), any(), anyBoolean(), any());
	}

	/**
	 * Test when the projection query returns less than batch size
	 */
	@Test
	public void testBulkSyncWithCreatedEdgeCase2() throws Exception {
		setupSearchSingle();
		_sut.bulkSyncAudit(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, object -> "",
				() -> false, 10, 3, _arguments);
		verify(_context, times(1)).search(contains("count"), anyMap(), any());
		verify(_context, times(1)).search(contains("select id"), anyMap(), any());
		verify(_bulkFirehoseWriter, times(3)).writeLine(any(), anyString(), anyObject(), anyBoolean(), any());
		verify(_bulkFirehoseWriter, times(1)).sendBatch(anyString(), any(), anyBoolean(), any());
	}

	/**
	 * Test when the job is cancelled
	 * () -> true
	 */
	@Test
	public void testBulkSyncWithCreatedEdgeCase3() throws Exception {
		_sut.bulkSyncAudit(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, object -> "",
				() -> true, 10, 3, _arguments);
		verify(_crudService, times(0)).findAll(any(), any(QueryOptions.class));
		verify(_bulkFirehoseWriter, times(0)).writeLine(any(), anyString(), anyObject(), anyBoolean(), any());
		verify(_bulkFirehoseWriter, times(0)).sendBatch(anyString(), any(), anyBoolean(), any());
	}

	/**
	 * Test when the job is cancelled
	 * () -> true
	 */
	@Test
	public void testBulkSyncWithCreatedEdgeCase3_1() throws Exception {
		_arguments.put("onetimeSync", false);
		_sut.bulkSyncAudit(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, object -> "",
				() -> true, 10, 3, _arguments);
		verify(_crudService, times(0)).findAll(any(), any(QueryOptions.class));
		verify(_bulkFirehoseWriter, times(0)).writeLine(any(), anyString(), anyObject(), anyBoolean(), any());
		verify(_bulkFirehoseWriter, times(0)).sendBatch(anyString(), any(), anyBoolean(), any());
	}

	/**
	 * Test when created field is null on one of the audit events; in this case the first one
	 */
	@Test
	public void testBulkSyncWithCreatedEdgeCase4() throws Exception {
		setupSearchNull();
		_sut.bulkSyncAudit(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, object -> "",
				() -> false, 5, 3, _arguments);
		verify(_context, times(1)).search(contains("count"), anyMap(), any());
		verify(_context, times(2)).search(contains("select id"), anyMap(), any());
		verify(_bulkFirehoseWriter, times(5)).writeLine(any(), anyString(), anyObject(), anyBoolean(), any());
		verify(_bulkFirehoseWriter, times(2)).sendBatch(anyString(), any(), anyBoolean(), any());
	}

	/**
	 * Test when crudService throws RunTimeException
	 */
	@Test
	public void testBulkSyncWithCreatedEdgeCase5() throws Exception {
		setupSearchSingleError();

		try {
			_sut.bulkSyncAudit(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, object -> "",
				() -> false, 10, 3, _arguments);
		} catch (Exception e) {
			verify(_context, times(1)).search(contains("count"), anyMap(), any());
			verify(_context, times(1)).search(contains("select id"), anyMap(), any());
			//Total number of records processed will be 5. Batch size is 3. So, 2 files have to be sent.
			verify(_bulkFirehoseWriter, times(0)).writeLine(any(), anyString(), anyObject(), anyBoolean(), any());
			verify(_bulkFirehoseWriter, times(0)).sendBatch(anyString(), any(), anyBoolean(), any());
		}
	}

	/**
	 * Transformer returns null. So, no audit events get added and no file get uploaded even though crudservice returns
	 * records
	 */
	@Test
	public void testBulkSyncWithCreatedEdgeCase6() throws Exception {
		setupSearchSingle();

		//Returning null in the method call "object -> null"
		_sut.bulkSyncAudit(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, object -> null,
				() -> false, 5, 3, _arguments);
		verify(_context, times(1)).search(contains("count"), anyMap(), any());
		verify(_context, times(1)).search(contains("select id"), anyMap(), any());
		try {
			//Total number of records processed will be 5. Batch size is 3. So, 2 files have to be sent.
			verify(_bulkFirehoseWriter, times(3)).writeLine(any(), anyString(), anyObject(), anyBoolean(), any());
			verify(_bulkFirehoseWriter, times(1)).sendBatch(anyString(), any(), anyBoolean(), any());
		} catch (IOException e) {

		}
	}

	/**
	 * Test IOException scenario when either createTempFile or uploadUncompressedFile method throws Exception
	 */
	@Test
	public void testBulkSyncWithCreatedEdgeCase7() throws Exception {
		setupSearchSingle();

		try {
			when(_searchUtil.uploadUncompressedFile(any(), any(), any(), any())).thenThrow(new IOException());
		} catch (IOException e) {

		}

		try {
			_sut.bulkSyncAudit(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, object -> "",
				() -> false, 5, 3, _arguments);
		} catch (IOException e) {
			verify(_crudService, times(2)).getContext();

			//Total number of records processed will be 5. Batch size is 3. So, 2 files have to be sent.
			verify(_bulkFirehoseWriter, times(4)).writeLine(any(), anyString(), anyObject(), anyBoolean(), any());
			verify(_bulkFirehoseWriter, times(2)).sendBatch(anyString(), any(), anyBoolean(), any());
		}
	}

	@Test
	public void testPurgeSyncFiles() {
		_arguments.put("fromDate", "2019-11-01");
		_arguments.put("toDate", "2021-01-01");

		when(_atlasConfig.getString(eq("AER_AUDIT_PARQUET_DATA_S3_BUCKET")))
				.thenReturn("somebucket");

		when(_atlasConfig.getAwsRegion()).thenReturn(Regions.US_EAST_1);

		doNothing()
				.when(_remoteFileService)
				.deleteMultipleObjects(isA(String.class), isA(String[].class) , isA(String.class));

		when(_remoteFileService.listFiles(anyString(), anyString()))
				.thenReturn(new ArrayList<>(Arrays.asList("bulk_event_upload1", "bulk_event_upload2")));

		_sut.purgeBulkSyncFiles(_arguments, () -> false);

		verify(_remoteFileService, times(4))
				.deleteMultipleObjects(anyString(), any(), anyString());

		_arguments.remove("fromDate");
		_sut.purgeBulkSyncFiles(_arguments, () -> false);
		verify(_remoteFileService, times(8))
				.deleteMultipleObjects(anyString(), any(), anyString());

		_arguments.put("fromDate", "2020-01-01");
		_sut.purgeBulkSyncFiles(_arguments, () -> false);
		verify(_remoteFileService, times(12))
				.deleteMultipleObjects(anyString(), any(), anyString());

		_arguments.remove("toDate");
		_sut.purgeBulkSyncFiles(_arguments, () -> false);
		verify(_remoteFileService, times(16))
				.deleteMultipleObjects(anyString(), any(), anyString());
		verify(_auditEventService, times(1)).updateCheckpoint(anyLong(), anyString(), anyString());
	}

	@Test
	public void testPurgeSyncFilesWithFireHouseStatusNull() throws Exception {
		when(_bulkUploadUtil.getFirstAuditParquetCheckpoint()).thenReturn(null);
		_sut.purgeBulkSyncFiles(_arguments, () -> false);
		verify(_remoteFileService, never())
				.deleteMultipleObjects(anyString(), any(), anyString());
	}

	@Test
	public void testCount() throws Exception {
		_arguments.put("syncToSearch", false);
		setupSearch();
		assertEquals(10, _sut.countAuditRecords(_arguments));
	}

	@Test
	public void testCreateArchiveIndex() throws Exception {
		when(_iterator.hasNext()).thenReturn(true);
		when(_iterator.next()).thenReturn(new BigInteger("0"));
		_sut.createArchiveIndex();
		verify(_statement, times(1)).execute(anyString());
	}

	@Test
	public void testCreateArchiveIndexException1() throws Exception {
		when(_iterator.hasNext()).thenReturn(true);
		when(_iterator.next()).thenReturn(new BigInteger("0"));
		when(_context.getJdbcConnection()).thenReturn(_connection);
		when(_connection.createStatement()).thenReturn(_statement);
		when(_statement.execute(anyString())).thenThrow(_mySQLSyntaxErrorException);
		_sut.createArchiveIndex();
		verify(_log, times(1)).error(anyString(), anyObject());
	}

	@Test
	public void testCreateArchiveIndexException2() throws Exception {
		when(_iterator.hasNext()).thenReturn(true);
		when(_iterator.next()).thenReturn(new BigInteger("0"));
		when(_context.getJdbcConnection()).thenReturn(_connection);
		when(_connection.createStatement()).thenReturn(_statement);
		when(_mySQLSyntaxErrorException.getErrorCode()).thenReturn(1061);
		when(_statement.execute(anyString())).thenThrow(_mySQLSyntaxErrorException);
		_sut.createArchiveIndex();
		verify(_log, times(2)).info(anyString());
	}

	@Test
	public void testCreateArchiveIndexException3() throws Exception {
		when(_iterator.hasNext()).thenReturn(true);
		when(_iterator.next()).thenReturn(new BigInteger("0"));
		when(_context.getJdbcConnection()).thenReturn(_connection);
		when(_connection.createStatement()).thenReturn(_statement);
		when(_statement.execute(anyString())).thenThrow(new RuntimeException());
		_sut.createArchiveIndex();
		verify(_log, times(1)).error(anyString(), anyObject());
	}

	@Test
	public void testPurgeSearchIndex() throws Exception  {
		_sut.purgeSearchIndex(_syncTransformer);
		verify(_bulkWriterFactory, times(1)).getWriter(anyObject(), eq(true));
	}

	@Test(expected = Exception.class)
	public void testBulkSyncAuditWithFirehouseStatusNull() throws Exception {
		setupSearch();

		when(_bulkUploadUtil.getFirstAuditParquetCheckpoint()).thenReturn(null);
		_sut.bulkSyncAudit(BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT, object -> "",
				() -> false, 5, 3, _arguments);
	}

	public void setupSearch() throws Exception {
		List<Object[]> objects1 = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			objects1.add(getAuditObject(i));
		}
		List<Object[]> objects2 = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			objects2.add(getAuditObject(i+3));
		}

		when(_bulkUploadUtil.getString(eq("audit.upload.start.date")))
				.thenReturn("2020-05-01");
		when(_iterator.hasNext()).thenReturn(true);
		when(_iterator.next()).thenReturn(new BigInteger("5"));
		when(_context.search(contains("count"), anyMap(), any())).thenReturn(_iterator);
		when(_context.search(contains("select id"), anyMap(), any())).thenReturn(objects1.iterator(),objects2.iterator());
	}

	public void setupSearchSingle() throws Exception {
		List<Object[]> objects1 = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			objects1.add(getAuditObject(i));
		}

		when(_iterator.hasNext()).thenReturn(true);
		when(_iterator.next()).thenReturn(new BigInteger("3"));
		when(_context.search(contains("count"), anyMap(), any())).thenReturn(_iterator);
		when(_context.search(contains("select id"), anyMap(), any())).thenReturn(objects1.iterator());
	}

	public void setupSearchSingleError() throws Exception {
		List<Object[]> objects1 = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			objects1.add(getAuditObject(i));
		}

		when(_iterator.hasNext()).thenReturn(true);
		when(_iterator.next()).thenReturn(new BigInteger("3"));
		when(_context.search(contains("count"), anyMap(), any())).thenReturn(_iterator);
		when(_context.search(contains("select id"), anyMap(), any()))
				.thenThrow(new GeneralException())
				.thenReturn(objects1.iterator());

		when(_crudService.getContext())
				.thenReturn(_context);
	}

	public void setupSearchNull() throws Exception {
		List<Object[]> objects1 = new ArrayList<>();
		for (int i = 0; i < 1; i++) {
			Object[] o = getAuditObject(i);
			o[1]=null;
			objects1.add(o);
		}
		for (int i = 0; i < 2; i++) {
			objects1.add(getAuditObject(i+1));
		}
		List<Object[]> objects2 = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			objects2.add(getAuditObject(i+1+2));
		}

		when(_iterator.hasNext()).thenReturn(true);
		when(_iterator.next()).thenReturn(new BigInteger("5"));
		when(_context.search(contains("count"), anyMap(), any())).thenReturn(_iterator);
		when(_context.search(contains("select id"), anyMap(), any())).thenReturn(objects1.iterator(),objects2.iterator());
	}

	public static Object[] getAuditObject(int id) {
		Object[] o = new Object[11];
		o[0]="id"+id;
		o[1]=new BigInteger("0");
		o[2]="action";
		o[3]="source";
		o[4]="target";
		o[5]="application";
		o[6]="requestId";
		o[7]="idAddr";
		o[8]="contectId";
		o[9]="info";
		o[10]="";
		return o;
	}
}
