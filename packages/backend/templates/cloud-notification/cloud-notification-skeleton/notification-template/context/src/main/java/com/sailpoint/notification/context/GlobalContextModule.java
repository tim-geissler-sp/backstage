/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.context;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.sailpoint.notification.context.common.repository.GlobalContextRepository;
import com.sailpoint.notification.context.common.repository.impl.dynamodb.DynamoDBGlobalContextRepository;
import com.sailpoint.notification.context.event.BrandingChangedEventHandler;
import com.sailpoint.notification.context.event.EmailRedirectionEventHandler;
import com.sailpoint.notification.context.event.OrgLifecycleEventHandler;
import com.sailpoint.notification.context.service.GlobalContextDebugService;
import com.sailpoint.notification.context.service.GlobalContextService;

/**
 * A support class for GlobalContext module.
 */

public class GlobalContextModule extends AbstractModule {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void configure() {

		//  Bindings must be listed in a here in order to be injected
		binder().requireExplicitBindings();

		bind(BrandingChangedEventHandler.class);
		bind(EmailRedirectionEventHandler.class);
		bind(OrgLifecycleEventHandler.class);
		bind(GlobalContextService.class);
		bind(GlobalContextDebugService.class);
		bind(GlobalContextRepository.class).to(DynamoDBGlobalContextRepository.class)
				.in(Singleton.class);
	}
}
