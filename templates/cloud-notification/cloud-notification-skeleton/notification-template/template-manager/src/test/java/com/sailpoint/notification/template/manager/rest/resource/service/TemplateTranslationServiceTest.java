/*
 * Copyright (C) 2022 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.manager.rest.resource.service;


import com.sailpoint.notification.template.common.model.NotificationTemplate;
import com.sailpoint.notification.template.common.model.TemplateMediumDto;
import com.sailpoint.notification.template.common.model.teams.TeamsTemplate;

import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateDtoDefault;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateDtoMapper;
import com.sailpoint.notification.template.manager.rest.service.TemplateTranslationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Locale;
import static org.junit.Assert.*;


@RunWith(MockitoJUnitRunner.class)
public class TemplateTranslationServiceTest {
    TemplateTranslationService templateTranslationService;

    @Before
    public void setup() throws Exception{
        MockitoAnnotations.initMocks(this);
        templateTranslationService = new TemplateTranslationService();
    }

    @Test
    public void getTemplateTranslationsEn() {
        TemplateDtoDefault templateDtoDefault = mockTemplateDtoDefault();
        String localeString = "en-US";
        TemplateDtoDefault translatedTemplate = templateTranslationService.getDefaultTemplateTranslation(templateDtoDefault, localeString);
        assertEquals("Default Template", translatedTemplate.getName());
        assertEquals("Default template for rendered emails", translatedTemplate.getDescription());
    }

    @Test
    public void getTemplateTranslationsDe() {
        TemplateDtoDefault templateDtoDefault = mockTemplateDtoDefault();
        String localeString = "de-de";
        TemplateDtoDefault translatedTemplate = templateTranslationService.getDefaultTemplateTranslation(templateDtoDefault, localeString);
        assertEquals("Default Template in German", translatedTemplate.getName());
        assertEquals("Default template for rendered emails in German", translatedTemplate.getDescription());
    }

    @Test
    public void getTemplateTranslationsNoLocale() {
        TemplateDtoDefault templateDtoDefault = mockTemplateDtoDefault();
        String localeString = "";
        TemplateDtoDefault translatedTemplate = templateTranslationService.getDefaultTemplateTranslation(templateDtoDefault, localeString);
        assertEquals("Default Template", translatedTemplate.getName());
        assertEquals("Default template for rendered emails", translatedTemplate.getDescription());
    }

    @Test
    public void getTemplateTranslationsUnknownLocale() {
        TemplateDtoDefault templateDtoDefault = mockTemplateDtoDefault();
        String localeString = "test";
        TemplateDtoDefault translatedTemplate = templateTranslationService.getDefaultTemplateTranslation(templateDtoDefault, localeString);
        assertEquals("Default Template", translatedTemplate.getName());
        assertEquals("Default template for rendered emails", translatedTemplate.getDescription());
    }

    @Test
    public void getTemplateTranslationsWithMultipleLocale() {
        TemplateDtoDefault templateDtoDefault = mockTemplateDtoDefault();
        String localeString = "en-CA;q=0.9,en;q=0.8,en-us;q=0.6,de-de;q=1.0,de;q=0.2";
        TemplateDtoDefault translatedTemplate = templateTranslationService.getDefaultTemplateTranslation(templateDtoDefault, localeString);
        assertEquals("Default Template in German", translatedTemplate.getName());
        assertEquals("Default template for rendered emails in German", translatedTemplate.getDescription());
    }


    private TemplateDtoDefault mockTemplateDtoDefault() {
        NotificationTemplate notificationTemplate = NotificationTemplate.newBuilder()
                .id("1234")
                .name("default_template_name")
                .description("default_template_des")
                .key("key")
                .tenant("tenant")
                .medium(TemplateMediumDto.EMAIL.name().toLowerCase())
                .locale(Locale.ENGLISH)
                .teamsTemplate(TeamsTemplate.builder()
                        .messageJson("$domainEvent.get('messageJson')")
                        .text("$domainEvent.get('text')")
                        .title("$domainEvent.get('title')")
                        .build())
                .build();
        return TemplateDtoMapper.toTemplateDTODefault(notificationTemplate);
    }
}
