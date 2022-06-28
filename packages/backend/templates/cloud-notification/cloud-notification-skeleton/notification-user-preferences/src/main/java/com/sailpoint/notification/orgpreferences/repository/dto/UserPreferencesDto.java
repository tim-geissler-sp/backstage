/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.orgpreferences.repository.dto;

/**
 * An User Preferences Dto
 */
public class UserPreferencesDto extends PreferencesDto {

	private String userId;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	@Override
	public String toString() {
		return "UserPreferencesDto{" +
				"id='" + this.getUserId() + '\'' +
				", notificationKey='" + this.getKey() + '\'' +
				", userId='"  + this.getUserId() + '\'' +
				", medium='" + this.getMediums() + '\'' +
				", modified=" + this.getModified() +
				'}';
	}
}