/*
 * Copyright (C) 2022 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.manager.rest.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.atlas.util.StringUtil;
import com.sailpoint.notification.template.common.repository.TemplateRepositoryDefault;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateDto;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateDtoDefault;
import lombok.extern.apachecommons.CommonsLog;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

// This is the service to read from hermes-template-message property file
// and do the translation based on Accepted_Language from API call

@Singleton
@CommonsLog
public class TemplateTranslationService {
    @Inject
    private TemplateRepositoryDefault _templateRepositoryDefault;

    public static final String BUNDLE_NAME = "hermes-template-messages";
    public static final String BUNDLE_PREFIX = "hermes-template.";

    //Get translation for default email templates
    public TemplateDtoDefault getDefaultTemplateTranslation(TemplateDtoDefault templateDtoDefault, String localeString) {
        Locale locale = getLocale(localeString);
        //If bundle not found with targeted locale, return default bundle(hermes-template-messages)
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
        templateDtoDefault.setName(getFromBundle(bundle, BUNDLE_PREFIX + templateDtoDefault.getName()));
        templateDtoDefault.setDescription(getFromBundle(bundle, BUNDLE_PREFIX + templateDtoDefault.getDescription()));
        return templateDtoDefault;
    }


    //Get translation for custom email templates
    public TemplateDto getCustomTemplateTranslation(TemplateDto templateDto, String localeString) {
        Locale locale = getLocale(localeString);
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);

        String templateName = _templateRepositoryDefault.findOneByKey(templateDto.getKey()).getName();
        String templateDes = _templateRepositoryDefault.findOneByKey(templateDto.getKey()).getDescription();
        templateDto.setName(getFromBundle(bundle, BUNDLE_PREFIX + templateName));
        templateDto.setDescription(getFromBundle(bundle, BUNDLE_PREFIX + templateDes));
        return templateDto;
    }


    private String getFromBundle(ResourceBundle bundle, String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            log.warn(String.format("Failed to retrieve key '%s' from bundle for locale '%s'", key, bundle.getLocale()), e);
            return null;
        }
    }

    private Locale getLocale(String localeString) {
        // If no Accept-Language in the header, use the default locale en.
        if (StringUtil.isNullOrEmpty(localeString)) {
            return Locale.getDefault();
        }
        List<Locale.LanguageRange> localesByPriority = Locale.LanguageRange.parse(localeString);
        List<Locale> availableLocals = Arrays.asList(Locale.getAvailableLocales());
        Locale locale = Locale.lookup(localesByPriority, availableLocals);
        return (locale == null ? Locale.getDefault() : locale);
    }
}
