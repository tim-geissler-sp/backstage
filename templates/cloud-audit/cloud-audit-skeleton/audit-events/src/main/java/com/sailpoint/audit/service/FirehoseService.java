/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClientBuilder;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamResult;
import com.amazonaws.services.kinesisfirehose.model.InvalidArgumentException;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResult;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordResult;
import com.amazonaws.services.kinesisfirehose.model.Record;
import com.amazonaws.services.kinesisfirehose.model.ResourceNotFoundException;
import com.amazonaws.services.kinesisfirehose.model.ServiceUnavailableException;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.atlas.messaging.util.ExponentialBackoff;
import com.sailpoint.atlas.search.model.event.Event;
import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.audit.service.model.FirehoseTrackingRecord;
import com.sailpoint.metrics.MetricsUtil;
import com.sailpoint.metrics.annotation.Metered;
import com.sailpoint.utilities.JsonUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class FirehoseService {

	static Log _log = LogFactory.getLog(FirehoseService.class);

	private final static String FIREHOSE_NOT_EXISTS = "com.sailpoint.aer.firehose.notexists";

	private final static String FIREHOSE_THRESHOLD_REACHED = "com.sailpoint.aer.firehose.threshold.reached";

	private final static String AUDIT_EVENT_PARQUET_DEADLETTER = "audit.event.parquet.deadletter.total";

	private static final int MAX_ATTEMPTS = 3;

	AmazonKinesisFirehose _firehoseClient = AmazonKinesisFirehoseClientBuilder.defaultClient();

	@Inject
	FirehoseNotFoundCacheService _firehoseNotFoundCache;

	@Inject
	FirehoseCacheService _firehoseCacheService;

	@VisibleForTesting
	Map<Integer, Backoff> _backoffFirehoseMap = new ConcurrentHashMap<>();

	@VisibleForTesting
	FirehoseService.RetryCount _retryCount = new FirehoseService.RetryCount();

	FirehoseTrackingRecord _firehoseTrackingRecord;

	enum FirehoseStatus {
		ACTIVE,
		CREATING,
		CREATING_FAILED
	}

	/**
	 * Send audit event to firehose
	 * @param event
	 */
	@Metered
	public PutRecordResult sendToFirehose(Event event) {
		if (_firehoseCacheService.getFirehoseCount() == 0) {
			deadLetterHandler(JsonUtils.toJson(event), "Zero firehoses registered during startup");
			return null;
		}

		int firehoseId = getFirehose(event);

		return sendToFirehose(event, firehoseId);
	}

	private PutRecordResult sendToFirehose(Event event, int firehoseId) {

		//https://docs.aws.amazon.com/firehose/latest/dev/basic-deliver.html
		//Adding a \n (new line) as record separator
		final String json = JsonUtils.toJson(event) + "\n";

		final String firehoseName = _firehoseCacheService.get(firehoseId);

		if (_firehoseNotFoundCache.isFirehoseNotFound(firehoseName)) {
			deadLetterHandler(json,
					String.format("Firehose %s does not exist or creation failed. Audit event dropped", firehoseName));
			return null;
		}

		PutRecordRequest putRecordRequest = new PutRecordRequest();

		putRecordRequest.setDeliveryStreamName(firehoseName);

		Record record = new Record().withData(ByteBuffer.wrap(json.getBytes()));

		putRecordRequest.setRecord(record);

		PutRecordResult putRecordResult = null;
		try {

			putRecordResult = _firehoseClient.putRecord(putRecordRequest);

			_log.debug("FirehoseService:::" + firehoseName + ":::" + json);

			_backoffFirehoseMap.remove(firehoseId);

		} catch (InvalidArgumentException iae) {
			/* The specified input parameter has a value that is not valid.
			HTTP Status Code: 400 */
			deadLetterHandler(json,
					String.format("Record not sent to firehose %s", firehoseName));
			return null;
		} catch (ResourceNotFoundException rnfe) {
			/* The specified resource could not be found.
			Firehose does not exist OR is getting created
			HTTP Status Code: 400 */

			_log.error("Record not sent to firehose "+firehoseName+":::"+json, rnfe);
			handleResourceNotFound(firehoseName, json, rnfe);
		} catch (ServiceUnavailableException sue) {
			/* The service is unavailable. Back off and retry the operation. If you continue to see the exception,
			throughput limits for the delivery stream may have been exceeded. For more information about limits
			and how to request an increase, see Amazon Kinesis Data Firehose Limits.
			HTTP Status Code: 500 */

			_log.error("Record not sent to firehose "+firehoseName+":::"+json, sue);
			handleFirehoseThrottling(event);

		} catch (Exception e) {
			//Any other exception, may be related to IAM permissions or other piece of code
			_log.error("Record not sent to firehose "+firehoseName+":::"+json);
			throw e;
		}

		return putRecordResult;
	}

	@Metered
	public PutRecordBatchResult sendBatchToFirehose(List<Record> recordList) {
		final ExponentialBackoff exponentialBackoff =
				new ExponentialBackoff(15000, 7200000, 900000, 2.0);

		PutRecordBatchRequest putRecordBatchRequest = new PutRecordBatchRequest();

		int firehoseId = getRandomFirehose();
		String firehoseDataStream = _firehoseCacheService.get(firehoseId);

		putRecordBatchRequest.setDeliveryStreamName(firehoseDataStream);

		putRecordBatchRequest.setRecords(recordList);

		int count = 0;

		PutRecordBatchResult putRecordBatchResult = null;

		while (count < MAX_ATTEMPTS) {
			try {
				putRecordBatchResult = _firehoseClient.putRecordBatch(putRecordBatchRequest);
				return putRecordBatchResult;
			} catch (InvalidArgumentException iae) {
				_log.error("Record set not sent to firehose. Invalid format. " + firehoseDataStream + ":::" +
						JsonUtil.toJson(recordList), iae);
				deadLetterHandler("Batched event json",
						"Could not push data to firehose due to InvalidArgumentException");
				return null;
			} catch (ResourceNotFoundException rnfe) {
				_log.error("Resource not found exception. " + JsonUtil.toJson(recordList), rnfe);
				count++;
				try { Thread.sleep(exponentialBackoff.nextInterval()); } catch (InterruptedException e) {}
			} catch (ServiceUnavailableException sue) {
				_log.error("Service unavailable exception. " + JsonUtil.toJson(recordList), sue);
				count++;
				try { Thread.sleep(exponentialBackoff.nextInterval()); } catch (InterruptedException e) {}
			}
		}
		if (count >= MAX_ATTEMPTS) {
			deadLetterHandler("Batched event json",
					"Could not push data to firehose due to maximum attempts exceeded. Maximum attempts: " + MAX_ATTEMPTS
					+ ". Current attempts: " + count + 1);
		}
		return putRecordBatchResult;
	}

	private Integer getFirehose(Event event) {
		_firehoseTrackingRecord = new FirehoseTrackingRecord();
		final String org = event.getOrg();
		final String type = event.getType();

		//Calculate hashCode
		final int hashCode = Objects.hash(org, type);// or Objects.hashCode(org + type)

		//Determine which firehose we need to use to persist
		final int firehoseNumber = Math.abs(hashCode % _firehoseCacheService.getFirehoseCount());
		_firehoseTrackingRecord.setInitialFirehoseId(firehoseNumber);

		return firehoseNumber;
	}

	private Integer getNextFirehose() {
		int currentFirehoseId = _firehoseTrackingRecord.getCurrentFirehoseId();
		final int initialFirehoseId = _firehoseTrackingRecord.getInitialFirehoseId();

		int nextFirehoseId = (currentFirehoseId + 1) % _firehoseCacheService.getFirehoseCount();
		if (nextFirehoseId == initialFirehoseId) {
			try {
				final long nextInterval = _backoffFirehoseMap.computeIfAbsent(initialFirehoseId,
						firehoseId -> new Backoff()).getNextInterval();
				Thread.sleep(nextInterval);
			} catch (InterruptedException e) {}
		}

		return nextFirehoseId;
	}

	private Integer getRandomFirehose() {
		Random random = new Random();
		return random.nextInt(_firehoseCacheService.getFirehoseCount());
	}

	/**
	 * Handle AWS throttling. Exponentially backoff per firehose.
	 * Retry for fixed number of times and then send the audit event to dead letter flow
	 * @param event
	 */
	private void handleFirehoseThrottling(Event event) {

		MetricsUtil.getCounter(FIREHOSE_THRESHOLD_REACHED, new HashMap<>()).inc();
		final int initialFirehoseId = _firehoseTrackingRecord.getInitialFirehoseId();

		FirehoseService.Backoff backoff = _backoffFirehoseMap.get(initialFirehoseId);
		if (backoff != null) {
			int retryCountThrottled = backoff.getRetryCountThrottled();
			if (retryCountThrottled >= _retryCount.getRetryCountThrottled()) {
				deadLetterHandler(JsonUtils.toJson(event),
						String.format("Firehose throttled for a long time. Audit event dropped after %s attempts"
								, _retryCount.getRetryCountThrottled()));
				return;
			}
		}

		final Integer firehoseId = getNextFirehose();
		backoff = _backoffFirehoseMap.get(initialFirehoseId);
		backoff.setRetryCountThrottled(backoff.getRetryCountThrottled() + 1);
		sendToFirehose(event, firehoseId);
	}

	private void handleResourceNotFound(String firehoseName, String json, Exception e) {

		MetricsUtil.getCounter(FIREHOSE_NOT_EXISTS, new HashMap<>()).inc();

		String firehoseStatus = getFirehoseStatus(firehoseName);
		if (firehoseStatus == null ||
				FirehoseStatus.CREATING_FAILED.toString().equals(firehoseStatus)) {
			_firehoseNotFoundCache.cacheFirehoseNotFound(firehoseName);
			deadLetterHandler(json,
					String.format("Firehose %s does not exist or creation failed. Audit event dropped", firehoseName));
			return;
		}
	}

	private String getFirehoseStatus(String firehoseDataStream) {
		DescribeDeliveryStreamRequest request = new DescribeDeliveryStreamRequest();
		request.setDeliveryStreamName(firehoseDataStream);
		DescribeDeliveryStreamResult result;
		try {
			result = _firehoseClient.describeDeliveryStream(request);
		} catch (ResourceNotFoundException rnfe) {
			_log.error("Firehose " + firehoseDataStream + " was not created");
			return null;
		}
		String status = result.getDeliveryStreamDescription().getDeliveryStreamStatus();
		return status;
	}

	/**
	 * Deadletter flow when we have exhausted all our retries
	 * @param json
	 * @param message
	 */
	private void deadLetterHandler(String json, String message) {
		//First iteration we want to log the audit event
		//Subsequently, we probably want to write the audit events to S3
		_log.error(message + " : " + json);
		MetricsUtil.getCounter(AUDIT_EVENT_PARQUET_DEADLETTER, new HashMap<>()).inc();
	}

	static class Backoff {
		private final ExponentialBackoff _backoff;
		private int _retryCountThrottled;

		Backoff() {
			//15 seconds, Minutes:5, 10, 20, ...,...Max:80 minutes
			_backoff = new ExponentialBackoff(15000, 4800000, 300000, 2.0);
		}

		synchronized long getNextInterval() {
			return _backoff.nextInterval();
		}

		synchronized int getRetryCountThrottled() {
			return _retryCountThrottled;
		}
		synchronized void setRetryCountThrottled(int retryCountThrottled) {
			this._retryCountThrottled = retryCountThrottled;
		}
	}

	//Class used to mock the retry limits
	class RetryCount {
		private static final int _throttledRetryCount = 10;
		int getRetryCountThrottled() {
			return _throttledRetryCount;
		}
	}
}
