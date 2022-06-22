/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.repository;

import com.sailpoint.notification.template.common.model.version.TemplateVersion;
import com.sailpoint.notification.template.common.model.NotificationTemplate;
import com.sailpoint.notification.template.common.model.version.TemplateVersionInfo;
import com.sailpoint.notification.template.common.model.version.TemplateVersionUserInfo;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Template repository config interface for create/update/delete templates.
 */
public interface TemplateRepositoryConfig extends TemplateRepository {

	/**
	 * Save notification template in data storage. Every save will create new version.
	 * @param template notification template.
	 * @param versionInfo version info.
	 */
	TemplateVersion save(NotificationTemplate template, TemplateVersionInfo versionInfo);

	/**
	 * Delete all template's versions for given tenant and key.
	 * @param tenant tenant.
	 * @param key template key.
	 * @return true if success.
	 */
	boolean deleteAllByTenantAndKey(String tenant, String key);

	/**
	 * Delete all template's versions for given tenant, key, medium and locale.
	 * @param tenant tenant.
	 * @param key template key.
	 * @param medium template medium.
	 * @param locale template locale.
	 * @return true if successful.
	 */
	boolean deleteAllByTenantAndKeyAndMediumAndLocale(String tenant, String key, String medium, Locale locale);

	/**
	 * Bulk delete all template's versions for given tenant, key, medium and locale.
	 * It will delete only all or nothing.
	 * @param tenant tenant.
	 * @param batch list of delete operations.
	 * @return true if successful delete.
	 */
	boolean bulkDeleteAllByTenantAndKeyAndMediumAndLocale(String tenant, List<Map<String, String>> batch);

	/**
	 * Delete single temple version for given tenant and template id.
	 * @param tenant tenant.
	 * @param id id in data store.
	 * @return true if successful.
	 */
	boolean deleteOneByIdAndTenant(String tenant, String id);

	/**
	 * Get single temple version for given tenant and template id.
	 * @param tenant tenant.
	 * @param id id in data store.
	 * @return true if successful.
	 */
	TemplateVersion getOneByIdAndTenant(String tenant, String id);

	/**
	 * Get all latest version for template for tenant.
	 * @param tenant tenant.
	 * @return list of template version
	 */
	List<TemplateVersion> getAllLatestByTenant(String tenant);

	/**
	 * Get all latest version for template for tenant and key.
	 * @param tenant tenant.
	 * @param key template key.
	 * @return list of template version
	 */
	List<TemplateVersion> getAllLatestByTenantAndKey(String tenant, String key);

	/**
	 * Get latest version for template for tenant and key.
	 * @param tenant tenant.
	 * @param key template key.
	 * @param medium template medium.
	 * @param locale template locale.
	 * @return list of template version.
	 */
	List<TemplateVersion> getLatestByTenantAndKeyAndMediumAndLocale(String tenant, String key, String medium, Locale locale);

	/**
	 * Get all latest version for template for tenant and medium, and locale.
	 * @param tenant tenant.
	 * @param medium template medium.
	 * @param locale template locale.
	 * @return list of template version.
	 */
	List<TemplateVersion> getAllLatestByTenantAndMediumAndLocale(String tenant, String medium, Locale locale);

	/**
	 * Get list of versions for template.
	 * @param tenant tenant.
	 * @param key template key.
	 * @param medium template medium.
	 * @param locale template locale.
	 * @return list of template version
	 */
	List<TemplateVersion> getAllVersions(String tenant, String key, String medium, Locale locale);

	/**
	 * Restore latest version by template id for given tenant.
	 * @param id template id.
	 * @param tenant tenant.
	 * @param user user which is performed this operation.
	 * @return new template version
	 */
	TemplateVersion restoreVersionByIdAndTenant(String id, String tenant, TemplateVersionUserInfo user);
}
