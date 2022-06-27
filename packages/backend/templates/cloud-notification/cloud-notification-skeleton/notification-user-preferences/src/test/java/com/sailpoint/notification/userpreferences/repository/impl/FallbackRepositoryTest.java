/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.repository.impl;

import com.google.common.collect.ImmutableMap;
import com.sailpoint.atlas.idn.RestClientProvider;
import com.sailpoint.mantisclient.BaseRestClient;
import com.sailpoint.notification.userpreferences.dto.UserPreferences;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static com.sailpoint.notification.userpreferences.repository.impl.FallbackRepository.V1_IDENTITY_ATTRIBUTES;
import static com.sailpoint.notification.userpreferences.repository.impl.FallbackRepository.V1_IDENTITY_ATTRIBUTE_BRAND;
import static com.sailpoint.notification.userpreferences.repository.impl.FallbackRepository.V1_IDENTITY_ATTRIBUTE_PHONE;
import static com.sailpoint.notification.userpreferences.repository.impl.FallbackRepository.V1_IDENTITY_EMAIL_ADDRESS;
import static com.sailpoint.notification.userpreferences.repository.impl.FallbackRepository.V1_IDENTITY_NAME;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FallbackRepositoryTest
 */
public class FallbackRepositoryTest {

    @Mock
    RestClientProvider _restClientProvider;

    @Mock
    UserPreferencesRepository _userPreferencesRepository;

    @Mock
    BaseRestClient _baseRestClient;

    private FallbackRepository _fallbackRepository;

    private final String RECIPIENT_ID = "70e7cde5-3473-46ea-94ea-90bc8c605a6c";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(_restClientProvider.getInternalRestClient(any()))
                .thenReturn(_baseRestClient);

        _fallbackRepository = new FallbackRepository(_userPreferencesRepository, _restClientProvider);
    }

    private final Map<String, Object> mockMiceResponse() {
        Map<String, Object> attributesMap = ImmutableMap.of(
                V1_IDENTITY_ATTRIBUTE_BRAND, "brandX",
                V1_IDENTITY_ATTRIBUTE_PHONE, "111-111-1111"
        );

        return ImmutableMap.of(
                "id", RECIPIENT_ID,
                V1_IDENTITY_NAME, "Aaron Nichols",
                V1_IDENTITY_EMAIL_ADDRESS, "aaron@sailpoint.com",
                V1_IDENTITY_ATTRIBUTES, attributesMap
        );
    }

    @Test
    public void testWithoutFallback() {
        when(_userPreferencesRepository.findByRecipientId(any()))
                .thenReturn(new UserPreferences.UserPreferencesBuilder().build());

        _fallbackRepository.findByRecipientId(RECIPIENT_ID);
        verify(_baseRestClient, never()).getJson(any(Class.class), anyString());
        verify(_userPreferencesRepository, times(1)).findByRecipientId(anyString());
    }

    @Test
    public void testFallback() {
        when(_userPreferencesRepository.findByRecipientId(any()))
                .thenReturn(null);
        when(_baseRestClient.getJson(any(), any()))
                .thenReturn(mockMiceResponse());

        UserPreferences userPreferences = _fallbackRepository.findByRecipientId(RECIPIENT_ID);
        verify(_baseRestClient, times(1)).getJson(any(Class.class), anyString());
        verify(_userPreferencesRepository, times(1)).findByRecipientId(anyString());

        assertEquals(RECIPIENT_ID, userPreferences.getRecipient().getId());
        assertEquals("Aaron Nichols", userPreferences.getRecipient().getName());
        assertEquals("aaron@sailpoint.com", userPreferences.getRecipient().getEmail());
        assertEquals("111-111-1111", userPreferences.getRecipient().getPhone());
        assertEquals("brandX", userPreferences.getBrand().get());
    }

    @Test
    public void testFindAllByTenant() {
        _fallbackRepository.findAllByTenant("acme-solar");

        verify(_userPreferencesRepository, times(1)).findAllByTenant(anyString());
    }

    @Test
    public void testCreate() {
        _fallbackRepository.create(new UserPreferences.UserPreferencesBuilder().build());

        verify(_userPreferencesRepository, times(1)).create(any());
    }

    @Test
    public void testDeleteByRecipientId() {
        _fallbackRepository.deleteByRecipientId(RECIPIENT_ID);

        verify(_userPreferencesRepository, times(1)).deleteByRecipientId(anyString());
    }

    @Test
    public void testDeleteByTenant() {
        _fallbackRepository.deleteByTenant("acme-solar");

        verify(_userPreferencesRepository, times(1)).deleteByTenant(anyString());
    }
}
