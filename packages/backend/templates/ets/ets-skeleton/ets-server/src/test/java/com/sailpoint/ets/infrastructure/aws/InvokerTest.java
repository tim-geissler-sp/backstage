/*
 *  Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.infrastructure.aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.eventbridge.AmazonEventBridge;
import com.amazonaws.services.eventbridge.model.CreatePartnerEventSourceResult;
import com.amazonaws.services.lambda.AWSLambda;
import com.google.common.cache.LoadingCache;
import com.sailpoint.atlas.idn.RestClientProvider;
import com.sailpoint.ets.exception.ValidationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AwsPartnerEventSourceUtil
 */
@RunWith(MockitoJUnitRunner.class)
public class InvokerTest {

	@Mock
	private LoadingCache<String, AmazonEventBridge> _regionalEventBridge;

	@Mock
	private AmazonEventBridge amazonEventBridge;

	@Mock
	private AWSLambda awsLambda;

	@Mock
	private RestClientProvider _restClientProvider;

	private Invoker invoker;

	private final String TEST_ARN = "TEST-ARN:123";
	private final String TEST_ACCOUNT_ID = "123456789012";
	private final String TEST_REGION = Regions.CN_NORTH_1.getName();

	private CreatePartnerEventSourceResult _result = new CreatePartnerEventSourceResult();
	private final String  _partnerEventSourceName =  "aws.partner/sailpoint.com.unit.test/sub1234/idn/fire-and-forget";

	@Before
	public void setUp() {
		_result.setEventSourceArn(TEST_ARN);
	}

	@Test
	public void testCreatePartnerEventSourceHappyPath() throws ExecutionException {
		when(amazonEventBridge.createPartnerEventSource(any())).thenReturn(_result);
		when(_regionalEventBridge.get(anyString())).thenReturn(amazonEventBridge);

		invoker = new Invoker(awsLambda, _regionalEventBridge, _restClientProvider);

		String eventSourceArn = invoker.createPartnerEventSource(TEST_ACCOUNT_ID, TEST_REGION, _partnerEventSourceName);

		Assert.assertEquals(TEST_ARN, eventSourceArn);

	}

	@Test(expected= RuntimeException.class)
	public void testCreatePartnerSourceThrowsException() throws ExecutionException {
		when(amazonEventBridge.createPartnerEventSource(any())).thenThrow(RuntimeException.class);
		when(_regionalEventBridge.get(anyString())).thenReturn(amazonEventBridge);

		invoker = new Invoker(awsLambda, _regionalEventBridge, _restClientProvider);
		invoker.createPartnerEventSource(TEST_ACCOUNT_ID, TEST_REGION, _partnerEventSourceName);
	}

	@Test(expected= ValidationException.class)
	public void testRegionalEventBridgeThrowsException() throws ExecutionException {
		when(_regionalEventBridge.get(anyString())).thenThrow(RuntimeException.class);

		invoker = new Invoker(awsLambda, _regionalEventBridge, _restClientProvider);
		invoker.createPartnerEventSource(TEST_ACCOUNT_ID, TEST_REGION, _partnerEventSourceName);
	}


}
