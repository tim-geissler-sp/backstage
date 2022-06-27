/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.util.Strings;
import sailpoint.api.SailPointContext;
import sailpoint.object.Attributes;
import sailpoint.object.AuditEvent;
import sailpoint.tools.GeneralException;
import sailpoint.tools.xml.XMLObjectFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.function.BiConsumer;

import static com.sailpoint.audit.persistence.S3PersistenceManager.UTC_ZONE;

/**
 * Compartmentalized methods to query CIS databases directly for reading single, individual AuditEvent records.  This
 * is outside of Guice injection coverage (for now, at least); it may get rolled in later. Being outside Guice makes
 * use by worker threads cleaner.
 *
 * These could have been implemented in @see BulkUploadAuditEventsService, but has instead been implemented
 * here as a stand-alone class.  The baggage of constructing the bulk upload service was too great for direct
 * (non-hibernate) queries to the audit tables.
 */
public class SyncCisToS3Queries {

	public static final Log log = LogFactory.getLog(SyncCisToS3Queries.class);

	// Columns that exist in the relational model that we have ignored in CIS/IdentityNow use?
	// modified bigint,
	// owner varchar(128),
	// assigned_scope varchar(128),
	// assigned_scope_path varchar(450),
	// interface varchar(128),
	// account_name varchar(256),
	// instance varchar(128),
	// attribute_name varchar(128),
	// attribute_value varchar(450),
	private final static String GET_AUDIT_EVENT_BY_ID =
			"SELECT id, created, action, source, target, application, tracking_id, " +
			"       string1, string2, string3, string4, attributes " +
			"  FROM %s WHERE id = ?";

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

	private final static String GET_EARLIEST_AUDIT_EVENT_CREATED_TIMESTAMP_TEMPLATE =
			"SELECT MIN(created) FROM %s ;";

	public static boolean isMySqlDb = true;  // True for MySql, false for H2 in integration testing.

	SailPointContext context;

	// Statements need to be prepared differently for MySQL vs. H2.  Only set to H2/False during testing.
	public static void setIsMySqlDb(boolean isMySql) {
		isMySqlDb = isMySql;
	}


	public SyncCisToS3Queries(SailPointContext context) {
		this.context = context;
	}

	public boolean archiveTableExists() {
		try {
			context.search("sql:select id from spt_audit_event_archive limit 1", null, null);
			return true;
		} catch (GeneralException e) {
			log.trace("audit archive table does not exist");
		}
		return false;
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
		auditEvent.setSource(resultSet.getString(colIdx++));
		auditEvent.setTarget(resultSet.getString(colIdx++));
		auditEvent.setApplication(resultSet.getString(colIdx++));
		auditEvent.setTrackingId(resultSet.getString(colIdx++));
		auditEvent.setString1(resultSet.getString(colIdx++));
		auditEvent.setString2(resultSet.getString(colIdx++));
		auditEvent.setString3(resultSet.getString(colIdx++));
		auditEvent.setString4(resultSet.getString(colIdx++));

		// Parsing the Attributes XML Clob requires some processing.
		String attribXml = resultSet.getString(colIdx++);
		try {
			if (!resultSet.wasNull() && ( !Strings.isBlank(attribXml)) ) {
				Attributes attributes = (Attributes) XMLObjectFactory.getInstance().parseXml(
						context, attribXml, false
				);
				auditEvent.setAttributes(attributes);
			} else {
				auditEvent.setAttributes(new Attributes());
			}
		} catch (Throwable t) {
			log.error("Failure parsing Attributes XML CLOB: [" + attribXml + "]", t);
			throw t;
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
			sqlQuery = String.format(GET_AUDIT_EVENT_BY_ID,
					BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT.getTableName());
		} else {
			sqlQuery = String.format(GET_AUDIT_EVENT_BY_ID,
					BulkUploadAuditEventsService.AuditTableNames.AUDIT_EVENT_ARCHIVE.getTableName());
		}

		try {
			Connection jdbcCxn = context.getJdbcConnection();
			try (
					PreparedStatement pStmt = jdbcCxn.prepareStatement(
							sqlQuery, java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY
					)
			) {
				if (isMySqlDb) pStmt.setFetchSize(Integer.MIN_VALUE);
				pStmt.setString(1, auditEventId);
				try ( ResultSet resultSet = pStmt.executeQuery() ) {
					// Only return the first (and should be only!) match.
					if (resultSet.next()) {
						return transformResultSet(resultSet);
					}
				}
			}
		} catch (GeneralException e) {
			log.error("Failure establishing raw JDBC connection", e);
		} catch (SQLException throwables) {
			log.error("SQL error querying for audit event counts", throwables);
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
	public long getAuditEventCountsByDateRange (Date fromDate, Date upToButNotIncludingDate) {

		if (fromDate.after(upToButNotIncludingDate)) {
			log.warn("Refusing to query where fromDate [" + fromDate.toString() +
					"] is after upToButNotIncludingDate:[" + upToButNotIncludingDate.toString() + "]");
			return 0;
		}

		// Are we dealing with just the one audit table, or also the _archive table as well?
		boolean includeArchiveTable = archiveTableExists();

		try {
			Connection jdbcCxn = context.getJdbcConnection();
			try (
					PreparedStatement pStmt = jdbcCxn.prepareStatement( includeArchiveTable ?
							COUNT_AUDIT_EVENTS_BY_TIME_WITH_ARCHIVE_SQL : COUNT_AUDIT_EVENTS_BY_TIME_SQL,
							java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY
					)
			) {
				if (isMySqlDb) pStmt.setFetchSize(Integer.MIN_VALUE);
				pStmt.setLong(1, fromDate.getTime());
				pStmt.setLong(2, upToButNotIncludingDate.getTime());
				if (includeArchiveTable) {
					pStmt.setLong(3, fromDate.getTime());
					pStmt.setLong(4, upToButNotIncludingDate.getTime());
				}
				try ( ResultSet resultSet = pStmt.executeQuery() ) {
					while (resultSet.next()) {
						return resultSet.getLong(1);
					}
				}
			}
		} catch (GeneralException e) {
			log.error("Failure establishing raw JDBC connection", e);
		} catch (SQLException throwables) {
			log.error("SQL error querying for audit event counts", throwables);
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
	public long getAuditEventIdCreatedByDateRange (Date fromDate, Date upToButNotIncludingDate,
												   BiConsumer<String, Date> idCreatedDateConsumer) {

		long recordsProcessed = 0;

		if (fromDate.after(upToButNotIncludingDate)) {
			log.warn("Refusing to query where fromDate [" + fromDate.toString() +
					"] is after upToButNotIncludingDate:[" + upToButNotIncludingDate.toString() + "]");
			return 0;
		}

		// Are we dealing with just the one audit table, or also the _archive table as well?
		boolean includeArchiveTable = archiveTableExists();

		try {
			Connection jdbcCxn = context.getJdbcConnection();
			try (
					PreparedStatement pStmt = jdbcCxn.prepareStatement( includeArchiveTable ?
							GET_AUDIT_EVENT_ID_CREATED_BY_TIME_WITH_ARCHIVE_SQL :
							GET_AUDIT_EVENT_ID_CREATED_BY_TIME_SQL,
							java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY
					)
			) {
				if (isMySqlDb) pStmt.setFetchSize(Integer.MIN_VALUE);
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
			log.error("Failure establishing raw JDBC connection", e);
		} catch (SQLException throwables) {
			log.error("SQL error querying for audit event counts", throwables);
		}

		return recordsProcessed;
	}

	/**
	 * Retrieves the earliest Created time stamp day from the Org's spt_audit_event or spt_audit_event_archive table.
	 * This gives us an org-specific first time stamp from which we can start a bulk-sync operation for AuditEvent
	 * records.
	 *
	 * @return - a Date that is the earliest date we should use for syncing.
	 */
	public Instant getEarliestAuditEventCreatedDate() {

		// Pick a reasonable default date that captures all IdentityNow tenants since the dawn of the service.
		Instant earliestDate = LocalDateTime.of(
				2014, 1, 1, 0,0,0,0
		).atZone(UTC_ZONE).toInstant();

		// Find the earliest spt_audit_event value, then spt_audit_event_archive value if that table exists.
		String sqlQuery = String.format(GET_EARLIEST_AUDIT_EVENT_CREATED_TIMESTAMP_TEMPLATE, "spt_audit_event");
		try {
			Connection jdbcCxn = context.getJdbcConnection();
			try ( PreparedStatement pStmt = jdbcCxn.prepareStatement( sqlQuery ) ) {
				try ( ResultSet resultSet = pStmt.executeQuery() ) {
					while (resultSet.next()) {
						Long earliestCreatedMillis = resultSet.getLong(1);
						earliestDate = Instant.ofEpochMilli(earliestCreatedMillis);
					}
				}
			}
		} catch (GeneralException e) {
			log.error("Failure establishing raw JDBC connection", e);
		} catch (SQLException throwables) {
			log.error("SQL error querying for earliest audit event", throwables);
		}

		// Short-circuilt early if there is no audit event archive table.
		if (!archiveTableExists()) return earliestDate.truncatedTo(ChronoUnit.DAYS);

		sqlQuery = String.format(GET_EARLIEST_AUDIT_EVENT_CREATED_TIMESTAMP_TEMPLATE, "spt_audit_event_archive");
		try {
			Connection jdbcCxn = context.getJdbcConnection();
			try ( PreparedStatement pStmt = jdbcCxn.prepareStatement( sqlQuery ) ) {
				try ( ResultSet resultSet = pStmt.executeQuery() ) {
					while (resultSet.next()) {
						Long earliestCreatedMillis = resultSet.getLong(1);
						if (earliestCreatedMillis < earliestDate.toEpochMilli()) {
							earliestDate = Instant.ofEpochMilli(earliestCreatedMillis);
						}
					}
				}
			}
		} catch (GeneralException e) {
			log.error("Failure establishing raw JDBC connection", e);
		} catch (SQLException throwables) {
			log.error("SQL error querying for earliest audit event archive", throwables);
		}

		return earliestDate.truncatedTo(ChronoUnit.DAYS);

	}

}
