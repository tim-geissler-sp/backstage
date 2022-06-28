/*
 * Copyright (c) 2017-2019 SailPoint Technologies, Inc.  All rights reserved
 */
package com.sailpoint.audit.integration;

import com.sailpoint.atlas.test.EnvironmentUtil;
import com.sailpoint.atlas.test.integration.BaseAtlasApplicationTest;
import com.sailpoint.atlas.test.integration.kafka.EnableKafkaServer;
import com.sailpoint.atlas.test.integration.kafka.KafkaServerRule;
import com.sailpoint.audit.AuditServerApplication;
import org.junit.Rule;

@EnableKafkaServer(topics="search")
public class AuditAtlasApplicationTest extends BaseAtlasApplicationTest<AuditServerApplication> {

	private static final int KAFKA_PORT =  EnvironmentUtil.findFreePort();

	@Rule
	public KafkaServerRule kafkaServerRule = new KafkaServerRule(KAFKA_PORT);

	public AuditAtlasApplicationTest() {

		super(AuditServerApplication.class);

		System.setProperty("kafka.servers", "localhost:" + KAFKA_PORT);
	}
}
