/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.util;

import com.sailpoint.ets.domain.subscription.Subscription;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.infrastructure.web.dto.InlineConfigDto;
import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;

import java.util.Map;

/**
 * Utility class for converting InlineConfig map to InlineConfigDto.
 */
public class InlineConfigConverter implements Converter<Map<String, Object>, InlineConfigDto> {

	public final static String OUTPUT = "output";
	public final static String ERROR = "error";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public InlineConfigDto convert(MappingContext<Map<String, Object>, InlineConfigDto> context) {

		Subscription subscription = (Subscription) context.getParent().getSource();
		if (subscription.getType() == SubscriptionType.INLINE) {
			InlineConfigDto inlineConfigDto = new InlineConfigDto();
			inlineConfigDto.setOutput((Map<String, Object>) context.getSource().get(OUTPUT));
			inlineConfigDto.setError((String) context.getSource().get(ERROR));

			return inlineConfigDto;
		} else {
			return null;
		}
	}
}
