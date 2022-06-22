/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.sailpoint.notification.event.OrgDeletedEventHandler;
import com.sailpoint.notification.service.VerifiedFromAddressService;
import com.sailpoint.notification.template.manager.rest.service.TemplateTranslationService;

public class NotificationModule extends AbstractModule {
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void configure() {
		binder().requireExplicitBindings();
		bind(VerifiedFromAddressService.class).in(Singleton.class);
		bind(TemplateTranslationService.class).in(Singleton.class);
		bind(OrgDeletedEventHandler.class);
	}
}
