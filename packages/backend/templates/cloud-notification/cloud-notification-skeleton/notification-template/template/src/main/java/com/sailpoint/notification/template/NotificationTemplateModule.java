/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.sailpoint.notification.template.common.manager.impl.TemplateRepositoryManagerImpl;
import com.sailpoint.notification.template.common.repository.TemplateRepository;
import com.sailpoint.notification.template.common.repository.TemplateRepositoryConfig;
import com.sailpoint.notification.template.common.repository.TemplateRepositoryDefault;
import com.sailpoint.notification.template.common.repository.impl.dynamodb.DynamoDBTemplateRepository;
import com.sailpoint.notification.template.common.repository.impl.json.JsonTemplateRepository;
import com.sailpoint.notification.template.engine.TemplateEngine;
import com.sailpoint.notification.template.engine.impl.velocity.TemplateEngineVelocity;
import com.sailpoint.notification.template.event.NotificationTemplateEventHandler;
import com.sailpoint.notification.template.service.NotificationTemplateDebugService;
import com.sailpoint.notification.template.service.NotificationTemplateService;
import com.sailpoint.notification.userpreferences.mapper.UserPreferencesMapper;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepositoryProvider;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;

/**
 * A support class for NotificationTemplate modules.
 */
public class NotificationTemplateModule extends AbstractModule {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void configure() {

		//  Bindings must be listed in a here in order to be injected
		binder().requireExplicitBindings();

		bind(NotificationTemplateEventHandler.class);
		bind(NotificationTemplateService.class);
		bind(NotificationTemplateDebugService.class);
		bind(TemplateEngine.class).to(TemplateEngineVelocity.class);
		bind(TemplateRepositoryDefault.class).to(JsonTemplateRepository.class)
				.in(Singleton.class);
		bind(TemplateRepositoryConfig.class).to(DynamoDBTemplateRepository.class)
				.in(Singleton.class);

		bind(TemplateRepository.class).to(TemplateRepositoryManagerImpl.class)
				.in(Singleton.class);

		bind(UserPreferencesMapper.class);
		bind(UserPreferencesRepository.class)
				.toProvider(UserPreferencesRepositoryProvider.class)
				.in(Singleton.class);
	}
}
