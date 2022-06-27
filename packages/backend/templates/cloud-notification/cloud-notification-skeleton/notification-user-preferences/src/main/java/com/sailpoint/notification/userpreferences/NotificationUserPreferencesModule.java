/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.sailpoint.notification.orgpreferences.repository.TenantPreferencesRepository;
import com.sailpoint.notification.orgpreferences.repository.TenantUserPreferencesRepository;
import com.sailpoint.notification.orgpreferences.repository.impl.dynamodb.TenantPreferencesDynamoDBRepository;
import com.sailpoint.notification.orgpreferences.repository.impl.dynamodb.UserPreferencesDynamoDBRepository;
import com.sailpoint.notification.userpreferences.event.IdentityAttributesChangedEventHandler;
import com.sailpoint.notification.userpreferences.event.IdentityCreatedEventHandler;
import com.sailpoint.notification.userpreferences.event.IdentityDeletedEventHandler;
import com.sailpoint.notification.userpreferences.event.OrgLifecycleEventHandler;
import com.sailpoint.notification.userpreferences.event.UserPreferencesEventHandler;
import com.sailpoint.notification.userpreferences.mapper.UserPreferencesMapper;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepositoryProvider;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;
import com.sailpoint.notification.userpreferences.service.OrgLifecycleDebugService;
import com.sailpoint.notification.userpreferences.service.UserPreferencesDebugService;

public class NotificationUserPreferencesModule extends AbstractModule {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void configure() {
		binder().requireExplicitBindings();

		bind(UserPreferencesEventHandler.class);
		bind(IdentityAttributesChangedEventHandler.class);
		bind(IdentityCreatedEventHandler.class);
		bind(IdentityDeletedEventHandler.class);
		bind(OrgLifecycleEventHandler.class);
		bind(OrgLifecycleDebugService.class);
		bind(UserPreferencesDebugService.class);
		bind(UserPreferencesMapper.class);

		bind(UserPreferencesRepository.class)
				.toProvider(UserPreferencesRepositoryProvider.class)
				.in(Singleton.class);

		bind(TenantPreferencesRepository.class)
				.to(TenantPreferencesDynamoDBRepository.class)
				.in(Singleton.class);

		bind(TenantUserPreferencesRepository.class)
				.to(UserPreferencesDynamoDBRepository.class)
				.in(Singleton.class);
	}
}