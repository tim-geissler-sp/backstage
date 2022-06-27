/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.userpreferences.mapper;

import com.sailpoint.atlas.util.ValidationUtil;
import com.sailpoint.notification.api.event.RecipientBuilder;
import com.sailpoint.notification.api.event.dto.Recipient;
import com.sailpoint.notification.userpreferences.dto.UserPreferences;
import com.sailpoint.notification.userpreferences.repository.impl.dynamodb.entity.UserPreferencesEntity;

import java.util.Optional;

/**
 * Maps the UserPreferences DTO to the UserPreferences persistence entity
 */
public class UserPreferencesMapper {

	private static final String DELIMITER = "__";

	/**
	 * Returns a UserPreferencesEntity based on the DTO, pod and org. The pod and org
	 * are used to construct the hash key 'tenant'
	 *
	 * @param dto The UserPreferences DTO
	 * @param pod The pod of this user
	 * @param org The org of this user
	 * @return an entity
	 */
	public UserPreferencesEntity toEntity(UserPreferences dto, String pod, String org) {

		UserPreferencesEntity entity = new UserPreferencesEntity();

		entity.setTenant(toHashKey(pod, org));
		entity.setRecipientId(dto.getRecipient().getId());
		entity.setName(dto.getRecipient().getName());
		entity.setEmail(dto.getRecipient().getEmail());
		entity.setPhone(dto.getRecipient().getPhone());
		dto.getBrand().ifPresent(entity::setBrand);

		return entity;
	}

	/**
	 * Returns a UserPreferences DTO from the entity.
	 * @param entity
	 * @return a UserPreference DTO
	 */
	public UserPreferences toDto(UserPreferencesEntity entity) {
		Recipient recipient = new RecipientBuilder().withEmail(entity.getEmail())
				.withId(entity.getRecipientId())
				.withName(entity.getName())
				.withPhone(entity.getPhone())
				.build();

		UserPreferences userPreferences = new UserPreferences.UserPreferencesBuilder().withRecipient(recipient)
				.withBrand(Optional.ofNullable(entity.getBrand()))
				.build();

		return userPreferences;
	}

	/**
	 * Creates a hashkey with pod and org
	 * @param pod
	 * @param org
	 * @return
	 */
	public static String toHashKey(String pod, String org) {

		ValidationUtil.require("pod", pod);
		ValidationUtil.require("org", org);

		return pod + DELIMITER + org;
	}
}
