/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.repository;

import com.sailpoint.notification.template.common.model.NotificationTemplate;

import java.util.List;
import java.util.Locale;

/**
 * Template repository interface access to notification templates during runtime.
 */
public interface TemplateRepository {

	/**
	 * List latest versions of templates for given tenant.
	 * @param tenant tenant.
	 * @return list of notification templates.
	 */
	List<NotificationTemplate> findAllByTenant(String tenant);

	/**
	 * Get default template for given tenant.
	 * @param tenant tenant.
	 * @return default notification template.
	 */
	NotificationTemplate getDefault(String tenant);

	/**
	 * Find latest version of templates for given tenant, key, medium and locale.
	 *
	 * @param tenant tenant.
	 * @param key key.
	 * @param medium medium.
	 * @param locale locale.
	 * @return notification template
	 */
	NotificationTemplate findOneByTenantAndKeyAndMediumAndLocale(String tenant, String key, String medium, Locale locale);

}
