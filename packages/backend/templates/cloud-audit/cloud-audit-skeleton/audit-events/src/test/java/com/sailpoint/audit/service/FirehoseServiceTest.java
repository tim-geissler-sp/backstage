/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.DeliveryStreamDescription;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamResult;
import com.amazonaws.services.kinesisfirehose.model.InvalidArgumentException;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResult;
import com.amazonaws.services.kinesisfirehose.model.PutRecordResult;
import com.amazonaws.services.kinesisfirehose.model.Record;
import com.amazonaws.services.kinesisfirehose.model.ResourceNotFoundException;
import com.amazonaws.services.kinesisfirehose.model.ServiceUnavailableException;
import com.sailpoint.atlas.search.model.event.Event;
import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.audit.utils.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FirehoseServiceTest {

	@Mock
	AmazonKinesisFirehose _firehoseClient;

	@Mock
	PutRecordResult _result;

	@Mock
	FirehoseNotFoundCacheService _firehoseNotFoundCache;

	@Mock
	DescribeDeliveryStreamResult _firehoseDescribeResult;

	@Mock
	DeliveryStreamDescription _streamDescription;

	@Mock
	FirehoseCacheService _firehoseCacheService;

	@Mock
	com.sailpoint.audit.service.FirehoseService.RetryCount _retryCount;

	Map<Integer, FirehoseService.Backoff> _backOffMap = new HashMap<>();

	List<Record> _recordList;

	FirehoseService _sut;

	@Before
	public void setUp() {
		_sut = new FirehoseService();
		_sut._firehoseClient = _firehoseClient;
		_sut._backoffFirehoseMap = _backOffMap;
		_sut._firehoseNotFoundCache = _firehoseNotFoundCache;
		_sut._firehoseCacheService = _firehoseCacheService;
		_sut._retryCount = _retryCount;

		when(_firehoseClient.putRecord(any())).thenReturn(_result);
		when(_firehoseClient.describeDeliveryStream(any())).thenReturn(_firehoseDescribeResult);
		when(_firehoseDescribeResult.getDeliveryStreamDescription()).thenReturn(_streamDescription);
		when(_streamDescription.getDeliveryStreamStatus()).thenReturn("ACTIVE");
		when(_firehoseCacheService.getFirehoseCount()).thenReturn(1);
		when(_retryCount.getRetryCountThrottled()).thenReturn(1);

		List<Event> eventList = Arrays.asList(new Event(), new Event());
		_recordList = eventList.stream()
				.map(event -> {
					String json = JsonUtils.toJson(event);
					return new Record().withData(ByteBuffer.wrap(json.getBytes()));
				})
				.collect(Collectors.toList());

		TestUtils.setDummyRequestContext();
	}

	@Test
	public void testFirehose() {
		_sut.sendToFirehose(new Event());
		verify(_firehoseClient, times(1)).putRecord(any());
	}

	@Test
	public void testNoFirehose() {
		when(_firehoseCacheService.getFirehoseCount()).thenReturn(0);

		_sut.sendToFirehose(new Event());
		verify(_firehoseClient, times(0)).putRecord(any());
	}

	@Test
	public void testFirehoseServiceUnavailable() {
		when(_firehoseClient.putRecord(any()))
				.thenThrow(new ServiceUnavailableException("Throttled"))
				.thenReturn(_result);
		when(_retryCount.getRetryCountThrottled()).thenReturn(10);

		_sut.sendToFirehose(new Event());
		verify(_firehoseClient, times(2)).putRecord(any());
	}

	@Test
	public void testFirehoseThrottledRetryLimits() {
		when(_firehoseClient.putRecord(any())).thenThrow(new ServiceUnavailableException("Throttled"));

		_sut.sendToFirehose(new Event());
		//First attempt + 1 Retry = 2 attempts
		verify(_firehoseClient, times(2)).putRecord(any());
	}

	@Test
	public void testFirehoseResourceNotFound() {
		when(_firehoseClient.putRecord(any())).thenThrow(new ResourceNotFoundException("Firehose not created"));
		_sut.sendToFirehose(new Event());
		verify(_firehoseClient, times(1)).putRecord(any());

		when(_streamDescription.getDeliveryStreamStatus()).thenReturn("CREATING_FAILED");
		_sut.sendToFirehose(new Event());
		verify(_firehoseClient, times(2)).putRecord(any());

		when(_firehoseClient.describeDeliveryStream(any())).thenThrow(new ResourceNotFoundException("test"));
		_sut.sendToFirehose(new Event());
		verify(_firehoseClient, times(3)).putRecord(any());
	}

	@Test
	public void testFirehoseBatchRequest() {
		_sut.sendBatchToFirehose(_recordList);
		verify(_firehoseClient, times(1)).putRecordBatch(any(PutRecordBatchRequest.class));
	}

	@Test
	public void testFirehoseBatchRequestThrottled() {
		when(_firehoseClient.putRecordBatch(any(PutRecordBatchRequest.class)))
				.thenThrow(new ServiceUnavailableException("Throttled")).thenReturn(new PutRecordBatchResult());

		_sut.sendBatchToFirehose(_recordList);
		//First attempt + 1 Retry = 2 attempts
		verify(_firehoseClient, times(2)).putRecordBatch(any());
	}

	@Test
	public void testFirehoseBatchRequestResourceNotFound() {
		when(_firehoseClient.putRecordBatch(any(PutRecordBatchRequest.class)))
				.thenThrow(new ResourceNotFoundException("Not found")).thenReturn(new PutRecordBatchResult());

		_sut.sendBatchToFirehose(_recordList);
		//First attempt + 1 Retry = 2 attempts
		verify(_firehoseClient, times(2)).putRecordBatch(any());
	}

	@Test
	public void testFirehoseBatchRequestInvalidArgument() {
		when(_firehoseClient.putRecordBatch(any(PutRecordBatchRequest.class)))
				.thenThrow(new InvalidArgumentException("Invalid"));

		PutRecordBatchResult result = _sut.sendBatchToFirehose(_recordList);
		verify(_firehoseClient, times(1)).putRecordBatch(any());
		Assert.assertNull(result);
	}
}
