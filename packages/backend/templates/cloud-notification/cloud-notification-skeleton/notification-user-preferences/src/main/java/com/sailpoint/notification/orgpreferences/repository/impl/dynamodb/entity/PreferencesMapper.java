/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.orgpreferences.repository.impl.dynamodb.entity;

import com.sailpoint.notification.api.event.dto.NotificationMedium;
import com.sailpoint.notification.orgpreferences.repository.dto.PreferencesDto;
import com.sailpoint.notification.orgpreferences.repository.dto.UserPreferencesDto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Utility class for preferences mapper.
 */
public class PreferencesMapper {

	private PreferencesMapper() {
	}
	public static UserPreferencesDto tenantUserPreferencesEntityToUserPreferencesDto(TenantUserPreferencesEntity entity) {
		if(entity == null) {
			return null;
		}
		UserPreferencesDto result = basePreferencesEntityToPreferencesDto(new UserPreferencesDto(), entity);
		result.setUserId(entity.getUserId());
		return result;

	}

	public static TenantUserPreferencesEntity userPreferencesDtoToTenantUserPreferencesEntity(String tenant, UserPreferencesDto userPreferencesDto) {
		if(userPreferencesDto == null) {
			return null;
		}
		TenantUserPreferencesEntity result = preferencesDtoToBasePreferencesEntity(tenant, new TenantUserPreferencesEntity(), userPreferencesDto);
		result.setUserId(userPreferencesDto.getUserId());
		return result;
	}

	public static PreferencesDto tenantPreferencesEntityToPreferencesDto(TenantPreferencesEntity entity) {
		if(entity == null) {
			return null;
		}
		return basePreferencesEntityToPreferencesDto(new PreferencesDto(), entity);

	}

	public static TenantPreferencesEntity preferencesDtoToTenantPreferencesEntity(String tenant, PreferencesDto preferencesDto) {
		if(preferencesDto == null) {
			return null;
		}
		return preferencesDtoToBasePreferencesEntity(tenant, new TenantPreferencesEntity(), preferencesDto);
	}

	static OffsetDateTime toOffsetDateTime(Date date) {
		if(date == null) {
			return null;
		}
		Instant instant = date.toInstant();
		return instant.atOffset(ZoneOffset.UTC);
	}

	static Date toDate(OffsetDateTime date) {
		if(date == null) {
			return null;
		}
		ZonedDateTime zdt = date.toInstant().atZone(ZoneOffset.UTC) ;
		return Date.from(zdt.toInstant());
	}

	private static <T extends PreferencesDto> T basePreferencesEntityToPreferencesDto(T result, BasePreferencesEntity entity) {
		result.setKey(entity.getNotificationKey());
		result.setMediums(
				entity.getMediums()
						.stream()
						.map(NotificationMedium::valueOf)
						.collect(Collectors.toList()));
		result.setModified(toOffsetDateTime(entity.getModified()));
		return result;
	}

	private static <T extends BasePreferencesEntity> T preferencesDtoToBasePreferencesEntity(String tenant, T result, PreferencesDto preferencesDto) {
		result.setTenant(tenant);
		result.setNotificationKey(preferencesDto.getKey());
		result.setMediums(preferencesDto.getMediums()
				.stream()
				.map(Object::toString)
				.collect(Collectors.toList()));
		result.setModified(toDate(preferencesDto.getModified()));
		return result;
	}
}