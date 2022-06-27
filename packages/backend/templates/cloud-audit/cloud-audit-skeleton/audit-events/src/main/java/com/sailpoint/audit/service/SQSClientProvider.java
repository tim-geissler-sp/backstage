/*
 * Copyright (C) 2022 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.buffered.QueueBufferConfig;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class SQSClientProvider implements Provider<AmazonSQS> {
	@VisibleForTesting
	public static AmazonSQS _amazonSQS;

	@Override
	public AmazonSQS get() {
		if(_amazonSQS == null) {
			final AmazonSQSAsync asyncClient = AmazonSQSAsyncClientBuilder.defaultClient();
			// Relevant default config values:
			// maxBatchOpenMs             200 ms
			// maxBatchSize               10
			// maxBatchSizeBytes          256 KB -- this is sqs max batch size
			// maxInflightOutboundBatches 5
			final QueueBufferConfig bufferConfig = new QueueBufferConfig();
			_amazonSQS = new AmazonSQSBufferedAsyncClient(asyncClient, bufferConfig);
		}
		return _amazonSQS;
	}
}
