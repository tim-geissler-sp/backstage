/*
 * Copyright (C) 2021 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.util;

import com.google.common.collect.ImmutableMap;
import com.sailpoint.ets.domain.subscription.Subscription;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.infrastructure.web.dto.SubscriptionDto;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for WorkflowConfigConverter.
 */
@RunWith(MockitoJUnitRunner.class)
public class WorkflowConfigConverterTest {

	private ModelMapper _modelMapper;

	@Before
	public void setup() {
		_modelMapper = new ModelMapper();
		_modelMapper.typeMap(Subscription.class, SubscriptionDto.class)
			.addMappings(mapper -> {
				mapper.using(new WorkflowConfigConverter()).map(Subscription::getConfig, SubscriptionDto::setWorkflowConfig);
			});
	}

	@Test
	public void testBuildDtoFromSubscription() {
		Subscription subscription = Subscription.builder()
			.id(UUID.randomUUID())
			.type(SubscriptionType.WORKFLOW)
			.scriptSource(Base64.encodeBase64String("decryptedScript".getBytes()))
			.config(ImmutableMap.of(WorkflowConfigConverter.WORKFLOW_ID, "workflow-1"))
			.build();

		SubscriptionDto subscriptionDto = _modelMapper.map(subscription, SubscriptionDto.class);

		assertEquals(subscriptionDto.getId(), subscription.getId().toString());
		assertEquals(subscriptionDto.getType(), SubscriptionType.WORKFLOW.name());
		assertEquals(subscriptionDto.getWorkflowConfig().getWorkflowId(), "workflow-1");
	}

}
