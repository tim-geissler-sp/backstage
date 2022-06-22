/*
 *
 * Copyright (c) 2020. SailPoint Technologies, Inc.  All rights reserved.
 *
 */
package com.sailpoint.audit.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.atlas.service.RemoteFileService;
import com.sailpoint.audit.service.mapping.DomainAuditEventsUtil;
import com.sailpoint.audit.service.model.AuditTimeAndIdsRecord;
import com.sailpoint.audit.service.model.AuditUploadStatus;
import com.sailpoint.audit.service.model.BulkUploadDTO;
import com.sailpoint.audit.service.util.AuditUtil;
import com.sailpoint.audit.util.AuditMetricUtil;
import com.sailpoint.audit.util.BulkUploadUtil;
import com.sailpoint.audit.writer.BulkSearchWriter;
import com.sailpoint.audit.writer.BulkWriter;
import com.sailpoint.audit.writer.BulkWriterFactory;
import com.sailpoint.mantis.core.service.ConfigService;
import com.sailpoint.mantis.core.service.CrudService;
import com.sailpoint.mantis.platform.api.interfaces.Persistence;
import com.sailpoint.mantis.platform.service.search.BulkSynchronizationService;
import com.sailpoint.mantis.platform.service.search.SearchUtil;
import com.sailpoint.mantis.platform.service.search.SyncTransformer;
import com.sailpoint.metrics.MetricsUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sailpoint.api.SailPointContext;
import sailpoint.object.Attributes;
import sailpoint.object.AuditEvent;
import sailpoint.object.SailPointObject;
import sailpoint.tools.GeneralException;
import sailpoint.tools.xml.XMLObjectFactory;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.sailpoint.audit.event.AuditEventS3Handler.AUDITEVENT_PARQUET_TIMESTAMP;

/**
 *
 * This is the code that queries CIS so that it is common and optimized and consistent.
 *
 * There are 2 writers:
 *  - One writes to ES by uploading to S3 intermediate temporary file that is purged on ingestion to ES.
 *  - The other writes to FireHose service which constructs batches of 10k records to FireHose.
 *
 *  On the `spt_audit_event_archive` table:
 *  - This was a need-based execution on-demand for 20 orgs the first time, then fewer than 5 orgs after 6 months.
 *    The code is still here in cis-core repository; it is not a scheduled job.  Jeff and Dustin were the orchestration
 *    engineers behind it.  The script runs in a docker container to do the back-port of the data.  Highly manually
 *    initiated process.  Docker container wakes up, does its thing, avoid having to add it to a micro-service.
 *    Was a GoLang module; and it processes slowly to not put a bunch of load on the CIS database. It is very
 *    conservative.
 *
 * The table is *important* and the records must be moved over to the S3 by-time persistence records.
 *
 * Two new methods are added to support bulk Sync of CIS to S3 by-time persistece:
 *
 *     1) getAuditEventCountsByDateRange
 *     2) getAuditEventIdCreatedByDateRange
 *
 * These can be used to hour-by-hour or day-by-day or month-by-month iterate through AuditEvents housed in
 * CIS and move them over to S3.  Use of that will come in a future PR.
 */
@Singleton
public class BulkUploadAuditEventsService extends BulkSynchronizationService {

	public static Log _log = LogFactory.getLog(BulkUploadAuditEventsService.class);

	private final static String METRICS_BASE_PATH = "com.sailpoint.audit.bulk.upload";

	private final static String GET_AUDIT_EVENTS_SQL = "sql:select id,created,action,source,target,application," +
			"tracking_id,string2,string3,string4,attributes from %s";

	private final static String COUNT_AUDIT_EVENTS_SQL = "sql:select count(1) from %s";

	private final static String CREATE_AUDIT_EVENT_ARCHIVE_INDEX =
			"ALTER TABLE spt_audit_event_archive ADD index spt_audit_event_archive_created (created), LOCK = NONE, ALGORITHM = INPLACE;";

	private final static String COUNT_AUDIT_EVENTS_BY_TIME_SQL =
			"SELECT COUNT(sae.id) AS num_rows FROM spt_audit_event sae WHERE created >=? AND created <? ;";

	private final static String COUNT_AUDIT_EVENTS_BY_TIME_WITH_ARCHIVE_SQL =
			"SELECT SUM(mySumTable.num_rows) AS num_rows FROM ( " +
			"  SELECT COUNT(sae.id)  AS num_rows FROM spt_audit_event         sae  WHERE created >=? AND created <? " +
			"  UNION ALL " +
			"  SELECT COUNT(saea.id) AS num_rows FROM spt_audit_event_archive saea WHERE created >=? AND created <? " +
			") mySumTable ;";

	private final static String GET_AUDIT_EVENT_ID_CREATED_BY_TIME_SQL =
			"SELECT id, created FROM spt_audit_event sae WHERE created >=? AND created <? ;";

	private final static String GET_AUDIT_EVENT_ID_CREATED_BY_TIME_WITH_ARCHIVE_SQL =
			"SELECT id, created FROM ( " +
			"  SELECT sae.id  AS id,  sae.created AS created FROM spt_audit_event         sae  WHERE created >=? AND created <? " +
			"  UNION ALL " +
			"  SELECT saea.id AS id, saea.created AS created FROM spt_audit_event_archive saea WHERE created >=? AND created <? " +
			") mySumTable ;";

	private final static String GET_AUDIT_EVENT_BY_ID =
			"SELECT id, created, action, source, target, application, tracking_id, " +
			"       string1, string2, string3, string4, attributes " +
			"  FROM %s WHERE id = ?";

	public final static String EVENT_TYPE = "event";

	public enum AuditTableNames {
		AUDIT_EVENT("spt_audit_event"),
		AUDIT_EVENT_ARCHIVE("spt_audit_event_archive");

		private String tableName;

		AuditTableNames(String tableName)
		{
			this.tableName = tableName;
		}

		public String getTableName()
		{
			return this.tableName;
		}
	}

	// Maintain the status of to where we have uploaded, so when we want to query the next chunck we
	// want to know where to start.  This is a key and we insert a record in the `configuration` table
	// in CIS, and a it houses a combination of a time stamp and a set of CIS IDs that have been uploaded
	// and this record contains the time stamp and ID list.
	public enum PhaseStatus {
		BULK_UPLOAD_STATUS("bulk_upload_status"),
		SEARCH_SYNC("search_sync_status");

		private String phaseStatus;

		PhaseStatus(String phaseStatus)
		{
			this.phaseStatus = phaseStatus;
		}

		public String getPhaseStatus(AuditTableNames tableName)
		{
			return tableName.getTableName()+"_"+this.phaseStatus;
		}
	}

	@Inject
	AtlasConfig _atlasConfig;

	@Inject
	BulkUploadUtil _bulkUploadUtil;

	@Inject
	BulkWriterFactory _bulkWriterFactory;

	@Inject
	ConfigService _configService;

	@Inject
	CrudService _crudService;

	@Inject
	FeatureFlagService _featureFlagService;

	@Inject
	Persistence _persistence;

	@Inject
	RemoteFileService _remoteFileService;

	@Inject
	SearchUtil _searchUtil;

	@Inject
	AmazonS3 _s3Client;

	@Inject
	DomainAuditEventsUtil _domainAuditEventsUtil;

	@Inject
	AuditEventService _auditEventService;

	private AuditMetricUtil _auditMetricUtil = new AuditMetricUtil(METRICS_BASE_PATH);

	/**
	 * Upload audit events into S3
	 *
	 * @param tableName
	 * @param transformer The SyncTransformer to use to convert the object to the search document.
	 * @param cancelledFunction Supplier function to dynamically get the boolean value
	 * @param recordLimit
	 * @param batchSize Number of records to be processed in each batch. Pass <= 0 if you want to use default batch
	 *                  size of 10000
	 * @param arguments Uses fromDate and toDate to construct archive table key filter and the config store key for status
	 */
	public int bulkSyncAudit(AuditTableNames tableName,
												 SyncTransformer transformer,
												 Supplier<Boolean> cancelledFunction,
												 int recordLimit, int batchSize,
												 Map<String, Object> arguments) throws Exception {
		RequestContext requestContext = RequestContext.ensureGet();
		final String org = requestContext.getOrg();
		boolean useCompression = (boolean)arguments.getOrDefault("useCompression", false);
		boolean syncToSearch = (boolean)arguments.getOrDefault("syncToSearch", false);
		boolean onetimeSync = (boolean)arguments.getOrDefault("onetimeSync", false);

		BulkWriter bulkWriter = _bulkWriterFactory.getWriter(transformer, syncToSearch);

		AuditUploadStatus status = new AuditUploadStatus();
		Set<String> firehoseLastIds = new HashSet<>();
		Set<String> lastIds = new HashSet<>();

		status.setUploadToSearch(syncToSearch);
		status.setOnetimeSync(onetimeSync);

		// Retrieve firehose status for to date and ids to exclude
		long toDate;
		LocalDate localTo = arguments.containsKey("toDate") ? AuditUtil.toDate(arguments, "toDate") : null;
		if (!syncToSearch) {
			AuditTimeAndIdsRecord firehoseStatus = _bulkUploadUtil.getFirstAuditParquetCheckpoint();
			if (firehoseStatus == null) {
				_log.error("firehoseStatus is missing - cannot continue");
				throw new Exception("firehoseStatus is missing - cannot continue");
			}
			if (firehoseStatus.getIds() != null) {
				firehoseLastIds = new HashSet<>(firehoseStatus.getIds());
				lastIds.addAll(firehoseLastIds);
			}
			toDate = localTo != null ? localTo.atStartOfDay(ZoneId.of("GMT")).toInstant().toEpochMilli() : firehoseStatus.getTimestamp();
		} else {
			AuditTimeAndIdsRecord syncToSearchStart = _bulkUploadUtil.getSyncToSearchStart();
			if (syncToSearchStart == null) {
				syncToSearchStart = new AuditTimeAndIdsRecord();
				syncToSearchStart.setTimestamp(System.currentTimeMillis());
				if (!onetimeSync) {
					_bulkUploadUtil.setSyncToSearchStart(syncToSearchStart);
				}
			}
			toDate = localTo != null ? localTo.atStartOfDay(ZoneId.of("GMT")).toInstant().toEpochMilli() : syncToSearchStart.getTimestamp();
		}

		// Retrieve last upload status for from date and ids to exclude
		AuditTimeAndIdsRecord lastUploadStatus = syncToSearch ? _bulkUploadUtil.getLastSyncToSearchStatus(tableName) :
				_bulkUploadUtil.getLastUploadStatus(tableName);
		if (lastUploadStatus != null) {
			if (lastUploadStatus.getIds() != null) {
				lastIds.addAll(lastUploadStatus.getIds());
			}
			status.setLastCreatedTime(lastUploadStatus.getTimestamp());
			status.setTotalProcessed(lastUploadStatus.getTotalProcessed());
			status.setTotalUploaded(lastUploadStatus.getTotalUploaded());
		}
		LocalDate localFrom = arguments.containsKey("fromDate") ? AuditUtil.toDate(arguments, "fromDate") : null;
		if (status.getLastCreatedTime()==0 && localFrom != null) {
			status.setLastCreatedTime(localFrom.atStartOfDay(ZoneId.of("GMT")).toInstant().toEpochMilli());
		}
		long fromDate;
		if (onetimeSync && localFrom != null) {
			fromDate = localFrom.atStartOfDay(ZoneId.of("GMT")).toInstant().toEpochMilli();
			status.setLastCreatedTime(fromDate);
		} else {
			fromDate = status.getLastCreatedTime();
			status.addIds(lastIds);
		}

		status.setStartTime(System.nanoTime());
		int remainingRecords = countRecordsAudit(tableName, status.getLastCreatedTime(), toDate, lastIds);
		if (remainingRecords == 0 && !onetimeSync) {
			_log.info(bulkSyncLogMessage(EVENT_TYPE,"no more records to process, marking complete."));
			if (lastUploadStatus == null) {
				lastUploadStatus = new AuditTimeAndIdsRecord();
				lastUploadStatus.setCompleted(true);
				lastUploadStatus.setTotalProcessed(status.getTotalProcessed());
				lastUploadStatus.setTotalUploaded(status.getTotalUploaded());
			} else {
				lastUploadStatus.setCompleted(true);
			}
			if (syncToSearch) {
				_bulkUploadUtil.setCurrentSyncToSearchStatus(tableName, lastUploadStatus);
			} else {
				_bulkUploadUtil.setCurrentUploadStatus(tableName, lastUploadStatus);
			}
			return remainingRecords;
		}
		status.setTotalRemaining(remainingRecords);
		if (onetimeSync) {
			recordLimit = remainingRecords;
		}
		status.setSessionLimit(Math.min(recordLimit, remainingRecords));

		_log.info(bulkSyncLogMessage(EVENT_TYPE, _bulkUploadUtil.getStartMessage(status, toDate, remainingRecords)));

		while (status.getSessionProcessed() < status.getSessionLimit() &&  !isCancelled(cancelledFunction, status)) {
			Iterator idIterator = getIterator(tableName, fromDate, toDate, lastIds, batchSize);
			while (idIterator != null && idIterator.hasNext() && !isCancelled(cancelledFunction, status)) {
				BulkUploadDTO dto = transformToBulkUploadDTO((Object[]) idIterator.next());
				SailPointObject object = AuditUtil.transformBulkEvent(dto);

				if (object.getCreated() != null) {
					status.setCreatedTime(object.getCreated().getTime());
				}

				bulkWriter.writeLine(object, org, tableName, useCompression, status);

				if (status.getLastCreatedTime() == status.getCreatedTime()) {
					status.addId(object.getId());
				} else {
					status.getLastIds().clear();
					status.addIds(firehoseLastIds);
					status.addId(object.getId());
				}
				status.setLastCreatedTime(status.getCreatedTime());
				status.incrementProcessed();

				_crudService.decache(object);
			}
			_crudService.decache();

			bulkWriter.sendBatch(org, tableName, useCompression, status);

			fromDate = status.getLastCreatedTime();
			lastIds.clear();
			lastIds.addAll(firehoseLastIds);
			lastIds.addAll(status.getLastIds());
		}
		if (!onetimeSync) {
			generateMetrics(status, tableName);
		}

		return status.getSessionProcessed();
	}

	/**
	 * Purges files from S3 until one day before the toDate or one day before first audit event through firehose
	 * @param arguments
	 * @param cancelledFunction
	 * @return
	 */
	public void purgeBulkSyncFiles(Map<String,Object> arguments, Supplier<Boolean> cancelledFunction) {
		try {
			String org = RequestContext.ensureGet().getOrg();
			
			final String sourceS3Bucket = _atlasConfig.getString("AER_AUDIT_PARQUET_DATA_S3_BUCKET");

			AuditTimeAndIdsRecord firehoseStatus = _bulkUploadUtil.getFirstAuditParquetCheckpoint();
			//If firehoseStatus is null, it means that the org was created after firehsose was enabled for audit
			if (firehoseStatus == null) {
				_log.error("firehoseStatus is missing - cannot continue");
				throw new Exception("firehoseStatus is missing - cannot continue");
			}

			LocalDate fromDatetimeOverride = null;
			if (arguments.containsKey("fromDate")) {
				String toDate = (String)arguments.get("fromDate");
				fromDatetimeOverride = LocalDate.parse(toDate);
			}

			final LocalDate fromDate = fromDatetimeOverride != null ? fromDatetimeOverride : null;

			LocalDate toDatetimeOverride = null;
			LocalDate firehoseStartDate = (LocalDateTime.ofInstant(
					Instant.ofEpochMilli(firehoseStatus.getTimestamp()), ZoneId.of("GMT"))).toLocalDate();
			if (arguments.containsKey("toDate")) {
				String toDate = (String)arguments.get("toDate");
				toDatetimeOverride = LocalDate.parse(toDate);
			}

			final LocalDate toDate = toDatetimeOverride != null ? toDatetimeOverride : firehoseStartDate;

			StringBuilder sb = new StringBuilder("parquet/org="+org+"/");

			ListObjectsRequest listPrefixRequest = new ListObjectsRequest()
					.withBucketName(sourceS3Bucket).withPrefix(sb.toString())
					.withDelimiter("/");
			ObjectListing objects = _s3Client.listObjects(listPrefixRequest);
			final List<String> prefixes = objects.getCommonPrefixes();

			List<String> types = prefixes.stream()
					.map(prefix -> prefix.substring(prefix.lastIndexOf("=") + 1, prefix.lastIndexOf("/")))
					.collect(Collectors.toList());

			types.forEach(type -> {
				StringBuilder sbPrefix = new StringBuilder(sb);
				sbPrefix.append("type=" + type + "/");

				ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
						.withBucketName(sourceS3Bucket).withPrefix(sbPrefix.toString())
						.withDelimiter("/");
				ObjectListing dateObjects = _s3Client.listObjects(listObjectsRequest);
				List<String> dateStrings = dateObjects.getCommonPrefixes();

				dateStrings.forEach(datePartitionString -> {
					if (isCancelled(cancelledFunction, "purge bulk sync files cancelled")) {
						return;
					}
					String dateString = datePartitionString
							.substring(datePartitionString.lastIndexOf("=") + 1, datePartitionString.lastIndexOf("/"));
					LocalDate partitionDate = LocalDate.parse(dateString);
					//From and toDates are inclusive
					//Deletes the files on the toDate. We want the files to be deleted when first audit event through
					//firehose was sent
					LocalDate toDateTemp = toDate.plusDays(1);
					LocalDate fromDateTemp = fromDate == null ? null : fromDate.minusDays(1);
					if (partitionDate.isBefore(toDateTemp) &&
							(fromDate == null || partitionDate.isAfter(fromDateTemp)) ) {

						String prefix = sbPrefix.toString() + "date=" + dateString + "/";

						String[] prefixKeys = { prefix };
						_remoteFileService.deleteMultipleObjects(sourceS3Bucket,
								prefixKeys,
								AuditUtil.getCurrentRegion(_atlasConfig.getAwsRegion().getName()));
					}
				});

			});

			if (toDatetimeOverride == null) {
				_auditEventService.updateCheckpoint(
						//Converting to millis. Adding 1 day to set to the start of the next day
						firehoseStartDate.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000,
						null,
						AUDITEVENT_PARQUET_TIMESTAMP);
			}

			_log.info(bulkSyncLogMessage(EVENT_TYPE,"bulk event upload file purge complete"));
		} catch (Exception e) {
			_log.error(bulkSyncLogMessage(EVENT_TYPE,"error purging bulk event upload files"), e);
		}
	}

	public void purgeSearchIndex(SyncTransformer transformer) throws Exception {
		RequestContext requestContext = RequestContext.ensureGet();
		final String org = requestContext.getOrg();
		BulkSearchWriter BulkSearchWriter = (BulkSearchWriter) _bulkWriterFactory.getWriter(transformer, true);
		BulkSearchWriter.purge(org);
	}

	public int countAuditRecords(Map<String, Object> arguments) {
		boolean syncToSearch = (boolean)arguments.getOrDefault("syncToSearch", false);

		int auditRecordCount = 0;
		int archiveAuditRecordCount = 0;
		try {
			long fromDate = 0;
			long toDate = 0;
			Set<String> firehoseLastIds;
			Set<String> lastIds = new HashSet<>();
			if (!syncToSearch) {
				AuditTimeAndIdsRecord firehoseStatus = getFirehoseStatus();

				if (firehoseStatus.getIds() != null) {
					firehoseLastIds = new HashSet<>(firehoseStatus.getIds());
					lastIds.addAll(firehoseLastIds);
				}

				toDate = getLastFirehoseWrittenTimestamp(firehoseStatus, arguments);
				LocalDate localFrom = LocalDate.parse(_bulkUploadUtil.getString("audit.upload.start.date"));

				_log.info("From date " + localFrom.toString() + " To date for count "
						+ LocalDateTime.ofEpochSecond(toDate/1000, 0, ZoneOffset.UTC).toString());
				fromDate = localFrom.atStartOfDay(ZoneId.of("GMT")).toInstant().toEpochMilli();
			} else {
				LocalDate localFrom = AuditUtil.toDate(arguments, "fromDate");
				fromDate = localFrom.atStartOfDay(ZoneId.of("GMT")).toInstant().toEpochMilli();
			}
			auditRecordCount = countRecordsAudit(AuditTableNames.AUDIT_EVENT, fromDate, toDate, lastIds);
			archiveAuditRecordCount = countRecordsAudit(AuditTableNames.AUDIT_EVENT_ARCHIVE, fromDate, toDate, lastIds);
			generateCountMetrics(auditRecordCount, archiveAuditRecordCount, syncToSearch, false);
		} catch (Exception e) {
			_log.error(bulkSyncLogMessage(EVENT_TYPE, "error getting count"), e);
		}
		return auditRecordCount + archiveAuditRecordCount;
	}

	public int countAllAuditRecords(Map<String, Object> arguments) {
		boolean syncToSearch = (boolean)arguments.getOrDefault("syncToSearch", false);

		int auditRecordCount = 0;
		int archiveAuditRecordCount = 0;
		try {
			long toDate = 0;
			Set<String> firehoseLastIds;
			Set<String> lastIds = new HashSet<>();
			if (!syncToSearch) {
				AuditTimeAndIdsRecord firehoseStatus = getFirehoseStatus();

				if (firehoseStatus.getIds() != null) {
					firehoseLastIds = new HashSet<>(firehoseStatus.getIds());
					lastIds.addAll(firehoseLastIds);
				}

				toDate = getLastFirehoseWrittenTimestamp(firehoseStatus, arguments);

				auditRecordCount = countAllAuditRecords(AuditTableNames.AUDIT_EVENT, toDate);
				archiveAuditRecordCount = countAllAuditRecords(AuditTableNames.AUDIT_EVENT_ARCHIVE, toDate);
				generateCountMetrics(auditRecordCount, archiveAuditRecordCount, syncToSearch, true);
			}

		} catch (Exception e) {
			_log.error(bulkSyncLogMessage(EVENT_TYPE, "error getting count"), e);
		}
		return auditRecordCount + archiveAuditRecordCount;
	}

	private AuditTimeAndIdsRecord getFirehoseStatus() throws Exception {
		AuditTimeAndIdsRecord firehoseStatus = _bulkUploadUtil.getFirstAuditParquetCheckpoint();
		if (firehoseStatus == null) {
			_log.error("firehoseStatus is missing - cannot continue");
			throw new Exception("firehoseStatus is missing - cannot continue");
		}
		return firehoseStatus;
	}

	private Long getLastFirehoseWrittenTimestamp(AuditTimeAndIdsRecord firehoseStatus, Map<String, Object> arguments){
		long toDate = 0;
		long toDateLong = firehoseStatus.getTimestamp();
		if (toDateLong != 0) {
			toDate = toDateLong;
		} else {
			LocalDate localTo = AuditUtil.toDate(arguments, "toDate");
			toDate = arguments.get("toDate") != null ? localTo.atStartOfDay(ZoneId.of("GMT")).toInstant().toEpochMilli() : firehoseStatus.getTimestamp();
		}
		return toDate;
	}

	public void createArchiveIndex() {
		try {
			SailPointContext ctx = _persistence.getContext();
			if (ctx != null) {
				Connection conn = ctx.getJdbcConnection();
				Statement statement = conn.createStatement();
				_log.info("starting execution of audit event archive created index");
				statement.execute(CREATE_AUDIT_EVENT_ARCHIVE_INDEX);
				_log.info("audit event archive created index execution complete");
			} else {
				_log.error("error getting context");
			}
		} catch (MySQLSyntaxErrorException sqlException) {
			if (sqlException.getErrorCode()==1061) {
				_log.info("audit event archive created index already exists");
			} else {
				_log.error("error creating audit event archive index", sqlException);
			}
		} catch (Exception e) {
			_log.error("error creating audit event archive index", e);
		}
	}

	public int countRecordsAudit(AuditTableNames tableNames, long fromDate, long toDate, Set<String> idSet) throws GeneralException {
		int countRecords = 0;

		if (tableNames.equals(AuditTableNames.AUDIT_EVENT_ARCHIVE) && !archiveTableExists()) {
			return 0;
		}

		Iterator iterator = _crudService.getContext()
				.search(String.format(COUNT_AUDIT_EVENTS_SQL, tableNames.getTableName())
								+ getWhereClause(fromDate, toDate, idSet)
								+ getOrderBy(),
						null, null);
		if (iterator != null && iterator.hasNext()) {
			long count = ((BigInteger)iterator.next()).longValue();
			countRecords = (int)count;
		}

		return countRecords;
	}

	public int countAllAuditRecords(AuditTableNames tableNames, long toDate) throws GeneralException {
		int countRecords = 0;

		if (tableNames.equals(AuditTableNames.AUDIT_EVENT_ARCHIVE) && !archiveTableExists()) {
			return 0;
		}

		Iterator iterator = _crudService.getContext()
				.search(String.format(COUNT_AUDIT_EVENTS_SQL, tableNames.getTableName())
								+ getWhereClause(0, toDate, null),
						null, null);
		if (iterator != null && iterator.hasNext()) {
			long count = ((BigInteger)iterator.next()).longValue();
			countRecords = (int)count;
		}

		return countRecords;
	}

	private boolean isCancelled(Supplier<Boolean> cancelledFunction, AuditUploadStatus status) {
		return isCancelled(cancelledFunction,
				bulkSyncLogMessage(EVENT_TYPE, "job got cancelled after processing " + status.getSessionProcessed()));
	}

	private boolean isCancelled(Supplier<Boolean> cancelledFunction, String message) {
		boolean isJobCancelled = cancelledFunction.get();
		if (isJobCancelled) {
			_log.info(message);
			return true;
		}
		return false;
	}

	private boolean archiveTableExists() {
		try {
			_crudService.getContext()
					.search("sql:select id from spt_audit_event_archive limit 1", null, null);
			return true;
		} catch (GeneralException e) {
			_log.debug(bulkSyncLogMessage(EVENT_TYPE, "audit archive table does not exist"));
		}
		return false;
	}

	// The use of the archive table, which is outside the Hibernate bindings, requires us to go
	// by-column to query the audit events and reconstruct them.
	// AEH: This *might* be an integrity concern; lots of columns were pushed down into the attributes XML/CLOB.
	private Iterator getIterator(AuditTableNames tableName, long fromDate, long toDate, Set<String> idSet, int batchSize)
			throws GeneralException {
		Iterator iterator = _crudService.getContext()
					.search(String.format(GET_AUDIT_EVENTS_SQL, tableName.getTableName())
									+ getWhereClause(fromDate, toDate, idSet)
									+ getOrderBy()
									+ getLimit(batchSize),
							null, null);
		return iterator;
	}

	private String getOrderBy() {
		StringBuilder sb = new StringBuilder(" ORDER BY created, id ");
		return sb.toString();
	}

	private String getLimit(int batchSize) {
		StringBuilder sb = new StringBuilder(" LIMIT " + batchSize);
		return sb.toString();
	}

	private String getWhereClause(long fromDate, long toDate, Set<String> idSet) {
		StringBuilder sb = new StringBuilder(" WHERE");
		boolean needsAnd = false;
		if (toDate!=0) {
			sb.append(" created <= ").append(toDate);
			needsAnd = true;
		}
		if (fromDate!=0) {
			if (needsAnd) {
				sb.append(" AND");
			}
			sb.append(" created >= ").append(fromDate);
			needsAnd = true;
		}
		// If there are multiple IDs in the same time and we have uploaded multiple IDs in the same
		// time stamp then we don't want to double-upload them to the downstream system.  This is
		// important for the S3/Athena persistence image; we explicitly say don't pull those records.
		if (idSet!=null && idSet.size()>0) {
			if (needsAnd) {
				sb.append(" AND");
			}
			sb.append(" id NOT IN (").append(_bulkUploadUtil.getIdsCSV(idSet)).append(")");
		}

		// Very specific use case to fix a defect.  This block of code _can_ probably be removed
		if(_featureFlagService.getBoolean(FeatureFlags.PLTDP_CRUD_DOMAIN_BULKSYNC, false)) {
			//crud audit events
			sb.append(" AND action IN (");//\"create\", \"update\", \"delete\", ");

			//domain audit events
			for (String domainAuditEventActionName: _domainAuditEventsUtil.getDomainEventActions()) {
				sb.append("\"" + domainAuditEventActionName + "\", ");
			}

			sb.replace(0, sb.length() -1, sb.substring(0, sb.length() -2 ));
			sb.append(")");
		}
		return sb.toString();
	}

	private BulkUploadDTO transformToBulkUploadDTO(Object[] o) {
		BulkUploadDTO bulkUploadDTO = new BulkUploadDTO();
		bulkUploadDTO.setId((String) o[0]);

		Optional<BigInteger> created = Optional.ofNullable(((BigInteger) o[1]));
		created.ifPresent(createdString -> {
			Date d = new Date(Long.parseLong(createdString.toString()));
			bulkUploadDTO.setCreated(DateTimeFormatter.ISO_INSTANT.format(d.toInstant()));
		});

		bulkUploadDTO.setAction((String) o[2]);
		bulkUploadDTO.setSource((String) o[3]);
		bulkUploadDTO.setTarget((String) o[4]);
		bulkUploadDTO.setApplication((String) o[5]);
		bulkUploadDTO.setRequestId((String) o[6]);
		bulkUploadDTO.setIpaddr((String) o[7]);
		bulkUploadDTO.setContextId((String) o[8]);
		bulkUploadDTO.setInfo((String) o[9]);

		String xml = (String) o[10];

		Attributes attributes;
		if (null != xml && "".equals(xml.trim())) {
			attributes = new Attributes();
		} else {
			attributes = (Attributes) XMLObjectFactory.getInstance()
					.parseXml(_crudService.getContext(), xml, false);
		}

		bulkUploadDTO.setAttributes(attributes);

		return bulkUploadDTO;
	}

	private void generateMetrics(AuditUploadStatus status, AuditTableNames tableNames) {
		long endTime = System.nanoTime();
		//Converting it into seconds, since seconds as a standard "base unit" in Prometheus. Also, using the ceil value
		//since most requests could end up showing 0 seconds.
		long durationInMilli = (long) Math.ceil((TimeUnit.MILLISECONDS
				.convert(endTime - status.getStartTime(), TimeUnit.NANOSECONDS))/1000);

		MetricsUtil.getHistogram(METRICS_BASE_PATH+ "." + AuditMetricUtil.UPLOAD_TIME_METRIC, ImmutableMap.of()).update(durationInMilli);
		String baseMetric = tableNames.getTableName();
		if (status.isUploadToSearch()) {
			baseMetric+=".syncToSearch";
		} else {
			baseMetric+=".uploadToS3";
		}

		_auditMetricUtil.writeGauge(baseMetric+"."+AuditMetricUtil.REMAINING_COUNT_METRIC, status.getTotalRemaining(), null);
		_auditMetricUtil.writeGauge(baseMetric+"."+AuditMetricUtil.PROCESSED_COUNT_METRIC, status.getTotalProcessed(), null);
		_auditMetricUtil.writeGauge(baseMetric+"."+AuditMetricUtil.UPLOADED_COUNT_METRIC, status.getTotalUploaded(), null);

		_log.info(bulkSyncLogMessage(EVENT_TYPE, _bulkUploadUtil.getStatusString("bulk upload "+(status.isUploadToSearch()?" to search ":"")
				+"session completed", status, durationInMilli)));
	}

	private void generateCountMetrics(int auditRecordCount, int auditArchiveRecordCount, boolean sendToSearch, boolean allRecordsCount) {
		String baseMetric = AuditMetricUtil.AUDIT_RECORD_COUNT;
		if(allRecordsCount){
			baseMetric+=".all";
		}
		if (sendToSearch) {
			baseMetric+=".syncToSearch";
		} else {
			baseMetric+=".uploadToS3";
		}
		_auditMetricUtil.writeGauge(baseMetric, (auditRecordCount+auditArchiveRecordCount), null);
		StringBuilder countStringBuilder = new StringBuilder("count complete: ")
				.append(baseMetric)
				.append(", total count: ")
				.append(auditRecordCount+auditArchiveRecordCount)
				.append(", audit table: ")
				.append(auditRecordCount)
				.append(", archive table: ")
				.append(auditArchiveRecordCount);
		_log.info(bulkSyncLogMessage(EVENT_TYPE, countStringBuilder.toString()));
	}


	/**
	 * Counts the number of Audit Event records that exist in the Org's CIS MySQL database for
	 * a given time range.  The fromDate is inclusive; the upTo date is exclusive (just before that
	 * time. This method is compatible with the existence of both the spt_audit_event and
	 * spt_audit_event_archive tables; when both a present the count returned is the sum of both.
	 *
	 * This method _bypasses_ the Hibernate layer issues direct SQL to a raw JDBC Connection.
	 * The dates are passed as their milliseconds since the unix Epoch directly to the relational
	 * layer, which uses a bigint data type to store the created date.  Really MySQL is simply
	 * keeping an index on an integer value for the date stamps of audit events.
	 *
	 * @param fromDate
	 * @param upToButNotIncludingDate
	 * @return
	 */
	public int getAuditEventCountsByDateRange (Date fromDate, Date upToButNotIncludingDate) {

		if (fromDate.after(upToButNotIncludingDate)) {
			_log.warn("Refusing to query where fromDate [" + fromDate.toString() +
					"] is after upToButNotIncludingDate:[" + upToButNotIncludingDate.toString() + "]");
			return 0;
		}

		// Are we dealing with just the one audit table, or also the _archive table as well?
		boolean includeArchiveTable = archiveTableExists();

		try {
			Connection jdbcCxn = _crudService.getContext().getJdbcConnection();
			try (
					PreparedStatement pStmt = jdbcCxn.prepareStatement( includeArchiveTable ?
							COUNT_AUDIT_EVENTS_BY_TIME_WITH_ARCHIVE_SQL : COUNT_AUDIT_EVENTS_BY_TIME_SQL
					)
			) {
				pStmt.setLong(1, fromDate.getTime());
				pStmt.setLong(2, upToButNotIncludingDate.getTime());
				if (includeArchiveTable) {
					pStmt.setLong(3, fromDate.getTime());
					pStmt.setLong(4, upToButNotIncludingDate.getTime());
				}
				try ( ResultSet resultSet = pStmt.executeQuery() ) {
					while (resultSet.next()) {
						return resultSet.getInt(1);
					}
				}
			}
		} catch (GeneralException e) {
			_log.error("Failure establishing raw JDBC connection", e);
		} catch (SQLException throwables) {
			_log.error("SQL error querying for audit event counts", throwables);
		}

		return 0;
	}

	/**
	 * Returns the IDs of Audit Event records that exist in the Org's CIS MySQL database for
	 * a given time range.  The fromDate is inclusive; the upTo date is exclusive (just before that
	 * time. This method is compatible with the existence of both the spt_audit_event and
	 * spt_audit_event_archive tables; when both a present the count returned is the UNION of both.
	 *
	 * This method _bypasses_ the Hibernate layer issues direct SQL to a raw JDBC Connection.
	 * The dates are passed as their milliseconds since the unix Epoch directly to the relational
	 * layer, which uses a bigint data type to store the created date.  Really MySQL is simply
	 * keeping an index on an integer value for the date stamps of audit events.
	 *
	 * @param fromDate
	 * @param upToButNotIncludingDate
	 * @param idCreatedDateConsumer - Consumer to call for each Audit Event ID, createdDate tuple.
	 * @return
	 */
	public int getAuditEventIdCreatedByDateRange (Date fromDate, Date upToButNotIncludingDate,
												  BiConsumer<String, Date> idCreatedDateConsumer) {

		int recordsProcessed = 0;

		if (fromDate.after(upToButNotIncludingDate)) {
			_log.warn("Refusing to query where fromDate [" + fromDate.toString() +
					"] is after upToButNotIncludingDate:[" + upToButNotIncludingDate.toString() + "]");
			return 0;
		}

		// Are we dealing with just the one audit table, or also the _archive table as well?
		boolean includeArchiveTable = archiveTableExists();

		try {
			Connection jdbcCxn = _crudService.getContext().getJdbcConnection();
			try (
					PreparedStatement pStmt = jdbcCxn.prepareStatement( includeArchiveTable ?
							GET_AUDIT_EVENT_ID_CREATED_BY_TIME_WITH_ARCHIVE_SQL :
							GET_AUDIT_EVENT_ID_CREATED_BY_TIME_SQL
					)
			) {
				pStmt.setLong(1, fromDate.getTime());
				pStmt.setLong(2, upToButNotIncludingDate.getTime());
				if (includeArchiveTable) {
					pStmt.setLong(3, fromDate.getTime());
					pStmt.setLong(4, upToButNotIncludingDate.getTime());
				}
				try ( ResultSet resultSet = pStmt.executeQuery() ) {
					while (resultSet.next()) {
						String auditEventId = resultSet.getString(1);
						Long createdMillis = resultSet.getLong(2);
						idCreatedDateConsumer.accept(auditEventId, new Date(createdMillis));
						recordsProcessed++;
					}
				}
			}
		} catch (GeneralException e) {
			_log.error("Failure establishing raw JDBC connection", e);
		} catch (SQLException throwables) {
			_log.error("SQL error querying for audit event counts", throwables);
		}

		return recordsProcessed;
	}

	/**
	 * Constructs an AuditEvent from a raw query against spt_audit_event or _archive.  This method
	 * is used to (a) bypass hibernate and (b) provide a single code path for both the archive and
	 * non-archive tables for consistent re-hydration of the objects.
	 *
	 * TODO: Write a test to validate that this AuditEvent comes back exactly like a test one.
	 * TODO: Handle the modified column for immutable auditEvent records?
	 *
	 * @param resultSet
	 * @return
	 */
	private AuditEvent transformResultSet(ResultSet resultSet) throws SQLException {

		AuditEvent auditEvent = new AuditEvent();

		// Order of columns from the SQL statement constant GET_AUDIT_EVENT_BY_ID above:
		// id, created, action, source, target, application, tracking_id,
		// string1, string2, string3, string4, attributes FROM %s WHERE id = ?";
		int colIdx = 1;
		auditEvent.setId(resultSet.getString(colIdx++));
		auditEvent.setCreated(new Date(resultSet.getLong(colIdx++)));
		auditEvent.setAction(resultSet.getString(colIdx++));
		auditEvent.setTarget(resultSet.getString(colIdx++));
		auditEvent.setApplication(resultSet.getString(colIdx++));
		auditEvent.setTrackingId(resultSet.getString(colIdx++));
		auditEvent.setString1(resultSet.getString(colIdx++));
		auditEvent.setString2(resultSet.getString(colIdx++));
		auditEvent.setString3(resultSet.getString(colIdx++));
		auditEvent.setString4(resultSet.getString(colIdx++));

		// Parsing the Attributes XML Clob requires some processing.
		String attribXml = resultSet.getString(colIdx++);
		if (!resultSet.wasNull()) {
			Attributes attributes = (Attributes) XMLObjectFactory.getInstance()
					.parseXml(_crudService.getContext(), attribXml, false);
			auditEvent.setAttributes(attributes);
		} else {
			auditEvent.setAttributes(new Attributes());
		}

		return auditEvent;
	}

	/**
	 * Retrieve an AuditEvent from a sqlQuery template.  Used by both the spt_audit_event_archive
	 * and non-archive code paths for consistency and prevented repeated code.
	 * @param auditEventId - the ID of the AuditEvent to retrieve.
	 * @param queryArchiveTable - true for execute query against archive table; false for standard table.
	 * @return - The AuditEvent record found on success, null on no match.
	 */
	private AuditEvent getAuditEventFromSql(String auditEventId, boolean queryArchiveTable) {

		// Construct the query to execute.
		String sqlQuery;
		if (!queryArchiveTable) {
			sqlQuery = String.format(GET_AUDIT_EVENT_BY_ID, AuditTableNames.AUDIT_EVENT);
		} else {
			sqlQuery = String.format(GET_AUDIT_EVENT_BY_ID, AuditTableNames.AUDIT_EVENT_ARCHIVE);
		}

		try {
			Connection jdbcCxn = _crudService.getContext().getJdbcConnection();
			try ( PreparedStatement pStmt = jdbcCxn.prepareStatement( sqlQuery ) ) {
				pStmt.setString(1, auditEventId);
				try ( ResultSet resultSet = pStmt.executeQuery() ) {
					while (resultSet.next()) {
						return transformResultSet(resultSet);
					}
				}
			}
		} catch (GeneralException e) {
			_log.error("Failure establishing raw JDBC connection", e);
		} catch (SQLException throwables) {
			_log.error("SQL error querying for audit event counts", throwables);
		}
		return null; // no match found.
	}

	/**
	 * Retrieves a single AuditEvent record from the CIS database.  Checks first the `spt_audit_event`,
	 * then the `spt_audit_event_archive` table (if present).  Returns a null reference if no AuditEvent
	 * with a matching ID column is found.
	 *
	 * @param auditEventId - The String auditEventId (usually UUID with no dashes) to lookup.
	 * @return - The AuditEvent record on success, null on failure.
	 */
	public AuditEvent getAuditEvent (String auditEventId) {

		AuditEvent auditEvent = getAuditEventFromSql(auditEventId, false);
		if (null != auditEvent) {
			return auditEvent; // Found in main table, return it.
		}

		if (!archiveTableExists()) {
			return null; // Short circuit if no archive table defined.
		}

		// Return results from the _archive table;
		return getAuditEventFromSql(auditEventId, true);
	}

}
