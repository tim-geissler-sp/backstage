/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.context.common.repository.impl.dynamodb;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.notification.sender.common.exception.persistence.StaleElementException;
import com.sailpoint.notification.context.common.model.GlobalContext;
import com.sailpoint.notification.context.common.model.GlobalContextEntity;
import com.sailpoint.notification.context.common.repository.GlobalContextRepository;
import com.sailpoint.notification.context.common.util.GlobalContextMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sailpoint.notification.context.common.util.GlobalContextMapper.toEntity;
import static java.util.Objects.requireNonNull;

/**
 * DynamoDB implementation of GlobalContextRepository
 */
@Singleton
public class DynamoDBGlobalContextRepository implements GlobalContextRepository {

	private static final Log _log = LogFactory.getLog(DynamoDBGlobalContextRepository.class);

	private final DynamoDBMapper _dynamoDBMapper;

	@Inject
	public DynamoDBGlobalContextRepository(DynamoDBMapper dynamoDBMapper) {
		_dynamoDBMapper = dynamoDBMapper;
	}

	/**
	 *  {@inheritDoc}
	 */
	@Override
	public Optional<GlobalContext> findOneByTenant(String tenant) {

		_log.info("Retrieving GlobalContext for tenant " + tenant);
		return  findByTenant(tenant)
				.map(GlobalContextMapper::toDtoGlobalContext)
				.findFirst();
	}

	/**
	 *  {@inheritDoc}
	 */
	@Override
	public List<GlobalContext> findAllByTenant(String tenant) {
		return findByTenant(tenant)
				.map(GlobalContextMapper::toDtoGlobalContext)
				.collect(Collectors.toList());
	}

	/**
	 *  {@inheritDoc}
	 */
	@Override
	public void save(GlobalContext globalContext) throws StaleElementException {
		requireNonNull(globalContext, "GlobalContext must not be null.");
		requireNonNull(globalContext.getTenant(), "Tenant in GlobalContext must not be null");

		Optional<GlobalContextEntity> globalContextEntity = findByTenant(globalContext.getTenant()).findFirst();
		globalContextEntity.ifPresent(g -> globalContext.setId(g.getId()));

		try {
			_dynamoDBMapper.save(toEntity(globalContext));
			_log.info("GlobalContext for tenant " + globalContext.getTenant() + " has been saved.");
		} catch (ConditionalCheckFailedException ccfe) {
			throw new StaleElementException(ccfe);
		}
	}

	/**
	 *  {@inheritDoc}
	 */
	@Override
	public boolean deleteByTenant(String tenant) throws StaleElementException {
		Optional<GlobalContextEntity> globalContextEntity = findByTenant(tenant).findFirst();

		if (globalContextEntity.isPresent()) {
			try {
				_dynamoDBMapper.delete(globalContextEntity.get());
			_log.info("GlobalContext for tenant " + tenant + " has been deleted.");
			} catch (ConditionalCheckFailedException ccfe) {
				throw new StaleElementException(ccfe);
			}
			return true;
		} else {
			_log.info("Unable to delete GlobalContext for tenant " + tenant);
			return false;
		}
	}

	private Stream<GlobalContextEntity> findByTenant(String tenant) {
		// Entity used to perform the query by HashKey 'tenant'.
		GlobalContextEntity globalContextEntity = new GlobalContextEntity();
		globalContextEntity.setTenant(tenant);

		DynamoDBQueryExpression<GlobalContextEntity> queryExpression = new DynamoDBQueryExpression<>();
		queryExpression.withHashKeyValues(globalContextEntity);

		return  _dynamoDBMapper.query(GlobalContextEntity.class, queryExpression).stream();
	}
}
