/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.persistence;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.MultiObjectDeleteException;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.hash.Hashing;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.metrics.annotation.Metered;
import com.sailpoint.metrics.annotation.Timed;
import com.sailpoint.utilities.JsonUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.util.Strings;
import sailpoint.object.AuditEvent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of persistence of an Audit Event in S3 Buckets.  Includes utility methods to standardize on
 * path naming and ISO-8601 + UTC date stamping and time range conventions.
 *
 * See the RFC (https://github.com/sailpoint/saas-rfcs/pull/89) for design documentation.
 */
@Singleton
public class S3PersistenceManager {

	public static final Log log = LogFactory.getLog(S3PersistenceManager.class);

	// Read the environment to determine which S3 Bucket to write Audit Events.
	public static final String BUCKET_ENV_VAR = "AER_S3_AUDIT_PERSISTENCE_BUCKET";
	public static final String BUCKET_BY_TIME = ( null == System.getenv(BUCKET_ENV_VAR) ?
					"spt-aer-audit-events-useast1" : // Default value for dev environments, was aer-audit-events-by-time
					System.getenv(BUCKET_ENV_VAR)    // use environment specified value for regional locality.
	);

	public static final String SHA256_META_KEY = "slpt-sha256";
	public static final String SLPT_ORG_META_KEY = "slpt-org";
	public static final String SLPT_POD_META_KEY = "slpt-pod";
	public static final ZoneId UTC_ZONE = ZoneId.of("Etc/UTC");

	public static final int DELETE_OBJS_PAGE_SIZE = 256;

	public static final int OBJECT_KEY_LENGTH = 7;
	public static final int OBJECT_KEY_TENANT_ID_INDEX = 0;
	public static final int OBJECT_KEY_AUDIT_EVENT_INDEX = 6;

	// s3Clients are thread safe and manage their own pooling internally. Not 'final' to support test side-loading.
	// See the S3ClientProvider class for how we build the ClientConfiguraiton and connection pool tuning.
	@Inject
	protected static AmazonS3 s3Client;

	public S3PersistenceManager() {
		log.info("Using S3 persistence bucket: " + BUCKET_BY_TIME);
	}

	/**
	 * Returns the string expansion for the S3 object key path for the given date.
	 * @param date
	 * @return - a string formatted like "yyyy/MM/dd/HH/mm" of SimpleDateFormatter.
	 */
	public static String getKeyPathDateParts(Date date) {

		requireNonNull(date, "a date reference is required");

		Instant instant = date.toInstant();
		ZonedDateTime zdt = instant.atZone(UTC_ZONE);
		return String.format(
				"%04d/%02d/%02d/%02d/%02d",
				zdt.getYear(),
				zdt.getMonth().getValue(),
				zdt.getDayOfMonth(),
				zdt.getHour(),
				zdt.getMinute()
		);
	}

	/**
	 * Returns the S3 Object Key path for the given auditEvent.
	 * Format is: ${tenantId}/${year}/${month}/${day}/${hour}/${minute}/${eventId}
	 * Example: 53ff8153-7456-43ff-b9b4-e5deaeac5f09/2021/08/19/12/53/f54b489527e04318be593594d57ae4e1
	 * @param auditEvent
	 * @return
	 */
	public static String getObjectKey(AuditEvent auditEvent) {
		RequestContext requestContext = RequestContext.ensureGet();
		Optional<String> optTenantId = requestContext.getTenantId();
		if (!optTenantId.isPresent()) {
			log.error("No `tenantId` found in RequestContext");
			throw new RuntimeException("No `tenantId` found in RequestContext, unable to derive key for AuditEvent");
		}
		return getObjectKey(optTenantId.get(), auditEvent.getCreated(), auditEvent.getId());
	}

	/**
	 * Returns the S3 Object Key path for the given tenant, auditEvent.
	 * Format is: ${tenantId}/${year}/${month}/${day}/${hour}/${minute}/${eventId}
	 * Example: 53ff8153-7456-43ff-b9b4-e5deaeac5f09/2021/08/19/12/53/f54b489527e04318be593594d57ae4e1
	 * @param tenantId
	 * @param auditEvent
	 * @return
	 */
	public static String getObjectKey(String tenantId, AuditEvent auditEvent) {
		requireNonNull(tenantId, "a valid tenantId UUID is required");
		requireNonNull(auditEvent, "a valid auditEvent reference is required");
		return getObjectKey(tenantId, auditEvent.getCreated(), auditEvent.getId());
	}

	/**
	 * Returns the S3 Object Key path for the given tenant, createdDate and auditEventId.
	 * Format is: ${tenantId}/${year}/${month}/${day}/${hour}/${minute}/${eventId}
	 * Example: 53ff8153-7456-43ff-b9b4-e5deaeac5f09/2021/08/19/12/53/f54b489527e04318be593594d57ae4e1
	 * @param tenantId
	 * @param createdDate
	 * @param auditEventId
	 * @return
	 */
	public static String getObjectKey(String tenantId, Date createdDate, String auditEventId) {

		requireNonNull(tenantId, "a valid tenantId UUID is required");
		requireNonNull(createdDate, "a createdDate reference is required");
		requireNonNull(auditEventId, "a valid auditEvent UUID is required");

		Instant instant = createdDate.toInstant();
		ZonedDateTime zdt = instant.atZone(UTC_ZONE);

		return String.format(
				"%s/%s/%s",
				tenantId,
				getKeyPathDateParts(createdDate),
				auditEventId
		);
	}

	public static String getSha256Hash(AuditEvent auditEvent) {
		requireNonNull(auditEvent, "a valid auditEvent reference is required");
		return getSha256Hash(JsonUtil.toJson(auditEvent));
	}

	public static String getSha256Hash(String auditEventJson) {
		requireNonNull(auditEventJson, "a valid auditEventJson String reference is required");
		return Hashing.sha256().hashString(auditEventJson, StandardCharsets.UTF_8).toString();
	}

	/**
	 * Attempts to retrieve an AuditEvent from S3 CLOB storage.   If the AuditEVent is found then an
	 * envelope wrapping the AuditEvent with extra meta-data is returned.  If not found then a null
	 * is returned from this method.
	 *
	 * TODO: Evaluate whether we need to throw any other exceptions from this service.
	 *
	 * @param tenantId
	 * @param createdDate
	 * @param auditEventId
	 * @return
	 */
	@Timed
	@Metered
	public static S3AuditEventEnvelope getAuditEvent(String tenantId, Date createdDate, String auditEventId) {

		requireNonNull(tenantId, "a valid tenantId UUID is required");
		requireNonNull(createdDate, "a createdDate reference is required");
		requireNonNull(auditEventId, "a valid auditEvent UUID is required");

		String keyPath = getObjectKey(tenantId, createdDate, auditEventId);

		return getAuditEvent(keyPath);
	}

	/**
	 * Attempts to retrieve an AuditEvent from S3 CLOB storage.   If the AuditEVent is found then an
	 * envelope wrapping the AuditEvent with extra meta-data is returned.  If not found then a null
	 * is returned from this method.
	 *
	 * TODO: Evaluate whether we need to throw any other exceptions from this service.
	 *
	 * @param keyPath
	 * @return
	 */
	@Timed
	@Metered
	public static S3AuditEventEnvelope getAuditEvent(String keyPath) {

		try {
			String[] keyPathArray = keyPath.split("/");
			String tenantId;
			String auditEventId;
			if (keyPathArray.length==OBJECT_KEY_LENGTH) {
				tenantId = keyPathArray[OBJECT_KEY_TENANT_ID_INDEX];
				auditEventId = keyPathArray[OBJECT_KEY_AUDIT_EVENT_INDEX];
			} else {
				throw new IllegalArgumentException("keyPath string was invalid");
			}

			log.debug("Retrieving " + BUCKET_BY_TIME + ":"+ keyPath);

			// Bail out early if the object we're looking for does not exist.
			if ( !s3Client.doesObjectExist(BUCKET_BY_TIME, keyPath) ) {
				return null;
			}

			// Ensure that in all circumstances the input stream is closed.
			try (
					S3Object s3Obj = s3Client.getObject(BUCKET_BY_TIME, keyPath);
					S3ObjectInputStream s3is = s3Obj.getObjectContent()
			) {
				String auditEventJson = IOUtils.toString(s3is, StandardCharsets.UTF_8.name());

				ObjectMetadata objMeta = s3Obj.getObjectMetadata();
				Map<String,String> userMeta = objMeta.getUserMetadata();
				String persistedSha256 = userMeta.get(SHA256_META_KEY);
				if (Strings.isBlank(persistedSha256)) {
					// This is an integrity concern. TODO: Peg an integrity concern metric?
					log.warn("No " + persistedSha256 + " hash found on record; unable to validate.");
					// Re-calculate our own SHA256 for use.
					persistedSha256 = getSha256Hash(auditEventJson);
				}

				S3AuditEventEnvelope envelope = new S3AuditEventEnvelope();
				envelope.setTenantId(tenantId);
				envelope.setS3ObjectKey(keyPath);
				envelope.setAuditEventId(auditEventId);
				envelope.setMd5Checksum(objMeta.getContentMD5());
				envelope.setSha256Hash(persistedSha256);
				envelope.setAuditEventJson(auditEventJson);
				envelope.setAuditEvent(JsonUtil.parse(AuditEvent.class, auditEventJson));

				return envelope;
			}

		} catch (com.amazonaws.services.s3.model.AmazonS3Exception s3ex) {
			if (s3ex.getMessage().contains("The specified key does not exist.")) {
				// The record simply does not exist, it might have been deleted in between
				// the earlier call and now.  Warn and return null to the caller.
				log.warn("Object " + keyPath + " in bucket: " + BUCKET_BY_TIME + " has evaporated!");
				return null;
			}
			log.error("AmazonS3Exception while looking up keyPath: " + keyPath, s3ex);
		} catch (AmazonServiceException e) {
			log.error("AmazonServiceException while looking up keyPath: " + keyPath, e);
		} catch (IOException e) {
			log.error("IOException while reading AuditEvent at keyPath: " + keyPath, e);
		}

		return null;
	}

	/**
	 * Persists an AuditEvent to durable storage.  Checks the RequestContext for the tenantId to apply.
	 * If the AuditEvent already exists and the checksums match then we leave the exising (immutable) record
	 * in place and do not write.  If the record already exists and the checksums differ then a WARN
	 * and metric are pegged and write is issued to S3.  S3's bucket will track the version information.
	 *
	 * @param auditEvent
	 * @return S3AuditEventEnvelope with properties for the saved audit event.
	 */
	public static S3AuditEventEnvelope saveAuditEvent(AuditEvent auditEvent) {
		return saveAuditEvent(null, auditEvent);
	}

	/**
	 * Persists an AuditEvent to durable storage.  Checks the RequestContext if tenantId is passed as null.
	 * If the AuditEvent already exists and the checksums match then we leave the exising (immutable) record
	 * in place and do not issue write.  If the record already exists and the checksums differ then a WARN
	 * and metric are pegged and write is issued to S3.  S3's bucket will track the version information.
	 *
	 * @param tenantId
	 * @param auditEvent
	 * @return S3AuditEventEnvelope with properties for the saved audit event.
	 */
	@Timed
	@Metered
	public static S3AuditEventEnvelope saveAuditEvent(String tenantId, AuditEvent auditEvent) {

		RequestContext requestContext = RequestContext.ensureGet();

		if (Strings.isEmpty(tenantId)) {
			Optional<String> optTenantId = requestContext.getTenantId();
			if (!optTenantId.isPresent()) {
				log.error("No `tenantId` found in RequestContext");
				throw new RuntimeException("No `tenantId` found in RequestContext, unable to persist AuditEvent");
			}
			tenantId = optTenantId.get();
		}

		String keyPath = getObjectKey(tenantId, auditEvent);
		String sha256 = getSha256Hash(auditEvent);

		// Check to see if the Audit Event exists first.
		S3AuditEventEnvelope prevEnvelope = getAuditEvent(tenantId, auditEvent.getCreated(), auditEvent.getId());
		boolean prevChecksumMismatch = false;
		if (null != prevEnvelope) {
			if (sha256.equals(prevEnvelope.getSha256Hash())) {
				// They match, no need to save.
				log.debug("Redundant save: " + keyPath);
				// Increment a "record already exists" flag and move on.
				prevEnvelope.setAlreadyExistedInS3(true);
				return prevEnvelope;
			}
			// They don't match, log a warning and set the flag to return to the caller.
			log.warn("Checksum mismatch for " + keyPath +
					" old sha256:" + prevEnvelope.getSha256Hash() +
					" new sha256:" + sha256
			);
			prevChecksumMismatch = true;
			// TODO: Check the version of the previous item, it better be v1!  If not, warn and peg a metric.
			// Steam roll ahead with saving the new copy. It will have a new version, and that's okay.
		}

		String jsonStr = JsonUtil.toJson(auditEvent);
		InputStream jsonAsInputStream = new ByteArrayInputStream(jsonStr.getBytes(StandardCharsets.UTF_8));

		HashMap<String,String> userMetaData = new HashMap<>();
		userMetaData.put(SHA256_META_KEY, sha256);
		userMetaData.put(SLPT_ORG_META_KEY, requestContext.getOrg());
		userMetaData.put(SLPT_POD_META_KEY, requestContext.getPod());

		ObjectMetadata objMeta = new ObjectMetadata();
		objMeta.setUserMetadata(userMetaData);
		objMeta.setContentType("application/json");

		// Required to prevent `No content length specified for stream data.` warnings.
		objMeta.setContentLength(jsonStr.getBytes(StandardCharsets.UTF_8).length);

		PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET_BY_TIME, keyPath, jsonAsInputStream, objMeta);

		PutObjectResult putObjectResult = s3Client.putObject(putObjectRequest);

		putObjectResult.getContentMd5();

		S3AuditEventEnvelope envelope = new S3AuditEventEnvelope();
		envelope.setAuditEvent(auditEvent);
		envelope.setAuditEventJson(jsonStr);
		envelope.setAuditEventId(auditEvent.getId());
		envelope.setS3ObjectKey(keyPath);
		envelope.setSha256Hash(sha256);
		envelope.setPreviousDiffered(prevChecksumMismatch);

		return envelope;

	}

	/**
	 * Deletes an AuditEvent from the S3 durable storage.  This is a _destructive_ operation that
	 * is used for testing and in the future possibly for purging tenant data in slow-trickle fashion.
	 * @param tenantId
	 * @param cratedDate
	 * @param eventId
	 */
	public static void deleteAuditEvent (String tenantId, Date cratedDate,  String eventId) {
		String pathKey = getObjectKey(tenantId, cratedDate, eventId);
		if (s3Client.doesObjectExist(BUCKET_BY_TIME, pathKey)) {
			try {
				DeleteObjectsRequest dor = new DeleteObjectsRequest(BUCKET_BY_TIME).withKeys(pathKey);
				s3Client.deleteObjects(dor);
			} catch (AmazonServiceException e) {
				log.error("Failure Deleting " + pathKey + " in bucket: " + BUCKET_BY_TIME, e);
				throw e;
			}
		}
	}

	/**
	 * Internal helper function to delete a page of records.  This deletes a page or records and tracks the amount
	 * of time spent deleting that page of records.
	 * @param searchedPrefix - the path prefix used to find the records.
	 * @param keysToDelete - the List of specific object keys to delete.
	 */
	private static void deleteRecordPage (String searchedPrefix, List<String> keysToDelete) {

		String [] strArr = new String [keysToDelete.size()];
		strArr = keysToDelete.toArray(strArr);
		int requestedCount = strArr.length;

		DeleteObjectsRequest doReq = new DeleteObjectsRequest(BUCKET_BY_TIME).withKeys(strArr);

		long startTime = System.currentTimeMillis();
		int deletedCount = 0;
		try {
			DeleteObjectsResult doRes = s3Client.deleteObjects(doReq);
			deletedCount = doRes.getDeletedObjects().size();
		} catch (MultiObjectDeleteException modEx) {
			deletedCount = modEx.getDeletedObjects().size();
			log.warn("Some objects were not deleted, requestedCount: " + requestedCount +
					" deletedCount:" + deletedCount, modEx);
			throw modEx;
		}
		long duration = System.currentTimeMillis() - startTime;

		log.info("Deleted " + deletedCount + " objects from prefix: " + searchedPrefix + " in msecs:" + duration);

	}

	/**
	 * Deletes all the AuditEvent records from the S3 durable storage for the given tenant.  This is a
	 * _destructive_ operation that purges all the records from storage in a non-recoverable fashion.
	 * Used during org deletes.  This can be a lengthy process that takes a very long time to execute.
	 * This operation can block a Kafka/Iris worker thread for a long period of time.  We delete records
	 * in page sizes so that no one call takes too long, and we track the total time delete needed
	 * to take place.
	 * @param tenantId
	 */
	public static void deleteAuditEventsForTenant (String tenantId) {

		// Sanity check to ensure we were given something resembling a valid UUID for tenantId.
		if (36 > tenantId.length()) {
			log.error("Refusing to delete all objects matching non-UUID tenantId:" + tenantId);
			return;
		}

		String keyPrefix = tenantId + "/";

		ArrayList<String> pageOfPathsToDelete = new ArrayList<>();

		long totalCount = 0;

		long startTime = System.currentTimeMillis();

		for ( S3ObjectSummary summary : S3Objects.withPrefix(s3Client, BUCKET_BY_TIME, keyPrefix) ) {
			pageOfPathsToDelete.add(summary.getKey());
			if ((pageOfPathsToDelete.size() % DELETE_OBJS_PAGE_SIZE) == 0) {
				deleteRecordPage(keyPrefix, pageOfPathsToDelete);
				totalCount += pageOfPathsToDelete.size();
				pageOfPathsToDelete.clear();
			}
		}
		if (pageOfPathsToDelete.size() != 0) {
			deleteRecordPage(keyPrefix, pageOfPathsToDelete);
			totalCount += pageOfPathsToDelete.size();
			pageOfPathsToDelete.clear();
		}

		long duration = System.currentTimeMillis() - startTime;
		log.info("Deleted total of " + totalCount + " objects from prefix: " + keyPrefix + " in msecs:" + duration);

	}

	/**
	 * Iterate over all the S3 object keys from the Audit Event bucket that match the given prefix.  For each
	 * record invoke the specified consumer with the S3 object's key for the consumer to do with it what it
	 * chooses.  Consumers might do things like:
	 *
	 *  - Count the number of objects that match the prefix pattern.
	 *  - Get the meta-data to retrieve check sums and created date information.
	 *  - Retrieve the full record for re-play to down-stream consumers.
	 *
	 * @param keyPrefix
	 * @param recordKeyConsumer
	 */
	public static void iterateObjectKeys(String keyPrefix, Consumer<String> recordKeyConsumer) {
		for ( S3ObjectSummary summary : S3Objects.withPrefix(s3Client, BUCKET_BY_TIME, keyPrefix) ) {
			// Simply invoke the consumer once for each record.
			recordKeyConsumer.accept(summary.getKey());
		}
	}

	/**
	 * Search S3 storage for Audit Events given a tenantId and date range.  For keys
	 * that matches the search, call the provided Consumer with the key. This implements
	 * a fuzzy match based on the date ranges given.  The S3 bucket has to-the-minute
	 * precision pathing for the minute the Audit Event was created, which means there are
	 * potentially 1,440 different path prefixes for a given org for a given day.  Searching
	 * thousands of path prefixes in S3 produces a lot of search calls, many of which will
	 * return no data to the caller. As the primary use case of this function is to support
	 * bulk re-play of Audit Events over a time range, we allow this iterator to dither the
	 * results to align with making fewer S3 calls to search prefixes.  This means if the
	 * search across years is issued with to-the-minute precision, then some records before
	 * and after the given dates will be returned.
	 *
	 * It is up to the caller to retrieve the record form S3 and provide more precise date
	 * filtering to remove records exactly outside the time ragne..  Caveat Emptor: you have been warned!
	 *
	 * Cases:
	 * 	 The dates have different years or months.  We iterate each intervening day as prefix.
	 * 	 The dates have the same yyyy/mm/dd, but have different hours.  Iterate by intervening hours as prefix.
	 *
	 * We currently do not support iterating over minutes. This means this method will return at
	 * least 1 hour's worth of data for the given tenant.
	 *
	 * Typical examples, with some illustration of counter-intuitive behavior:
	 *
	 * 	 fromDate= 2019/08/17/00/00   toDate= 2020/08/01/23/59  ==> iterate by days.
	 * 	 fromDate= 2019/12/29/00/00   toDate= 2020/01/02/23/59  ==> iterate by days, even though 5 days apart.
	 * 	 fromDate= 2019/01/01/00/00   toDate= 2020/01/01/23/59  ==> iterate by days, even though 366 days apart.
	 * 	 fromDate= 2020/09/01/00/00   toDate= 2020/09/30/23/59  ==> iterate by days.
	 * 	 fromDate= 2020/12/01/00/00   toDate= 2021/12/01/16/00  ==> iterate by hours.
	 * 	 fromDate= 2020/12/01/17/03   toDate= 2021/12/01/18/30  ==> iterate by hours; minute ranges ignored!
	 *
	 * @param tenantId
	 * @param fromDate
	 * @param toDate
	 * @param keyConsumer
	 */
	public static void iterateByDateRange(String tenantId, Date fromDate, Date toDate, Consumer<String> keyConsumer) {

		// Sanity check to ensure fromDate is earlier than toDate. No need to except out, but we
		// should issue a warning if we encounter this circumstance.
		if (fromDate.getTime() > toDate.getTime()) {
			log.warn("Returning no records when fromDate:[" + fromDate.getTime() +
					"] after toDate:[" + toDate.getTime() + "]");
			return;
		}

		ZonedDateTime fromZdt = fromDate.toInstant().atZone(UTC_ZONE);
		ZonedDateTime toZdt = toDate.toInstant().atZone(UTC_ZONE);

		// Default to searching a span of time with different days. Iterate over hours.
		ChronoUnit stepUnit = ChronoUnit.HOURS;
		if ((fromZdt.getYear() != toZdt.getYear()) || (fromZdt.getMonth() != toZdt.getMonth())) {
			// We are searching a span of time with different years or months.  Iterate over days.
			stepUnit = ChronoUnit.DAYS;
		}

		// Invoke the helper method to perform the iteration of the S3 key prefixes.
		iterateByTimeStep(tenantId,fromZdt, toZdt, stepUnit, keyConsumer);

	}

	/**
	 * Internal helper method to build key prefixes and issue searches to S3. This provides a single method
	 * to build keys and issue the search, stepping the time up by day or by hour of repeated key space searching.
	 *
	 * For example, searches issue key prefix queries for:
	 *
	 *   by day:  tenantId/2020/01/01,    tenantId/2020/01/02,    tenantId/2020/01/03    ... tenantId/2020/02/28
	 *  by hour:  tenantId/2020/01/01/00, tenantId/2020/01/01/01, tenantId/2020/01/01/02 ... tenantId/2020/01/01/23
	 *
	 * @param tenantId - the tenantId to search records against.
	 * @param fromZdt - the starting search time stamp, zoned to UTC.
	 * @param toZdt - the up-to search time stamp, zoned to UTC.
	 * @param stepUnit - The chrono unit to increment by; either DAYS or HOURS.
	 * @param keyConsumer - the Consumer<String> that receives the S3 object keys.
	 */
	private static void iterateByTimeStep(String tenantId, ZonedDateTime fromZdt, ZonedDateTime toZdt,
										  ChronoUnit stepUnit, Consumer<String> keyConsumer) {

		Instant searchInstant;
		Instant upToInstant;

		if (stepUnit == ChronoUnit.DAYS) {

			// Start at the from timestamp truncated to the nearest day, then advance day by day.
			searchInstant = LocalDateTime.of(
					fromZdt.getYear(), fromZdt.getMonth(), fromZdt.getDayOfMonth(),
					0,0,0,0
			).atZone(UTC_ZONE).toInstant();

			upToInstant = LocalDateTime.of(
					toZdt.getYear(), toZdt.getMonth(), toZdt.getDayOfMonth(),
					0, 0, 0, 0
			).atZone(UTC_ZONE).toInstant();

		} else {

			// Start with a time stamp truncated to then nearest hour, then advance by hour.
			searchInstant = LocalDateTime.of(
					fromZdt.getYear(), fromZdt.getMonth(), fromZdt.getDayOfMonth(), fromZdt.getHour(),
					0,0,0
			).atZone(UTC_ZONE).toInstant();

			upToInstant = LocalDateTime.of(
					toZdt.getYear(), toZdt.getMonth(), toZdt.getDayOfMonth(), toZdt.getHour(),
					0, 0, 0
			).atZone(UTC_ZONE).toInstant();

		}

		log.info("Searching S3 key space from " + searchInstant.toString() + " through " + upToInstant.toString());

		// Issue repeated searches to S3, supplying a new key prefix for each search, incrementing from old-est
		// time step to new-est time stamp.
		do {

			ZonedDateTime searchZdt = searchInstant.atZone(UTC_ZONE);

			String keyPrefix;
			if (stepUnit == ChronoUnit.DAYS) {
				// Build a key prefix specific to the day.
				keyPrefix = String.format(
						"%s/%04d/%02d/%02d/",
						tenantId,
						searchZdt.getYear(),
						searchZdt.getMonth().getValue(),
						searchZdt.getDayOfMonth()
				);
			} else {
				// Build a key prefix specific to the hour.
				keyPrefix = String.format(
						"%s/%04d/%02d/%02d/%02d/",
						tenantId,
						searchZdt.getYear(),
						searchZdt.getMonth().getValue(),
						searchZdt.getDayOfMonth(),
						searchZdt.getHour()
				);
			}

			log.debug("Searching S3 Key Prefix: " + keyPrefix);

			iterateObjectKeys(keyPrefix, keyConsumer);

			searchInstant = searchInstant.plus(1, stepUnit);

		} while (searchInstant.toEpochMilli() <= upToInstant.toEpochMilli());

	}

	/**
	 * Used for mocking S3 persistence and injecting and s3Client reference.
	 * @param newS3Client
	 */
	public static void overrideS3Client(AmazonS3 newS3Client) {
		s3Client = newS3Client;
		log.info("Using S3 persistence bucket: " + BUCKET_BY_TIME);
	}

}
