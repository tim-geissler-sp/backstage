/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.util;

import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Utility class for converting OffsetDateTimes to OffsetDateTimes with UTC offset.
 */
@Component
public class DateTimeToUTCConverter implements Converter<OffsetDateTime, OffsetDateTime> {
	@Override
	public OffsetDateTime convert(MappingContext<OffsetDateTime, OffsetDateTime> context) {
		if(context.getSource() == null) {
			return null;
		}
		return context.getSource().withOffsetSameInstant(ZoneOffset.UTC);
	}
}
