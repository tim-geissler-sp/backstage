/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.repository.impl;

import com.sailpoint.notification.api.event.RecipientBuilder;
import com.sailpoint.notification.api.event.dto.Recipient;
import com.sailpoint.notification.userpreferences.dto.UserPreferences;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CachedRepositoryTest
 */
public class CachedRepositoryTest {

    @Mock
    UserPreferencesRepository _userPreferencesRepository;

    CachedRepository _cachedRepository;

    long _currentTime;

    private final String RECIPIENT_ID = "70e7cde5-3473-46ea-94ea-90bc8c605a6c";

    private final Recipient mockRecipient = new RecipientBuilder()
            .withId(RECIPIENT_ID)
            .withEmail("aaron@sailpoint.com")
            .withName("Aaron Nichols")
            .withPhone("111-111-1111")
            .build();

    private final UserPreferences mockPreferences = new UserPreferences.UserPreferencesBuilder()
            .withRecipient(mockRecipient)
            .withBrand(Optional.of("brandX"))
            .build();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        _cachedRepository = new CachedRepository(_userPreferencesRepository);
        _cachedRepository._currentTimeSupplier = () -> _currentTime;

        Mockito.doAnswer(answer -> mockPreferences)
                .when(_userPreferencesRepository).findByRecipientId(anyString());
    }

    @Test
    public void emptyCache() {
        _cachedRepository.findByRecipientId(RECIPIENT_ID);
        Mockito.verify(_userPreferencesRepository).findByRecipientId(RECIPIENT_ID);
    }

    @Test
    public void subsequentHitsDoNotCallWrappedProvider() {
        for (int i = 0; i < 10; ++i) {
            _cachedRepository.findByRecipientId(RECIPIENT_ID);
        }

        Mockito.verify(_userPreferencesRepository, Mockito.times(1)).findByRecipientId(RECIPIENT_ID);
    }

    @Test
    public void eachHitReturnsTheSameResult() {
        UserPreferences userPreferences = _cachedRepository.findByRecipientId(RECIPIENT_ID);

        for (int i = 0; i < 10; ++i) {
            Assert.assertSame(userPreferences, _cachedRepository.findByRecipientId(RECIPIENT_ID));
        }
    }

    @Test
    public void refreshStaleDataChangedUserPreferences() {
        _currentTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);
        _cachedRepository.findByRecipientId(RECIPIENT_ID);

        //Before stale
        _currentTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(6);
        _cachedRepository.refreshStaleData();

        UserPreferences beforeStale = _cachedRepository.findByRecipientId(RECIPIENT_ID);
        assertEquals(mockPreferences, beforeStale);

        //After stale
        UserPreferences changedPreferences = new UserPreferences.UserPreferencesBuilder()
                .withRecipient(mockRecipient.derive()
                        .withEmail("nichols@sailpoint.com")
                        .build())
                .withBrand(Optional.of("brandY"))
                .build();
        when(_userPreferencesRepository.findByRecipientId(RECIPIENT_ID)).thenReturn(changedPreferences);

        _currentTime = System.currentTimeMillis();
        _cachedRepository.refreshStaleData();

        UserPreferences afterStale = _cachedRepository.findByRecipientId(RECIPIENT_ID);
        assertEquals(changedPreferences, afterStale);

        //Get from cache again to assert same
        UserPreferences fromCacheAfterStale = _cachedRepository.findByRecipientId(RECIPIENT_ID);
        Assert.assertSame(afterStale, fromCacheAfterStale);
    }

    @Test
    public void refreshStaleDataRemovedUserPreferences() {
        _currentTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);
        _cachedRepository.findByRecipientId(RECIPIENT_ID);

        //Before stale
        _currentTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(6);
        _cachedRepository.refreshStaleData();
        assertNotNull(_cachedRepository.findByRecipientId(RECIPIENT_ID));

        //After stale
        when(_userPreferencesRepository.findByRecipientId(RECIPIENT_ID)).thenReturn(null);

        _currentTime = System.currentTimeMillis();
        _cachedRepository.refreshStaleData();
        assertNull(_cachedRepository.findByRecipientId(RECIPIENT_ID));
    }

    @Test
    public void testFindAllByTenant() {
        _cachedRepository.findAllByTenant("acme-solar");

        verify(_userPreferencesRepository, times(1)).findAllByTenant(anyString());
    }

    @Test
    public void testCreate() {
        _cachedRepository.create(new UserPreferences.UserPreferencesBuilder().build());

        verify(_userPreferencesRepository, times(1)).create(any());
    }

    @Test
    public void testDeleteByRecipientId() {
        _cachedRepository.deleteByRecipientId(RECIPIENT_ID);

        verify(_userPreferencesRepository, times(1)).deleteByRecipientId(anyString());
    }

    @Test
    public void testDeleteByTenant() {
        _cachedRepository.deleteByTenant("acme-solar");

        verify(_userPreferencesRepository, times(1)).deleteByTenant(anyString());
    }
}
