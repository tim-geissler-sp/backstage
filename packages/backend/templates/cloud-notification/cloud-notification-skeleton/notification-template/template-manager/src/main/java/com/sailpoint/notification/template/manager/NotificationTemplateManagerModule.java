/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.manager;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.sailpoint.notification.template.common.manager.TemplateRepositoryManager;
import com.sailpoint.notification.template.common.manager.impl.TemplateRepositoryManagerImpl;
import com.sailpoint.notification.template.common.repository.TemplateRepositoryConfig;
import com.sailpoint.notification.template.common.repository.TemplateRepositoryDefault;
import com.sailpoint.notification.template.common.repository.impl.dynamodb.DynamoDBTemplateRepository;
import com.sailpoint.notification.template.common.repository.impl.json.JsonTemplateRepository;
import com.sailpoint.notification.template.manager.rest.service.TemplateTranslationService;
import com.sailpoint.notification.userpreferences.mapper.UserPreferencesMapper;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepositoryProvider;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;

/**
 * A support class for NotificationManagerTemplate module.
 */
public class NotificationTemplateManagerModule extends AbstractModule {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void configure() {

		//  Bindings must be listed in a here in order to be injected
		binder().requireExplicitBindings();

		bind(TemplateRepositoryDefault.class).to(JsonTemplateRepository.class)
				.in(Singleton.class);
		bind(TemplateRepositoryConfig.class).to(DynamoDBTemplateRepository.class)
				.in(Singleton.class);

		bind(TemplateRepositoryManager.class).to(TemplateRepositoryManagerImpl.class)
				.in(Singleton.class);
		bind(TemplateTranslationService.class).in(Singleton.class);

		bind(UserPreferencesMapper.class);
		bind(UserPreferencesRepository.class)
				.toProvider(UserPreferencesRepositoryProvider.class)
				.in(Singleton.class);
	}
}
