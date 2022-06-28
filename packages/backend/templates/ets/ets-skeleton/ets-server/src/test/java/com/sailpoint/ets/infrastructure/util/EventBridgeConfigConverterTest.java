/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.util;

import com.amazonaws.regions.Regions;
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
 * Unit tests for EventBridgeConfigConverter.
 */
@RunWith(MockitoJUnitRunner.class)
public class EventBridgeConfigConverterTest {

	private ModelMapper _modelMapper;

	@Before
	public void setup() {
		_modelMapper = new ModelMapper();
		_modelMapper.typeMap(Subscription.class, SubscriptionDto.class)
			.addMappings(mapper -> {
				mapper.using(new EventBridgeConfigConverter()).map(Subscription::getConfig, SubscriptionDto::setEventBridgeConfig);
			});
	}

	@Test
	public void testBuildDtoFromSubscription() {
		Subscription subscription = Subscription.builder()
			.id(UUID.randomUUID())
			.type(SubscriptionType.EVENTBRIDGE)
			.scriptSource(Base64.encodeBase64String("decryptedScript".getBytes()))
			.config(ImmutableMap.of(EventBridgeConfigConverter.AWS_ACCOUNT_NUMBER, "123456789012", EventBridgeConfigConverter.AWS_REGION, Regions.AP_NORTHEAST_1.getName()))
			.build();

		SubscriptionDto subscriptionDto = _modelMapper.map(subscription, SubscriptionDto.class);

		assertEquals(subscriptionDto.getId(), subscription.getId().toString());
		assertEquals(subscriptionDto.getType(), SubscriptionType.EVENTBRIDGE.name());
		assertEquals(subscriptionDto.getEventBridgeConfig().getAwsAccount(), "123456789012");
		assertEquals(subscriptionDto.getEventBridgeConfig().getAwsRegion(), Regions.AP_NORTHEAST_1.getName());

	}

}
