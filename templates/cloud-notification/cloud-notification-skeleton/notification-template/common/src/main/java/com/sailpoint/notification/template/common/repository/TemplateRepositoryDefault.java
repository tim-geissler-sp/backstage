/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.repository;

import com.sailpoint.notification.template.common.model.NotificationTemplate;

import java.util.List;
import java.util.Locale;

/**
 * Default template repository interface.
 */
public interface TemplateRepositoryDefault extends TemplateRepository {

	List<NotificationTemplate> findAll();

	List<NotificationTemplate> findAllByMedium(String medium);

	NotificationTemplate findOneByName(String name);

	NotificationTemplate findOneByKey(String key);

	NotificationTemplate findOneByTenantAndKeyAndMedium(String tenant, String key, String medium);

	NotificationTemplate findOneByTenantAndNameAndMedium(String tenant, String name, String medium);

	NotificationTemplate findOneByTenantAndKey(String tenant, String key);

	NotificationTemplate findOneByTenantAndMedium(String tenant, String medium);

	NotificationTemplate findOneByTenantAndLocale(String tenant, Locale locale);

	NotificationTemplate findOneByTenantAndMediumAndLocale(String tenant, String medium, Locale locale);

	NotificationTemplate findOneByNameAndMedium(String name, String medium);

	NotificationTemplate findOneByNameAndLocale(String name, Locale locale);

	NotificationTemplate findOneByKeyAndMedium(String key, String medium);

	NotificationTemplate findOneByKeyAndLocale(String key, Locale locale);

	NotificationTemplate findOneByNameAndMediumAndLocale(String name, String medium, Locale locale);

	NotificationTemplate findOneByKeyAndMediumAndLocale(String key, String medium, Locale locale);

}
