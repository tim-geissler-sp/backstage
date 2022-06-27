/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.interest.matcher.integration;

import com.sailpoint.atlas.AtlasApplication;
import com.sailpoint.atlas.event.AtlasDefaultEventHandlerModule;
import com.sailpoint.atlas.event.AtlasEventPlugin;
import com.sailpoint.atlas.health.AtlasHealthPlugin;
import com.sailpoint.atlas.test.integration.IdnAtlasIntegrationTest;
import com.sailpoint.atlas.test.integration.IdnAtlasIntegrationTestApplication;
import com.sailpoint.atlas.test.integration.kafka.EnableKafkaServer;
import com.sailpoint.notification.interest.matcher.NotificationInterestMatcherPlugin;
import com.sailpoint.notification.interest.matcher.interest.Interest;
import com.sailpoint.notification.interest.matcher.repository.InterestRepository;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for Interest Matcher Integration
 */
@EnableKafkaServer(topics="notification")
public class InterestMatcherIntegrationTest extends IdnAtlasIntegrationTest {

	@Override
	protected AtlasApplication createApplication() {
		return new IdnAtlasIntegrationTestApplication() {{
			registerPlugin(new AtlasHealthPlugin());
			registerPlugin(new AtlasEventPlugin());
			registerPlugin(new NotificationInterestMatcherPlugin());

			addServiceModule(new AtlasDefaultEventHandlerModule());
		}};
	}

	@Override
	public void initializeApplication() throws Exception {
		super.initializeApplication();
	}

	@Test
	public void interestMatcherRepositoryTest() {
		InterestRepository repo = _application.getInjector()
				.getInstance(InterestRepository.class);
		Interest interest = repo.getInterests().get(0);

		Assert.assertEquals("Access Approval Request", interest.getInterestName());
		Assert.assertEquals("email", interest.getCategoryName());
		Assert.assertEquals("notification", interest.getTopicName());
		Assert.assertEquals("ACCESS_APPROVAL_REQUESTED", interest.getEventType());
		Assert.assertEquals("jsonArrayPathDiscovery", interest.getDiscoveryType());
		Assert.assertEquals("[{\n" +
				"\t\t\t\t'jsonPath': '$.content.approvers[*].id',\n" +
				"\t\t\t\t'valueName': 'recipientId'\n" +
				"\t\t\t},\n" +
				"\t\t\t{\n" +
				"\t\t\t\t'jsonPath': '$.content.approvers[*].name',\n" +
				"\t\t\t\t'valueName': 'recipientEmail'\n" +
				"\t\t\t}]", interest.getDiscoveryConfig());
	}
}
