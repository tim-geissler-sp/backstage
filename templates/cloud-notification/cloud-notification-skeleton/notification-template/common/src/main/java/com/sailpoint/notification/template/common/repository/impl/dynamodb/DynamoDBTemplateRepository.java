/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.repository.impl.dynamodb;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.Select;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.util.StringUtil;
import com.sailpoint.notification.template.common.model.version.TemplateVersion;
import com.sailpoint.notification.template.common.model.NotificationTemplate;
import com.sailpoint.notification.template.common.model.version.TemplateVersionInfo;
import com.sailpoint.notification.template.common.model.version.TemplateVersionUserInfo;
import com.sailpoint.notification.template.common.repository.TemplateRepositoryConfig;
import com.sailpoint.notification.template.common.repository.impl.dynamodb.entity.TemplateMapper;
import com.sailpoint.notification.template.common.repository.impl.dynamodb.entity.TemplatePersistentEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.sailpoint.notification.template.common.repository.impl.dynamodb.entity.TemplatePersistentEntity.KEY_NAME;
import static com.sailpoint.notification.template.common.repository.impl.dynamodb.entity.TemplatePersistentEntity.VERSION_NAME;

/**
 * Template repository implementation that uses a DynamoDB as data store.
 */
@Singleton
public class DynamoDBTemplateRepository implements TemplateRepositoryConfig {

	private static final Log _log = LogFactory.getLog(DynamoDBTemplateRepository.class);

	private final DynamoDBMapper _dynamoDBMapper;
	private final boolean _enableVersions;

	private final static String LATEST_VERSION = "V0";
	private final static String VERSION_PREFIX = "V";

	private final static String EXPRESSION_BY_VERSION_MEDIUM_LOCALE = "version =:v and medium = :m and locale = :l";
	private final static String EXPRESSION_BY_MEDIUM_LOCALE = "medium = :m and locale = :l";
	private final static String EXPRESSION_BY_VERSION = "version =:v";

	private final static String MEDIUM_ATTRIBUTE = ":m";
	private final static String VERSION_ATTRIBUTE = ":v";
	private final static String LOCALE_ATTRIBUTE = ":l";


	@VisibleForTesting
	@Inject
	public DynamoDBTemplateRepository(DynamoDBMapper dynamoDBMapper, AtlasConfig config) {
		_dynamoDBMapper = dynamoDBMapper;
		_enableVersions = config.getBoolean(TemplateVersion.HERMES_CONFIG_ENABLE_VERSION_SUPPORT, false);
	}

	@Override
	public List<NotificationTemplate> findAllByTenant(String tenant) {
		_log.debug("Retrieving templates by tenant " + tenant);
		return queryByTenantLatest(tenant).stream()
				.map(TemplateMapper::toDtoTemplate)
				.collect(Collectors.toList());
	}

	@Override
	public NotificationTemplate getDefault(String tenant) {
		return null;
	}

	@Override
	public NotificationTemplate findOneByTenantAndKeyAndMediumAndLocale(String tenant, String key,
																		String medium, Locale locale) {
		TemplatePersistentEntity entity = getByTenantAndKeyAndMediumAndLocale(tenant,key, medium, locale);
		if(entity == null) {
			return null;
		} else {
			return TemplateMapper.toDtoTemplate(entity);
		}
	}

	@Override
	public synchronized TemplateVersion save(NotificationTemplate template, TemplateVersionInfo versionInfo) {

		//save new template as latest.
		TemplatePersistentEntity entity = TemplateMapper.toEntity(LATEST_VERSION, template, versionInfo);

		//Version control based on best practices for DynamoDB:
		//https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/bp-sort-keys.html#bp-sort-keys-version-control

		//get all versions count.
		DynamoDBQueryExpression<TemplatePersistentEntity> query =
				getVersionsCountQuery(template);
		int count = _dynamoDBMapper.count(TemplatePersistentEntity.class, query);
		if (count > 0) {
			_log.info("Increment version for template " + template.getKey() + " versions " + count);
			//get the latest version.
			List<TemplatePersistentEntity> entities = _dynamoDBMapper.query(TemplatePersistentEntity.class,
					getLatestForTemplate(template));
			if (entities.size() > 1) {
				throw new IllegalStateException("More than one latest version for template "
						+ template.getKey() + " tenant " + template.getTenant());
			}

			if (entities.size() == 0) {
				throw new IllegalStateException("The latest version for template "
						+ template.getKey() + " tenant " + template.getTenant() + " was deleted");
			}

			TemplatePersistentEntity entityPrev = entities.get(0);
			if(_enableVersions) {
				//duplicated latest to with incremented version.
				entityPrev.setVersion(VERSION_PREFIX + count);
				_dynamoDBMapper.save(entityPrev);
			} else {
				//update existing template
				entity.setId(entityPrev.getId());
			}
		}

		_dynamoDBMapper.save(entity);
		return TemplateMapper.toDtoTemplateVersion(entity);
	}

	@Override
	public boolean deleteAllByTenantAndKey(String tenant, String key) {

		List<TemplatePersistentEntity> entities = queryByTenantAndKey(tenant, key);
		if(entities != null && entities.size() > 0) {
			entities.forEach(entity -> {
				_dynamoDBMapper.delete(entity);
				_log.info("Deleted template " + entity.getKey() + " for  " + entity.getTenant()
						+ " version " + entity.getVersion());
			});
		} else {
			_log.warn("Failed to delete template " + key + " for " + tenant + " not found");
			return false;
		}
		return true;

	}

	@Override
	public boolean deleteAllByTenantAndKeyAndMediumAndLocale(String tenant, String key, String medium, Locale locale) {
		List<TemplatePersistentEntity> entities = queryByTenantAndKeyAndMediumAndLocale(tenant, key, medium, locale, false);

		if(entities.size() == 0) {
			return false; //not found
		}

		if(entities.size() > 1) {
			throw new IllegalStateException("Not able delete more then one version for template "
					+ key + " tenant " + tenant);
		}
		deleteEntity(entities.get(0), false);
		return true;
	}

	@Override
	public boolean bulkDeleteAllByTenantAndKeyAndMediumAndLocale(String tenant, List<Map<String, String>> batch) {
		Map<String, TemplatePersistentEntity> entities = new HashMap<>();
		for(Map<String, String> attr : batch) {
			String key = attr.get("key");
			if(key == null) {
				return false; //key is required.
			}
			String medium = attr.get("medium");
			Locale locale = attr.get("locale") == null ? null : Locale.forLanguageTag(attr.get("locale"));
			List<TemplatePersistentEntity> records =
					queryByTenantAndKeyAndMediumAndLocale(tenant, key, medium, locale, false);
			if(records.size() == 0) {
				return false; //not found
			}
			//only uniq items
			records.forEach(r->entities.put(r.getId(), r));
		}
		List<DynamoDBMapper.FailedBatch> failedBatches = _dynamoDBMapper.batchDelete(entities.values());
		return failedBatches.size() == 0;
	}

	@Override
	public boolean deleteOneByIdAndTenant(String tenant, String id) {

		TemplatePersistentEntity entity = _dynamoDBMapper.load(TemplatePersistentEntity.class, tenant, id);
		if(entity != null) {
			deleteEntity(entity, true);
		} else {
			_log.warn("Failed to delete template for tenant " + tenant + ". Template with id: " + id + " not found");
			return false;
		}
		return true;
	}

	@Override
	public TemplateVersion getOneByIdAndTenant(String tenant, String id) {
		TemplatePersistentEntity entity =  _dynamoDBMapper.load(TemplatePersistentEntity.class, tenant, id);
		if(entity != null) {
			return TemplateMapper.toDtoTemplateVersion(entity);
		} else {
			return null;
		}
	}

	@Override
	public List<TemplateVersion> getAllLatestByTenant(String tenant) {
		return queryByTenantLatest(tenant).stream()
				.map(TemplateMapper::toDtoTemplateVersion)
				.collect(Collectors.toList());
	}

	@Override
	public List<TemplateVersion> getAllLatestByTenantAndKey(String tenant, String key) {
		Map<String, AttributeValue> expresionValues = new HashMap<>();
		expresionValues.put(VERSION_ATTRIBUTE, new AttributeValue().withS(LATEST_VERSION));

		return getTemplateVersions(tenant, key, expresionValues, EXPRESSION_BY_VERSION);
	}

	@Override
	public List<TemplateVersion> getLatestByTenantAndKeyAndMediumAndLocale(String tenant, String key, String medium, Locale locale) {
		return queryByTenantAndKeyAndMediumAndLocale(tenant, key, medium, locale, true)
				.stream()
				.map(TemplateMapper::toDtoTemplateVersion)
				.collect(Collectors.toList());
	}

	@Override
	public List<TemplateVersion> getAllLatestByTenantAndMediumAndLocale(String tenant, String medium, Locale locale) {
		Map<String, AttributeValue> expresionValues = new HashMap<>();
		expresionValues.put(MEDIUM_ATTRIBUTE, new AttributeValue().withS(medium.toUpperCase()));
		expresionValues.put(LOCALE_ATTRIBUTE, new AttributeValue().withS(locale.toLanguageTag()));

		DynamoDBQueryExpression<TemplatePersistentEntity> query =
				getQueryExpression(tenant, LATEST_VERSION,
						TemplatePersistentEntity.TENANT_VERSION_INDEX_NAME, VERSION_NAME);

		query.withFilterExpression(EXPRESSION_BY_MEDIUM_LOCALE)
				.withExpressionAttributeValues(expresionValues);

		return  _dynamoDBMapper.query(TemplatePersistentEntity.class, query)
				.stream()
				.map(TemplateMapper::toDtoTemplateVersion)
				.collect(Collectors.toList());
	}

	@Override
	public List<TemplateVersion> getAllVersions(String tenant, String key, String medium, Locale locale) {

		Map<String, AttributeValue> expresionValues = new HashMap<>();
		expresionValues.put(MEDIUM_ATTRIBUTE, new AttributeValue().withS(medium.toUpperCase()));
		expresionValues.put(LOCALE_ATTRIBUTE, new AttributeValue().withS(locale.toLanguageTag()));

		return getTemplateVersions(tenant, key, expresionValues, EXPRESSION_BY_MEDIUM_LOCALE);
	}

	@Override
	public TemplateVersion restoreVersionByIdAndTenant(String id, String tenant, TemplateVersionUserInfo user) {

		TemplatePersistentEntity entity = _dynamoDBMapper.load(TemplatePersistentEntity.class, tenant, id);
		if(entity == null) {
			throw new IllegalArgumentException("Failed to restore template for tenant " + tenant +
					". Template with id: " + id + " not found");
		}

		if(entity.getVersion().equals(LATEST_VERSION)) {
			throw new IllegalArgumentException("You can't restore latest template");
		}
		TemplateVersionInfo versionInfo = new TemplateVersionInfo(user, OffsetDateTime.now(), "Restored from version "
				+ entity.getVersion());

		return this.save(TemplateMapper.toDtoTemplate(entity), versionInfo);
	}

	/**
	 * Retrieves a list of all latest versions TemplatePersistentEntity for tenant.
	 *
	 * @param tenant The tenant.
	 * @return List of TemplatePersistentEntities.
	 */
	private List<TemplatePersistentEntity> queryByTenantLatest(String tenant) {
		if(StringUtil.isNullOrEmpty(tenant)) {
			throw new IllegalArgumentException("Tenant cannot be null or empty.");
		}

		return _dynamoDBMapper.query(TemplatePersistentEntity.class,
				getQueryExpression(tenant, LATEST_VERSION,  TemplatePersistentEntity.TENANT_VERSION_INDEX_NAME, VERSION_NAME));
	}

	/**
	 * Retrieves a list of all TemplatePersistentEntity by tenant and key.
	 *
	 * @param tenant The tenant.
	 * @param key The key.
	 * @return List of TemplatePersistentEntities.
	 */
	private List<TemplatePersistentEntity> queryByTenantAndKey(String tenant, String key) {
		if(StringUtil.isNullOrEmpty(tenant) || StringUtil.isNullOrEmpty(key)) {
			throw new IllegalArgumentException("Tenant or Key cannot be null or empty.");
		}

		return _dynamoDBMapper.query(TemplatePersistentEntity.class,
				getQueryExpression(tenant, key,  TemplatePersistentEntity.TENANT_KEY_INDEX_NAME, KEY_NAME));
	}
	/**
	 * Retrieves latest TemplatePersistentEntity by tenant and key and medium and locale.
	 * @param tenant tenant.
	 * @param key template key.
	 * @param medium template medium.
	 * @param locale template locale.
	 * @return TemplatePersistentEntity.
	 */
	private TemplatePersistentEntity getByTenantAndKeyAndMediumAndLocale(String tenant, String key,
																		 String medium, Locale locale) {
		List<TemplatePersistentEntity> entities = queryByTenantAndKeyAndMediumAndLocale(tenant,key,
				medium, locale, true);

		if(entities.size() == 0) {
			return null; //not found
		}

		if(entities.size() > 1) {
			throw new IllegalStateException("More then one latest version for template "
					+ key + " tenant " + tenant);
		}

		return entities.get(0);
	}

	/**
	 * Retrieves a list of versions TemplatePersistentEntity by tenant and key and medium and locale.
	 * @param tenant tenant.
	 * @param key template key.
	 * @param medium template medium.
	 * @param locale template locale.
	 * @return List of TemplatePersistentEntities.
	 */
	private List<TemplatePersistentEntity> queryByTenantAndKeyAndMediumAndLocale(String tenant, String key, String medium, Locale locale, boolean latest) {

		StringBuilder builder = new StringBuilder();
		Map<String, AttributeValue> expresionValues = new HashMap<>();
		if(latest) {
			builder.append("version =:v ");
			expresionValues.put(VERSION_ATTRIBUTE, new AttributeValue().withS(LATEST_VERSION));
		}
		if(medium != null) {
			if(builder.length() > 0) {
				builder.append("and ");
			}
			builder.append("medium = :m ");
			expresionValues.put(MEDIUM_ATTRIBUTE, new AttributeValue().withS(medium.toUpperCase()));
		}
		if(locale != null) {
			if(builder.length() > 0) {
				builder.append("and ");
			}
			builder.append("locale = :l ");
			expresionValues.put(LOCALE_ATTRIBUTE, new AttributeValue().withS(locale.toLanguageTag()));
		}

		DynamoDBQueryExpression<TemplatePersistentEntity> query =
				getQueryExpression(tenant, key,
						TemplatePersistentEntity.TENANT_KEY_INDEX_NAME, KEY_NAME);

		if(builder.length() > 0) {
			query.withFilterExpression(builder.toString())
					.withExpressionAttributeValues(expresionValues);
		}

		return  _dynamoDBMapper.query(TemplatePersistentEntity.class, query);
	}

	/**
	 * Get query expression by tenant, value, index and field.
	 *
	 * @param tenant tenant.
	 * @param value value.
	 * @param index index name.
	 * @param field field name.
	 * @return DynamoDBQueryExpression.
	 */
	private  DynamoDBQueryExpression<TemplatePersistentEntity> getQueryExpression(String tenant, String value,
																				  String index, String field) {
		final TemplatePersistentEntity userPreferencesEntity = new TemplatePersistentEntity();
		userPreferencesEntity.setId(null);
		userPreferencesEntity.setTenant(tenant);

		Condition rangeKeyCondition = new Condition();
		rangeKeyCondition.withComparisonOperator(ComparisonOperator.EQ)
				.withAttributeValueList(new AttributeValue().withS(value));

		final DynamoDBQueryExpression<TemplatePersistentEntity> queryExpression = new DynamoDBQueryExpression<>();
		queryExpression.withIndexName(index)
				.withHashKeyValues(userPreferencesEntity)
				.withRangeKeyCondition(field, rangeKeyCondition)
				.setConsistentRead(false);
		return queryExpression;
	}

	/**
	 * Get query expression for latest version from given notification template.
	 * @param template notification template.
	 * @return Dynamo query expression.
	 */
	private DynamoDBQueryExpression<TemplatePersistentEntity> getLatestForTemplate(NotificationTemplate template) {
		Map<String, AttributeValue> expresionValues = getCommonTemplateValues(template);

		expresionValues.put(VERSION_ATTRIBUTE, new AttributeValue().withS(LATEST_VERSION));

		DynamoDBQueryExpression<TemplatePersistentEntity> query =
				getQueryExpression(template.getTenant(), template.getKey(),
						TemplatePersistentEntity.TENANT_KEY_INDEX_NAME, KEY_NAME);
		query.withFilterExpression(EXPRESSION_BY_VERSION_MEDIUM_LOCALE)
				.withExpressionAttributeValues(expresionValues);

		return query;
	}

	/**
	 * Get query expression for version count from given notification template.
	 * @param template notification template.
	 * @return Dynamo query expression.
	 */
	private DynamoDBQueryExpression<TemplatePersistentEntity> getVersionsCountQuery(NotificationTemplate template) {
		Map<String, AttributeValue> expresionValues = getCommonTemplateValues(template);

		DynamoDBQueryExpression<TemplatePersistentEntity> query =
				getQueryExpression(template.getTenant(), template.getKey(),
						TemplatePersistentEntity.TENANT_KEY_INDEX_NAME, KEY_NAME);

		query.withFilterExpression(EXPRESSION_BY_MEDIUM_LOCALE)
				.withExpressionAttributeValues(expresionValues)
				.withSelect(Select.COUNT)
				.setConsistentRead(true);
		return query;
	}

	/**
	 * Get map of common template attributes from given notification template.
	 * @param template notification template.
	 * @return map of attributes.
	 */
	private Map<String, AttributeValue> getCommonTemplateValues(NotificationTemplate template) {
		Map<String, AttributeValue> expresionValues = new HashMap<>();
		expresionValues.put(MEDIUM_ATTRIBUTE, new AttributeValue().withS(template.getMedium()));
		expresionValues.put(LOCALE_ATTRIBUTE, new AttributeValue().withS(template.getLocale().toLanguageTag()));
		return expresionValues;
	}

	/**
	 * Get list of template versions based on provided paramaters.
	 * @param tenant tenant name.
	 * @param key template key.
	 * @param expresionValues expression values.
	 * @param expression expression.
	 * @return list of TemplateVersion
	 */
	private List<TemplateVersion> getTemplateVersions(String tenant, String key, Map<String, AttributeValue> expresionValues, String expression) {
		DynamoDBQueryExpression<TemplatePersistentEntity> query =
				getQueryExpression(tenant, key,
						TemplatePersistentEntity.TENANT_KEY_INDEX_NAME, KEY_NAME);

		query.withFilterExpression(expression)
				.withExpressionAttributeValues(expresionValues);

		return _dynamoDBMapper.query(TemplatePersistentEntity.class, query).stream()
				.map(TemplateMapper::toDtoTemplateVersion)
				.collect(Collectors.toList());
	}

	/**
	 * Delete notification template persistent entity.
	 * @param entity template persistent entity.
	 * @param checkForLastVersion indicator if need thow exeptions in case of latest version.
	 */
	private void deleteEntity(TemplatePersistentEntity entity, boolean checkForLastVersion) {

		if(checkForLastVersion && entity.getVersion().equals(LATEST_VERSION)) {
			throw new IllegalArgumentException("You can't delete latest template version");
		}

		_dynamoDBMapper.delete(entity);
		_log.info("Deleted template " + entity.getKey() + " with id: " + entity.getId()
				+ " for tenant " + entity.getId());
	}
}
