/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.orgpreferences.repository.impl.dynamodb.entity;

import com.sailpoint.notification.api.event.dto.NotificationMedium;
import com.sailpoint.notification.orgpreferences.repository.dto.PreferencesDto;
import com.sailpoint.notification.orgpreferences.repository.dto.UserPreferencesDto;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test for PreferencesMapper.
 */
public class PreferencesMapperTest {

	@Test
	public void preferencesMapperTenantTest() {
		TenantPreferencesEntity entity = new TenantPreferencesEntity();
		entity.setNotificationKey("access_control");
		entity.setMediums(Arrays.asList("EMAIL"));
		entity.setModified(new Date());

		PreferencesDto dto = PreferencesMapper.tenantPreferencesEntityToPreferencesDto(entity);
		Assert.assertEquals(dto.getKey(), entity.getNotificationKey());
		assertEqualsMediums(dto.getMediums(), entity.getMediums());
		Assert.assertEquals(dto.getModified(), PreferencesMapper.toOffsetDateTime(entity.getModified()));

		entity = PreferencesMapper.preferencesDtoToTenantPreferencesEntity("acme-solar", dto);
		Assert.assertEquals(dto.getKey(), entity.getNotificationKey());
		assertEqualsMediums(dto.getMediums(), entity.getMediums());
		Assert.assertEquals(dto.getModified(), PreferencesMapper.toOffsetDateTime(entity.getModified()));
	}

	@Test
	public void preferencesMapperUserTest() {
		TenantUserPreferencesEntity entity = new TenantUserPreferencesEntity();
		entity.setUserId("userId");
		entity.setNotificationKey("access_control");
		entity.setMediums(Arrays.asList("EMAIL"));
		entity.setModified(new Date());

		UserPreferencesDto dto = PreferencesMapper.tenantUserPreferencesEntityToUserPreferencesDto(entity);
		Assert.assertEquals(dto.getUserId(), entity.getUserId());
		Assert.assertEquals(dto.getKey(), entity.getNotificationKey());
		assertEqualsMediums(dto.getMediums(), entity.getMediums());
		Assert.assertEquals(dto.getModified(), PreferencesMapper.toOffsetDateTime(entity.getModified()));

		entity = PreferencesMapper.userPreferencesDtoToTenantUserPreferencesEntity("acme-solar", dto);
		Assert.assertEquals(dto.getUserId(), entity.getUserId());
		Assert.assertEquals(dto.getKey(), entity.getNotificationKey());
		assertEqualsMediums(dto.getMediums(), entity.getMediums());
		Assert.assertEquals(dto.getModified(), PreferencesMapper.toOffsetDateTime(entity.getModified()));
	}

	private void assertEqualsMediums(List<NotificationMedium> mediumsList, List<String> stringList) {
		Assert.assertEquals(new ArrayList(mediumsList.stream()
				.map(Object::toString)
				.collect(Collectors.toList())), stringList);
	}
}