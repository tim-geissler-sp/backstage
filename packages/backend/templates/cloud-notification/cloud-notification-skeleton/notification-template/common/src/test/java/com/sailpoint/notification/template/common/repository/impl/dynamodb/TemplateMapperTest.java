/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.repository.impl.dynamodb;

import com.sailpoint.notification.api.event.dto.NotificationMedium;
import com.sailpoint.notification.api.event.dto.SlackNotificationAutoApprovalData;
import com.sailpoint.notification.api.event.dto.TeamsNotificationAutoApprovalData;
import com.sailpoint.notification.template.common.model.NotificationTemplate;
import com.sailpoint.notification.template.common.model.teams.TeamsTemplate;
import com.sailpoint.notification.template.common.model.slack.SlackTemplate;
import com.sailpoint.notification.template.common.model.version.TemplateVersionInfo;
import com.sailpoint.notification.template.common.repository.impl.dynamodb.entity.TemplateMapper;
import com.sailpoint.notification.template.common.repository.impl.dynamodb.entity.TemplatePersistentEntity;
import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

public class TemplateMapperTest {

    private final TemplateMapper mapper = new TemplateMapper();

    @Test
    public void testToEntityWithTeamsMediumTemplate() {
        String version = "1A";
        NotificationTemplate template = getTestTemplate(NotificationMedium.TEAMS.name());

        TemplateVersionInfo versionInfo = new TemplateVersionInfo();
        versionInfo.setNote("fake note");

        TemplatePersistentEntity entity = TemplateMapper.toEntity(version, template, versionInfo);

        Assert.assertEquals(template.getName(), entity.getName());
        Assert.assertEquals(template.getKey(), entity.getKey());
        Assert.assertEquals(template.getTenant(), entity.getTenant());
        Assert.assertEquals(template.getMedium(), entity.getMedium());
        Assert.assertEquals(template.getDescription(), entity.getDescription());
        Assert.assertEquals(template.getSubject(), entity.getSubject());
        Assert.assertEquals(template.getBody(), entity.getBody());
        Assert.assertEquals(template.getHeader(), entity.getHeader());
        Assert.assertEquals(template.getFooter(), entity.getFooter());
        Assert.assertEquals(template.getReplyTo(), entity.getReplyTo());
        Assert.assertEquals(template.getFrom(), entity.getFrom());
        Assert.assertEquals(template.getLocale().toLanguageTag(), entity.getLocale());

        Assert.assertNull(entity.getSlackTemplate());

        Assert.assertEquals(template.getTeamsTemplate().getTitle(), entity.getTeamsTemplate().getTitle());
        Assert.assertEquals(template.getTeamsTemplate().getMessageJson(), entity.getTeamsTemplate().getMessageJson());
        Assert.assertEquals(template.getTeamsTemplate().getText(), entity.getTeamsTemplate().getText());

        Assert.assertEquals(TemplateMapper.DEFAULT_TEMPLATE_ENGINE, entity.getEngine());

    }

    @Test
    public void testToEntityWithSlackMediumTemplate() {
        String version = "1A";
        NotificationTemplate template = getTestTemplate(NotificationMedium.SLACK.name());

        TemplateVersionInfo versionInfo = new TemplateVersionInfo();
        versionInfo.setNote("fake note");

        TemplatePersistentEntity entity = TemplateMapper.toEntity(version, template, versionInfo);

        Assert.assertEquals(template.getName(), entity.getName());
        Assert.assertEquals(template.getKey(), entity.getKey());
        Assert.assertEquals(template.getTenant(), entity.getTenant());
        Assert.assertEquals(template.getMedium(), entity.getMedium());
        Assert.assertEquals(template.getDescription(), entity.getDescription());
        Assert.assertEquals(template.getSubject(), entity.getSubject());
        Assert.assertEquals(template.getBody(), entity.getBody());
        Assert.assertEquals(template.getHeader(), entity.getHeader());
        Assert.assertEquals(template.getFooter(), entity.getFooter());
        Assert.assertEquals(template.getReplyTo(), entity.getReplyTo());
        Assert.assertEquals(template.getFrom(), entity.getFrom());
        Assert.assertEquals(template.getLocale().toLanguageTag(), entity.getLocale());

        Assert.assertNull(entity.getTeamsTemplate());

        Assert.assertEquals(template.getSlackTemplate().getBlocks(), entity.getSlackTemplate().getBlocks());
        Assert.assertEquals(template.getSlackTemplate().getAttachments(), entity.getSlackTemplate().getAttachments());
        Assert.assertEquals(template.getSlackTemplate().getText(), entity.getSlackTemplate().getText());

        Assert.assertEquals(TemplateMapper.DEFAULT_TEMPLATE_ENGINE, entity.getEngine());

    }


    private NotificationTemplate getTestTemplate(String medium)
    {
        return NotificationTemplate
                .newBuilder()
                .name("name")
                .key("KEY_1")
                .tenant("testTenant")
                .medium(medium)
                .description("description")
                .subject("subject")
                .body("body")
                .header("header")
                .footer("footer")
                .replyTo("replyTo")
                .from("from")
                .locale(Locale.ENGLISH)
                .teamsTemplate(TeamsTemplate.builder()
                        .title("my title")
                        .text("my text")
                        .messageJson("json here")
                        .autoApprovalData(TeamsNotificationAutoApprovalData.builder()
                                .itemId("my itemId")
                                .itemType("my itemType")
                                .autoApprovalMessageJSON("json here")
                                .autoApprovalTitle("my title")
                                .build())
                        .build())
                .slackTemplate(SlackTemplate.builder()
                        .blocks("blocks")
                        .text("my text")
                        .attachments("attach it")
                        .autoApprovalData(SlackNotificationAutoApprovalData.builder()
                                .itemId("my itemId")
                                .itemType("my itemType")
                                .autoApprovalMessageJSON("json here")
                                .autoApprovalTitle("my title")
                                .build())
                        .build())
                .build();
    }

}
