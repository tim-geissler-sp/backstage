/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.context.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.inject.Singleton;
import com.sailpoint.notification.context.common.model.GlobalContext;
import com.sailpoint.notification.context.common.model.GlobalContextEntity;
import com.sailpoint.notification.context.common.model.NotificationTemplateContextDto;
import org.joda.time.DateTime;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Utility class for map GlobalContext <=> GlobalContextEntity.
 */
@Singleton
public class GlobalContextMapper {

	private static final ObjectMapper _objectMapper = new ObjectMapper();

	static {
		_objectMapper.registerModule(new JodaModule());
		_objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
	}

	/**
	 * Converts GlobalContext to GlobalContextEntity.
	 *
	 * @param globalContext
	 * @return
	 */
	public static GlobalContextEntity toEntity(GlobalContext globalContext) {
		return _objectMapper.convertValue(globalContext, GlobalContextEntity.class);
	}

	/**
	 * Converts GlobalContextEntity to GlobalContext.
	 *
	 * @param globalContextEntity
	 * @return
	 */
	public static GlobalContext toDtoGlobalContext(GlobalContextEntity globalContextEntity) {
		return _objectMapper.convertValue(globalContextEntity, GlobalContext.class);
	}

	/**
	 * Converts GlobalContext to NotificationTemplateContextDto.
	 * @param globalContext
	 * @return
	 */
	public static NotificationTemplateContextDto toNotificationTemplateContextDto(GlobalContext globalContext) {
		NotificationTemplateContextDto notificationTemplateContextDto = new NotificationTemplateContextDto();
		notificationTemplateContextDto.setAttributes(globalContext.getAttributes());
		notificationTemplateContextDto.setCreated(toOffsetDateTime(globalContext.getCreated()));
		notificationTemplateContextDto.setModified(toOffsetDateTime(globalContext.getModified()));

		return notificationTemplateContextDto;
	}

	private static OffsetDateTime toOffsetDateTime(DateTime dateTime) {
		Instant instant = Instant.ofEpochMilli(dateTime.getMillis());
		return OffsetDateTime.ofInstant(instant, ZoneId.of(dateTime.getZone().getID()));
	}
}
