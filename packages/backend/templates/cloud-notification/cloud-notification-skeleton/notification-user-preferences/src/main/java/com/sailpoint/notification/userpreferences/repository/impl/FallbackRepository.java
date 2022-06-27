/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.repository.impl;

import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.idn.RestClientProvider;
import com.sailpoint.mantisclient.BaseRestClient;
import com.sailpoint.mantisclient.HttpResponseException;
import com.sailpoint.mantisclient.exception.baserestclient.BaseRestClientException;
import com.sailpoint.metrics.MetricsUtil;
import com.sailpoint.metrics.annotation.Metered;
import com.sailpoint.notification.api.event.RecipientBuilder;
import com.sailpoint.notification.userpreferences.dto.UserPreferences;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;
import com.sailpoint.notification.userpreferences.repository.impl.dynamodb.DynamoDBRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * FallbackRepository
 */
public class FallbackRepository implements UserPreferencesRepository {

    private static final Log _log = LogFactory.getLog(FallbackRepository.class);
    private static final String FALLBACK_REPO_PREFIX = FallbackRepository.class.getName();


    public static final String MICE_SERVICE_NAME = "MICE";
    public static final String V1_IDENTITY_NAME = "displayName";
    public static final String V1_IDENTITY_EMAIL_ADDRESS = "emailAddress";
    public static final String V1_IDENTITY_ATTRIBUTE_PHONE = "phone";
    public static final String V1_IDENTITY_ATTRIBUTES = "attributes";
    public static final String V1_IDENTITY_ATTRIBUTE_BRAND = "brand";

    /**
     * The main repository from which we will fallback
     */
    private UserPreferencesRepository _userPreferencesRepository;

    /**
     * The rest client provider
     */
    private RestClientProvider _restClientProvider;

    public FallbackRepository(UserPreferencesRepository userPreferencesRepository, RestClientProvider restClientProvider) {
        _userPreferencesRepository = userPreferencesRepository;
        _restClientProvider = restClientProvider;
    }

    @Override
    public UserPreferences findByRecipientId(String recipientId) {

        //Try to find in UserPreferencesRepository
        UserPreferences userPreferences = _userPreferencesRepository.findByRecipientId(recipientId);
        if (userPreferences != null) {
            return userPreferences;
        }

        //Fall back to MICE
        UserPreferences miceUserPreferences;
        miceUserPreferences = getUserPreferencesFromMice(recipientId);
        if (miceUserPreferences != null) {
            _log.info("Fallback successful for " + recipientId);
            _userPreferencesRepository.create(miceUserPreferences);
            return miceUserPreferences;
        }

        return null;
    }

    @Override
    public List<UserPreferences> findAllByTenant(String tenant) {
        return _userPreferencesRepository.findAllByTenant(tenant);
    }

    @Override
    public void create(UserPreferences userPreferences) {
        _userPreferencesRepository.create(userPreferences);
    }

    @Override
    public void deleteByRecipientId(String recipientId) {
        _userPreferencesRepository.deleteByRecipientId(recipientId);
    }

    @Override
    public void deleteByTenant(String tenant) {
        _userPreferencesRepository.deleteByTenant(tenant);
    }

    @Metered
    private UserPreferences getUserPreferencesFromMice(String recipientId) {
        try {
            BaseRestClient client = _restClientProvider.getInternalRestClient(MICE_SERVICE_NAME);
            Map<String, Object> response = client.getJson(Map.class, "/api/v1/identities/" + recipientId);

            if (response.get(V1_IDENTITY_EMAIL_ADDRESS) != null) {
                return new UserPreferences.UserPreferencesBuilder()
                        .withRecipient(
                                new RecipientBuilder()
                                        .withId(recipientId)
                                        .withName((String) response.get(V1_IDENTITY_NAME))
                                        .withEmail((String) response.get(V1_IDENTITY_EMAIL_ADDRESS))
                                        .withPhone(getStringFromAttributes((Map<String, Object>) response.get(V1_IDENTITY_ATTRIBUTES), V1_IDENTITY_ATTRIBUTE_PHONE))
                                        .build())
                        .withBrand(Optional.ofNullable(getStringFromAttributes((Map<String, Object>) response.get(V1_IDENTITY_ATTRIBUTES), V1_IDENTITY_ATTRIBUTE_BRAND)))
                        .build();
            }
        } catch (HttpResponseException | BaseRestClientException e) {
            _log.warn("Could not get Identity from MICE.", e);
            RequestContext rc = RequestContext.ensureGet();

            Map<String, String> tags = new HashMap<>();
            tags.put("pod", rc.getPod());
            tags.put("org", rc.getOrg());
            tags.put("exception", e.getClass().getSimpleName());

            if( e instanceof  HttpResponseException)
            {
                Integer statusCode = ((HttpResponseException) e).getStatusCode();
                tags.put("statusCode", statusCode.toString() );
            }
            MetricsUtil.getCounter(FALLBACK_REPO_PREFIX + "_userPreferenceRetrievalFromMice_failure", tags).inc();

        }
        catch (Exception e) {
            _log.error("Error getting Identity from MICE.", e);
        }
        return null;
    }

    private String getStringFromAttributes(Map<String, Object> attributes, String key) {
        if (attributes != null && attributes.containsKey(key)) {
            return (String) attributes.get(key);
        }
        return null;
    }
}
