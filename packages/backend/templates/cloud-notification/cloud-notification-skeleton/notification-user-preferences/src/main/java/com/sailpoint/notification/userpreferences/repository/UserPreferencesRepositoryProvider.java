/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sailpoint.atlas.idn.RestClientProvider;
import com.sailpoint.notification.userpreferences.mapper.UserPreferencesMapper;
import com.sailpoint.notification.userpreferences.repository.impl.FallbackRepository;
import com.sailpoint.notification.userpreferences.repository.impl.dynamodb.DynamoDBRepository;

/**
 * CachedUserPreferencesRepositoryProvider
 */
public class UserPreferencesRepositoryProvider implements Provider<UserPreferencesRepository> {

    DynamoDBMapper _dynamoDBMapper;
    UserPreferencesMapper _userPreferencesMapper;
    RestClientProvider _restClientProvider;

    @Inject
    public UserPreferencesRepositoryProvider(DynamoDBMapper dynamoDBMapper, UserPreferencesMapper userPreferencesMapper, RestClientProvider restClientProvider) {
        _dynamoDBMapper = dynamoDBMapper;
        _userPreferencesMapper = userPreferencesMapper;
        _restClientProvider = restClientProvider;
    }

    @Override
    public UserPreferencesRepository get() {
        return new FallbackRepository(new DynamoDBRepository(_dynamoDBMapper, _userPreferencesMapper), _restClientProvider);
    }
}
