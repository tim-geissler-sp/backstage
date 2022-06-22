/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.sender;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.sailpoint.notification.sender.common.event.NotificationRenderedEventHandler;
import com.sailpoint.notification.sender.common.lifecycle.NotificationLifecycleMetricsReporter;
import com.sailpoint.notification.sender.common.lifecycle.NotificationMetricsUtil;
import com.sailpoint.notification.sender.common.service.SenderDebugService;
import com.sailpoint.notification.sender.common.service.SenderService;
import com.sailpoint.notification.sender.email.MailClient;
import com.sailpoint.notification.sender.email.Validator;
import com.sailpoint.notification.sender.email.impl.SESMailClientProvider;
import com.sailpoint.notification.sender.email.repository.DynamoDBTenantSenderEmailRepository;
import com.sailpoint.notification.sender.email.repository.TenantSenderEmailRepository;
import com.sailpoint.notification.sender.email.service.MailService;
import com.sailpoint.notification.sender.slack.service.SlackService;
import com.sailpoint.notification.sender.teams.service.TeamsService;

public class NotificationSenderModule extends AbstractModule {
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void configure() {
		binder().requireExplicitBindings();

		bind(NotificationRenderedEventHandler.class);
		bind(MailService.class);
		bind(SlackService.class);
		bind(TeamsService.class);
		bind(SenderDebugService.class);
		bind(Validator.class);
		bind(MailClient.class).toProvider(SESMailClientProvider.class).in(Singleton.class);
		bind(NotificationLifecycleMetricsReporter.class);
		bind(NotificationMetricsUtil.class);
		bind(TenantSenderEmailRepository.class).to(DynamoDBTenantSenderEmailRepository.class).in(Singleton.class);
		bind(SenderService.class).in(Singleton.class);
	}
}
