/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.util;

import com.sailpoint.atlas.util.JsonPathUtil;
import com.sailpoint.notification.api.event.RecipientBuilder;
import com.sailpoint.notification.api.event.dto.Recipient;
import com.sailpoint.notification.userpreferences.dto.UserPreferences;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;

/**
 * TemplateUtil
 */
public class TemplateUtil {

    public static final String DEFAULT_VALUE = "Unknown";
    private UserPreferencesRepository _userPreferencesRepository;

    public TemplateUtil(UserPreferencesRepository userPreferencesRepository) {
        _userPreferencesRepository = userPreferencesRepository;
    }

    /**
     * Gets a User from the UserPreferencesRepository.
     * @param id Same as the CIS externalId of the user.
     * @return The Recipient.
     */
    public Recipient getUser(String id) {
        UserPreferences userPreferences = _userPreferencesRepository.findByRecipientId(id);
        if (userPreferences != null && userPreferences.getRecipient() != null) {
            return userPreferences.getRecipient();
        }

        return new RecipientBuilder()
                .withId(id)
                .withName(DEFAULT_VALUE)
                .withEmail(DEFAULT_VALUE)
                .withPhone(DEFAULT_VALUE)
                .build();
    }

    /**
     * Get a Java object by JsonPath that applies to the provided Json string
     *
     * @param json a json string
     * @param path the json path
     * @return Java Object which can be ArrayList if it is a JsonArray and LinkedHashMap if it is a JsonObject
     * and VTL will auto unbox when interpret it.
     */
    public Object getObjectByJsonPath(String json, String path) {
        return JsonPathUtil.getObjectByPath(json, path);
    }
}
