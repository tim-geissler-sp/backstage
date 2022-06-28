/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.util;

import com.amazonaws.regions.Regions;
import com.sailpoint.ets.domain.subscription.Subscription;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.infrastructure.web.dto.EventBridgeConfigDto;
import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;

import java.util.Map;

/**
 * Utility class for converting EventBridgeConfigDto map to EventBridgeConfigDto.
 */
public class EventBridgeConfigConverter implements Converter<Map<String, Object>, EventBridgeConfigDto> {
	public final static String AWS_ACCOUNT_NUMBER = "awsAccount";
	public final static String AWS_REGION = "awsRegion";
	public final static String AWS_PARTNER_EVENT_SOURCE_NAME = "awsPartnerEventSourceName";
	public final static String AWS_PARTNER_EVENT_SOURCE_ARN = "awsPartnerEventSourceArn";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EventBridgeConfigDto convert(MappingContext<Map<String, Object>, EventBridgeConfigDto> context) {
		Map<String, Object> eventBridgeConfig = context.getSource();
		Subscription subscription = (Subscription) context.getParent().getSource();
		if (subscription.getType() == SubscriptionType.EVENTBRIDGE) {
			EventBridgeConfigDto eventBridgeConfigDto = new EventBridgeConfigDto();
			eventBridgeConfigDto.setAwsAccount((String)eventBridgeConfig.get(AWS_ACCOUNT_NUMBER));
			eventBridgeConfigDto.setAwsRegion(Regions.fromName((String)eventBridgeConfig.get(AWS_REGION)).getName());
			return eventBridgeConfigDto;
		} else {
			return null;
		}
	}

}
