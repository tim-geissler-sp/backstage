/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.repository.impl;

import com.sailpoint.notification.userpreferences.dto.UserPreferences;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * CachedRepository implements an LRU cache, and is based on the CachedOrgDataProvider implementation.
 */
public class CachedRepository implements UserPreferencesRepository {

    private static final Log _log = LogFactory.getLog(CachedRepository.class);

    /**
     * The maximum amount of time (in minutes) that UserPreferences remains in the cache.
     */
    private static final long MAX_CACHE_TIME_MINUTES = 5;

    /**
     * The repository to cache
     */
    private UserPreferencesRepository _userPreferencesRepository;

    /**
     * LRU map
     */
    private Map<String, CachedUserPreferences> _userPreferencesCache = new HashMap<>();

    /**
     * Used to schedule the periodic stale job checker.
     */
    private ScheduledExecutorService _scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    /**
     * Supplier to get the current time.
     */
    Supplier<Long> _currentTimeSupplier = () -> System.currentTimeMillis();

    public CachedRepository(UserPreferencesRepository userPreferencesRepository) {
        _userPreferencesRepository = userPreferencesRepository;

        _scheduledExecutorService.scheduleWithFixedDelay(this::refreshStaleData, MAX_CACHE_TIME_MINUTES,
                MAX_CACHE_TIME_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Refreshing the stale data.
     */
    void refreshStaleData() {
        _log.debug("refreshing stale org data!");

        List<CachedUserPreferences> changed = new ArrayList<>();
        List<CachedUserPreferences> removed = new ArrayList<>();

        Map<String, CachedUserPreferences> cacheCopy = new HashMap<>();
        synchronized (this) {
            cacheCopy.putAll(_userPreferencesCache);
        }

        cacheCopy.forEach((key, value) -> {
            if (value.isStale()) {
                UserPreferences oldUserPreference = value.getUserPreferences();

                UserPreferences newUserPreferences = _userPreferencesRepository.findByRecipientId(oldUserPreference.getRecipient().getId());

                if (newUserPreferences == null) {
                    removed.add(value);
                } else {
                    if (!Objects.equals(oldUserPreference, newUserPreferences)) {
                        changed.add(new CachedUserPreferences(newUserPreferences, _currentTimeSupplier));
                    }
                }
            }
        });

        synchronized (this) {
            changed.forEach(newData -> {
                _userPreferencesCache.put(newData.getUserPreferences().getRecipient().getId(), newData);
            });

            removed.forEach(oldData -> {
                _userPreferencesCache.remove(oldData.getUserPreferences().getRecipient().getId());
            });
        }
    }

    @Override
    public UserPreferences findByRecipientId(String recipientId) {
        CachedUserPreferences cachedUserPreferences = _userPreferencesCache.computeIfAbsent(recipientId, id ->
                new CachedUserPreferences(_userPreferencesRepository.findByRecipientId(id), _currentTimeSupplier)
        );

        return cachedUserPreferences.getUserPreferences();
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

    /**
     * Holds onto UserPreferences and a created timestamp that can be used
     * to invalidate the UserPreferences after a set amount of time.
     */
    static class CachedUserPreferences {

        private Supplier<Long> _currentTimeSupplier;

        private long _createdTimestamp;

        private UserPreferences _userPreferences;

        /**
         * Constructs a new UserPreferences instance, marking the current time as the cache start.
         * @param userPreferences The user preferences.
         * @param currentTimeSupplier The supplier for the current time.
         */
        public CachedUserPreferences(UserPreferences userPreferences, Supplier<Long> currentTimeSupplier) {
            _userPreferences = userPreferences;
            _currentTimeSupplier = currentTimeSupplier;
            _createdTimestamp = _currentTimeSupplier.get();
        }

        /**
         * Gets the associated UserPreferences.
         * @return The data.
         */
        public UserPreferences getUserPreferences() {
            return _userPreferences;
        }

        /**
         * Gets whether or not the cached data is stale.
         * @return True if the data is stale, false otherwise.
         */
        public boolean isStale() {
            long currentTime = _currentTimeSupplier.get();

            long dt = currentTime - _createdTimestamp;
            return dt > TimeUnit.MINUTES.toMillis(MAX_CACHE_TIME_MINUTES);
        }

    }
}
