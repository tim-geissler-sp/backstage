/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved
 */

package com.sailpoint.notification.integration;


import com.sailpoint.atlas.test.EnvironmentUtil;
import com.sailpoint.atlas.test.integration.BaseAtlasApplicationTest;
import com.sailpoint.atlas.test.integration.dynamodb.DynamoDBServerRule;
import com.sailpoint.atlas.test.integration.dynamodb.EnableInMemoryDynamoDB;
import com.sailpoint.atlas.test.integration.kafka.EnableKafkaServer;
import com.sailpoint.atlas.test.integration.kafka.KafkaServerRule;
import com.sailpoint.notification.NotificationServerApplication;
import org.junit.Rule;

@EnableKafkaServer
@EnableInMemoryDynamoDB
public class NotificationAtlasApplicationTest extends BaseAtlasApplicationTest<NotificationServerApplication> {

	private static final int KAFKA_PORT =  EnvironmentUtil.findFreePort();

	@Rule
	public KafkaServerRule kafkaServerRule = new KafkaServerRule(KAFKA_PORT);

	@Rule
	public DynamoDBServerRule dynamoDBServerRule = new DynamoDBServerRule(EnvironmentUtil.findFreePort());

	public NotificationAtlasApplicationTest() {
		super(NotificationServerApplication.class);
		System.setProperty("kafka.servers", "localhost:" + KAFKA_PORT);
	}
}
