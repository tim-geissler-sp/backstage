/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.manager.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.notification.template.common.manager.TemplateRepositoryManager;
import com.sailpoint.notification.template.common.model.version.TemplateVersion;
import com.sailpoint.notification.template.common.model.NotificationTemplate;
import com.sailpoint.notification.template.common.model.version.TemplateVersionInfo;
import com.sailpoint.notification.template.common.model.version.TemplateVersionUserInfo;
import com.sailpoint.notification.template.common.repository.TemplateRepositoryConfig;
import com.sailpoint.notification.template.common.repository.TemplateRepositoryDefault;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Template repository manager implementation.
 */
@Singleton
public class TemplateRepositoryManagerImpl implements TemplateRepositoryManager {

	@Inject
	private TemplateRepositoryConfig _configRepository;

	@Inject
	private TemplateRepositoryDefault _defaultRepository;

	@Inject
	public TemplateRepositoryManagerImpl(TemplateRepositoryConfig configRepository,
										 TemplateRepositoryDefault defaultRepository) {
		_configRepository = configRepository;
		_defaultRepository = defaultRepository;
	}

	@Override
	public TemplateRepositoryDefault getDefaultRepository() {
		return _defaultRepository;
	}

	@Override
	public List<NotificationTemplate> findAllByTenant(String tenant) {
		List<NotificationTemplate> result = _configRepository.findAllByTenant(tenant);
		if(result == null) {
			result = _defaultRepository.findAllByTenant(tenant);
		}
		return result;
	}

	@Override
	public NotificationTemplate getDefault(String tenant) {
		NotificationTemplate result = _configRepository.getDefault(tenant);
		if(result == null) {
			result = _defaultRepository.getDefault(tenant);
		}
		return result;
	}

	@Override
	public NotificationTemplate findOneByTenantAndKeyAndMediumAndLocale(String tenant, String key,
																		String medium, Locale locale) {
		NotificationTemplate result = _configRepository.findOneByTenantAndKeyAndMediumAndLocale(tenant, key, medium, locale);
		if(result == null) {
			result = _defaultRepository.findOneByTenantAndKeyAndMediumAndLocale(tenant, key, medium, locale);
		}
		return result;
	}

	@Override
	public TemplateVersion save(NotificationTemplate template, TemplateVersionInfo versionInfo) {
		return _configRepository.save(template, versionInfo);
	}

	@Override
	public boolean deleteAllByTenantAndKey(String tenant, String key) {
		return _configRepository.deleteAllByTenantAndKey(tenant, key);
	}

	@Override
	public boolean deleteAllByTenantAndKeyAndMediumAndLocale(String tenant, String key, String medium, Locale locale) {
		return _configRepository.deleteAllByTenantAndKeyAndMediumAndLocale(tenant, key, medium, locale);
	}

	@Override
	public boolean bulkDeleteAllByTenantAndKeyAndMediumAndLocale(String tenant, List<Map<String, String>> batch) {
		return _configRepository.bulkDeleteAllByTenantAndKeyAndMediumAndLocale(tenant, batch);
	}

	@Override
	public boolean deleteOneByIdAndTenant(String tenant, String id) {
		return _configRepository.deleteOneByIdAndTenant(tenant, id);
	}

	@Override
	public TemplateVersion getOneByIdAndTenant(String tenant, String id) {
		return _configRepository.getOneByIdAndTenant(tenant, id);
	}

	@Override
	public List<TemplateVersion> getAllLatestByTenant(String tenant) {
		return _configRepository.getAllLatestByTenant(tenant);
	}

	@Override
	public List<TemplateVersion> getAllLatestByTenantAndKey(String tenant, String key) {
		return _configRepository.getAllLatestByTenantAndKey(tenant, key);
	}

	@Override
	public List<TemplateVersion> getLatestByTenantAndKeyAndMediumAndLocale(String tenant, String key, String medium, Locale locale) {
		return _configRepository.getLatestByTenantAndKeyAndMediumAndLocale(tenant, key, medium, locale);
	}

	@Override
	public List<TemplateVersion> getAllLatestByTenantAndMediumAndLocale(String tenant, String medium, Locale locale) {
		return _configRepository.getAllLatestByTenantAndMediumAndLocale(tenant, medium, locale);
	}

	@Override
	public List<TemplateVersion> getAllVersions(String tenant, String key, String medium, Locale locale) {
		return _configRepository.getAllVersions(tenant, key, medium, locale);
	}

	@Override
	public TemplateVersion restoreVersionByIdAndTenant(String id, String tenant, TemplateVersionUserInfo user) {
		return _configRepository.restoreVersionByIdAndTenant(id, tenant, user);
	}
}
