/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.interest.matcher.service;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.sailpoint.notification.interest.matcher.event.InterestMatcherEventHandler;
import com.sailpoint.notification.interest.matcher.repository.InterestRepository;
import com.sailpoint.notification.interest.matcher.repository.InterestRepositoryProvider;
import com.sailpoint.notification.interest.matcher.repository.impl.json.InterestRepositoryJsonImpl;
import com.sailpoint.notification.sender.common.lifecycle.NotificationMetricsUtil;
import com.sailpoint.notification.sender.slack.service.SlackService;
import com.sailpoint.notification.sender.teams.service.TeamsService;

public class NotificationInterestMatcherModule extends AbstractModule {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void configure() {
		binder().requireExplicitBindings();

		bind(InterestMatcherEventHandler.class);
		bind(InterestMatcherDebugService.class);

		bind(InterestRepositoryJsonImpl.class);
		bind(InterestRepository.class)
				.toProvider(InterestRepositoryProvider.class)
				.in(Singleton.class);

		bind(NotificationMetricsUtil.class);
		bind(TeamsService.class);
		bind(SlackService.class);
	}
}
