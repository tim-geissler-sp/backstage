/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.userpreferences.repository.impl.dynamodb;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.util.StringUtil;
import com.sailpoint.notification.userpreferences.dto.UserPreferences;
import com.sailpoint.notification.userpreferences.mapper.UserPreferencesMapper;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;
import com.sailpoint.notification.userpreferences.repository.impl.dynamodb.entity.UserPreferencesEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.sailpoint.metrics.MetricsUtil;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UserPreferences repository implementation with DynamoDB.
 */
public class DynamoDBRepository implements UserPreferencesRepository {

	DynamoDBMapper _dynamoDBMapper;

	UserPreferencesMapper _userPreferencesMapper;

	private static final String DYNAMO_DB_REPO_PREFIX = DynamoDBRepository.class.getName();


	private static final Log _log = LogFactory.getLog(DynamoDBRepository.class);

	@VisibleForTesting
	@Inject
	public DynamoDBRepository(DynamoDBMapper dynamoDBMapper, UserPreferencesMapper userPreferencesMapper) {
		_dynamoDBMapper = dynamoDBMapper;
		_userPreferencesMapper = userPreferencesMapper;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UserPreferences findByRecipientId(String recipientId) {
		if(StringUtil.isNullOrEmpty(recipientId)) {
			_log.warn("RecipientId null or empty");
			return null;
		}
		RequestContext rc = RequestContext.ensureGet();
		if(StringUtil.isNullOrEmpty(recipientId)) {
			return null;
		}
		UserPreferencesEntity entity = _dynamoDBMapper.load(UserPreferencesEntity.class, _userPreferencesMapper.toHashKey(rc.getPod(), rc.getOrg()), recipientId);
		return entity != null ? _userPreferencesMapper.toDto(entity) : null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void create(UserPreferences userPreferences) {
		//TODO: In the DynamoDB CRUD story, validate that it doesn't exist
		saveUserPreferences(userPreferences);
		_log.info("Created user preferences for  " + userPreferences.getRecipient().getId());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deleteByRecipientId(String recipientId) {
		if(StringUtil.isNullOrEmpty(recipientId)) {
			_log.warn("RecipientId null or empty");
			return; //noting to do.
		}
		RequestContext rc = RequestContext.ensureGet();
		UserPreferencesEntity entity = _dynamoDBMapper.load(UserPreferencesEntity.class, _userPreferencesMapper.toHashKey(rc.getPod(), rc.getOrg()), recipientId);
		if(entity != null) {
			_dynamoDBMapper.delete(entity);
			_log.info("Deleted user preferences for  " + recipientId);
		} else {
			_log.warn("Failed to delete user preferences for " + recipientId);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<UserPreferences> findAllByTenant(String tenant) {
		_log.info("Retrieving user preferences by tenant " + tenant);
		return queryByTenant(tenant).stream()
				.map(_userPreferencesMapper::toDto)
				.collect(Collectors.toList());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deleteByTenant(String tenant) {
		_log.info("Deleting user preferences by tenant " + tenant);
		List<UserPreferencesEntity> userPreferencesEntities = queryByTenant(tenant);

		_dynamoDBMapper.batchDelete(userPreferencesEntities);
	}

	/**
	 * Retrieves a list of UserPreferencesEntity by tenant.
	 *
	 * @param tenant The tenant.
	 * @return List<UserPreferencesEntity> List of user preferences.
	 */
	private List<UserPreferencesEntity> queryByTenant(String tenant) {
		if(StringUtil.isNullOrEmpty(tenant)) {
			_log.warn("Tenant null or empty");
			return Collections.emptyList(); //return empty if tenant empty by some reason.
		}

		final UserPreferencesEntity userPreferencesEntity = new UserPreferencesEntity();
		userPreferencesEntity.setTenant(tenant);

		final DynamoDBQueryExpression<UserPreferencesEntity> queryExpression = new DynamoDBQueryExpression<>();
		queryExpression.withHashKeyValues(userPreferencesEntity);

		return _dynamoDBMapper.query(UserPreferencesEntity.class, queryExpression);
	}

	/**
	 * Saves the user preference in DynamoDB.
	 *
	 * @param userPreferences The user preference.
	 */
	private void saveUserPreferences(UserPreferences userPreferences) {
		RequestContext rc = RequestContext.ensureGet();
		try {
			_dynamoDBMapper.save(_userPreferencesMapper.toEntity(userPreferences, rc.getPod(), rc.getOrg()));
		} catch (Exception e) {
			Map<String, String> tags = new HashMap<>();
			tags.put("pod", rc.getPod());
			tags.put("org", rc.getOrg());
			tags.put("method", e.getStackTrace()[0].getMethodName());
			tags.put("exception", e.getClass().getSimpleName());

			MetricsUtil.getCounter(DYNAMO_DB_REPO_PREFIX + ".userPreferenceSaveFailure", tags).inc();

			_log.error("Failed to save User Preferences", e);

		}
	}

}
