/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.repository.impl.json;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.notification.sender.common.repository.BaseJsonRepository;
import com.sailpoint.notification.template.common.model.NotificationTemplate;
import com.sailpoint.notification.template.common.repository.TemplateRepositoryDefault;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Template repository implementation that uses a JSON file as data store.
 */
@Singleton
public class JsonTemplateRepository extends BaseJsonRepository<NotificationTemplate>
		implements TemplateRepositoryDefault  {

	public static final String ATLAS_NOTIFICATION_TEMPLATE_REPOSITORY_LOCATION = "ATLAS_NOTIFICATION_TEMPLATE_REPOSITORY_LOCATION";

	private static final String DEFAULT_INTEREST_REPOSITORY_LOCATION = "templates.json";

	private static final String DEFAULT_TEMPLATE_NAME = "default_template";

	private final NotificationTemplate _defaultTemplate;

	@Inject
	public JsonTemplateRepository(AtlasConfig atlasConfig) {
		super(atlasConfig.getString(ATLAS_NOTIFICATION_TEMPLATE_REPOSITORY_LOCATION),
				DEFAULT_INTEREST_REPOSITORY_LOCATION, NotificationTemplate.class);
		_defaultTemplate = findOneByKey(DEFAULT_TEMPLATE_NAME);
	}

	@Override
	public List<NotificationTemplate> findAll() {
		return getRepository();
	}

	@Override
	public List<NotificationTemplate> findAllByTenant(String tenant) {
		return getRepository().stream()
				.filter(notificationTemplate -> tenant.equalsIgnoreCase(notificationTemplate.getTenant()))
				.collect(Collectors.toList());
	}

	@Override
	public NotificationTemplate getDefault(String tenant) {
		return _defaultTemplate;
	}

	@Override
	public NotificationTemplate findOneByTenantAndKeyAndMediumAndLocale(String tenant, String key,
																		String medium, Locale locale) {
		NotificationTemplate template = getRepository().stream()
				.filter(notificationTemplate -> tenant.equalsIgnoreCase(notificationTemplate.getTenant())
						&& medium.equalsIgnoreCase(notificationTemplate.getMedium())
						&& locale.equals(notificationTemplate.getLocale())
						&& key.equalsIgnoreCase(notificationTemplate.getKey()))
				.findAny()
				.orElse(null);

		if(template == null) {
			template = findOneByKeyAndMediumAndLocale(key, medium, locale);
		}

		return template;
	}

	@Override
	public NotificationTemplate findOneByTenantAndNameAndMedium(String tenant, String name, String medium) {
		return getRepository().stream()
				.filter(notificationTemplate -> tenant.equalsIgnoreCase(notificationTemplate.getTenant())
						&& medium.equalsIgnoreCase(notificationTemplate.getMedium())
						&& name.equalsIgnoreCase(notificationTemplate.getName()))
				.findAny()
				.orElse(null);
	}

	@Override
	public NotificationTemplate findOneByTenantAndKey(String tenant, String key) {
		NotificationTemplate template = getRepository().stream()
				.filter(notificationTemplate -> tenant.equalsIgnoreCase(notificationTemplate.getTenant())
						&& key.equalsIgnoreCase(notificationTemplate.getKey()))
				.findAny()
				.orElse(null);

		if(template == null) {
			template = getRepository().stream()
					.filter(notificationTemplate -> notificationTemplate.getTenant() == null
							&& key.equalsIgnoreCase(notificationTemplate.getKey()))
					.findAny()
					.orElse(null);
		}
		return template;
	}

	@Override
	public NotificationTemplate findOneByTenantAndKeyAndMedium(String tenant, String key, String medium) {
		return getRepository().stream()
				.filter(notificationTemplate -> tenant.equalsIgnoreCase(notificationTemplate.getTenant())
						&& medium.equalsIgnoreCase(notificationTemplate.getMedium())
						&& key.equalsIgnoreCase(notificationTemplate.getKey()))
				.findAny()
				.orElse(null);
	}

	@Override
	public List<NotificationTemplate> findAllByMedium(String medium) {
		return getRepository().stream()
				.filter(notificationTemplate -> medium.equalsIgnoreCase(notificationTemplate.getMedium()))
				.collect(Collectors.toList());
	}

	@Override
	public NotificationTemplate findOneByName(String name) {
		return getRepository().stream()
				.filter(notificationTemplate -> name.equalsIgnoreCase(notificationTemplate.getName()))
				.findAny()
				.orElse(null);
	}

	@Override
	public NotificationTemplate findOneByKey(String key) {
		return getRepository().stream()
				.filter(notificationTemplate -> key.equalsIgnoreCase(notificationTemplate.getKey()))
				.findAny()
				.orElse(null);
	}

	@Override
	public NotificationTemplate findOneByTenantAndMedium(String tenant, String medium) {
		return getRepository().stream()
				.filter(notificationTemplate -> tenant.equalsIgnoreCase(notificationTemplate.getTenant())
						&& medium.equalsIgnoreCase(notificationTemplate.getMedium()))
				.findAny()
				.orElse(null);
	}

	@Override
	public NotificationTemplate findOneByTenantAndLocale(String tenant, Locale locale) {
		return getRepository().stream()
				.filter(notificationTemplate -> tenant.equalsIgnoreCase(notificationTemplate.getTenant())
						&& locale.equals(notificationTemplate.getLocale()))
				.findAny()
				.orElse(null);
	}

	@Override
	public NotificationTemplate findOneByNameAndMedium(String name, String medium) {
		return getRepository().stream()
				.filter(notificationTemplate -> name.equalsIgnoreCase(notificationTemplate.getName())
						&& medium.equalsIgnoreCase(notificationTemplate.getMedium()))
				.findAny()
				.orElse(null);
	}

	@Override
	public NotificationTemplate findOneByNameAndLocale(String name, Locale locale) {
		return getRepository().stream()
				.filter(notificationTemplate -> name.equalsIgnoreCase(notificationTemplate.getName())
						&& locale.equals(notificationTemplate.getLocale()))
				.findAny()
				.orElse(null);
	}

	@Override
	public NotificationTemplate findOneByKeyAndMedium(String key, String medium) {
		return getRepository().stream()
				.filter(notificationTemplate -> key.equalsIgnoreCase(notificationTemplate.getKey())
						&& medium.equalsIgnoreCase(notificationTemplate.getMedium()))
				.findAny()
				.orElse(null);
	}

	@Override
	public NotificationTemplate findOneByKeyAndLocale(String key, Locale locale) {
		return getRepository().stream()
				.filter(notificationTemplate -> key.equalsIgnoreCase(notificationTemplate.getKey())
						&& locale.equals(notificationTemplate.getLocale()))
				.findAny()
				.orElse(null);
	}

	@Override
	public NotificationTemplate findOneByNameAndMediumAndLocale(String name, String medium, Locale locale) {
		return getRepository().stream()
				.filter(notificationTemplate -> name.equalsIgnoreCase(notificationTemplate.getName())
						&& medium.equalsIgnoreCase(notificationTemplate.getMedium())
						&& locale.equals(notificationTemplate.getLocale()))
				.findAny()
				.orElse(null);
	}

	@Override
	public NotificationTemplate findOneByTenantAndMediumAndLocale(String tenant, String medium, Locale locale) {
		return getRepository().stream()
				.filter(notificationTemplate -> tenant.equalsIgnoreCase(notificationTemplate.getTenant())
						&& medium.equalsIgnoreCase(notificationTemplate.getMedium())
						&& locale.equals(notificationTemplate.getLocale()))
				.findAny()
				.orElse(null);
	}

	@Override
	public NotificationTemplate findOneByKeyAndMediumAndLocale(String key, String medium, Locale locale) {
		return getRepository().stream()
				.filter(notificationTemplate -> key.equalsIgnoreCase(notificationTemplate.getKey())
						&& medium.equalsIgnoreCase(notificationTemplate.getMedium())
						&& locale.equals(notificationTemplate.getLocale()))
				.findAny()
				.orElse(null);
	}
}
