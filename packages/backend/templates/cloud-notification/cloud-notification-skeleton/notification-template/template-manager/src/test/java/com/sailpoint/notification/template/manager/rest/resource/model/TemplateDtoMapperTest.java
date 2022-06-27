/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.manager.rest.resource.model;

import com.sailpoint.notification.template.common.model.NotificationTemplate;
import com.sailpoint.notification.template.common.model.teams.TeamsTemplate;
import com.sailpoint.notification.template.common.model.slack.SlackTemplate;
import com.sailpoint.notification.template.common.model.version.TemplateVersionInfo;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateDto;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateDtoDefault;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateDtoMapper;
import com.sailpoint.notification.template.common.model.TemplateMediumDto;
import com.sailpoint.notification.template.common.model.version.TemplateVersion;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateVersionDto;
import com.sailpoint.notification.template.manager.rest.resouce.model.TemplateVersionUserInfoDto;
import org.junit.Assert;
import org.junit.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

/**
 * Test TemplateDtoMapper
 */
public class TemplateDtoMapperTest {

	@Test
	public void templateDtoMapperTest() {

		NotificationTemplate notificationTemplate = NotificationTemplate.newBuilder()
				.id("1234")
				.name("name")
				.key("key")
				.tenant("tenant")
				.medium(TemplateMediumDto.SMS.name().toLowerCase())
				.description("description")
				.subject("subject")
				.body("body")
				.header("header")
				.footer("footer")
				.replyTo("replyTo")
				.from("from")
				.locale(Locale.ENGLISH)
				.build();

		TemplateDtoDefault defaultTemplate = TemplateDtoMapper.toTemplateDTODefault(notificationTemplate);

		Assert.assertEquals(notificationTemplate.getKey(), defaultTemplate.getKey());
		Assert.assertEquals(notificationTemplate.getName(), defaultTemplate.getName());
		Assert.assertEquals(notificationTemplate.getMedium(), defaultTemplate.getMedium().name());
		Assert.assertEquals(notificationTemplate.getLocale(), defaultTemplate.getLocale());
		Assert.assertEquals(notificationTemplate.getDescription(), defaultTemplate.getDescription());
		Assert.assertEquals(notificationTemplate.getSubject(), defaultTemplate.getSubject());
		Assert.assertEquals(notificationTemplate.getBody(), defaultTemplate.getBody());
		Assert.assertEquals(notificationTemplate.getHeader(), defaultTemplate.getHeader());
		Assert.assertEquals(notificationTemplate.getFooter(), defaultTemplate.getFooter());
		Assert.assertEquals(notificationTemplate.getReplyTo(), defaultTemplate.getReplyTo());
		Assert.assertEquals(notificationTemplate.getFrom(), defaultTemplate.getFrom());
		Assert.assertNull(defaultTemplate.getSlackTemplate());

		TemplateDto template = TemplateDtoMapper.toTemplateDTO(notificationTemplate);
		Assert.assertEquals(notificationTemplate.getId(), template.getId());
		Assert.assertNull(template.getSlackTemplate());

		NotificationTemplate notificationTemplateResult = TemplateDtoMapper.toNotificationTemplate("acme-solar",
				defaultTemplate);
		Assert.assertEquals(notificationTemplate.getKey(), notificationTemplateResult.getKey());
		Assert.assertEquals(notificationTemplate.getName(), notificationTemplateResult.getName());
		Assert.assertNull(notificationTemplateResult.getSlackTemplate());

		TemplateVersionDto versionInfoDto = new TemplateVersionDto();
		versionInfoDto.setCreated(OffsetDateTime.now(ZoneOffset.UTC));
		versionInfoDto.setCreatedBy(new TemplateVersionUserInfoDto());
		versionInfoDto.setNote("test version");
		versionInfoDto.setVersion("V0");

		TemplateVersionInfo versionInfo = TemplateDtoMapper.toTemplateVersionInfo(versionInfoDto);
		Assert.assertEquals(versionInfoDto.getCreated(), versionInfo.getDate());
		Assert.assertEquals(versionInfoDto.getNote(), versionInfo.getNote());
		Assert.assertEquals(versionInfoDto.getCreatedBy().getName(), versionInfo.getUpdatedBy().getName());

	}

	@Test
	public void templateDtoMapperTestSlack() {
		NotificationTemplate notificationTemplate = NotificationTemplate.newBuilder()
				.id("1234")
				.name("name")
				.key("key")
				.tenant("tenant")
				.medium(TemplateMediumDto.SLACK.name().toLowerCase())
				.locale(Locale.ENGLISH)
				.slackTemplate(SlackTemplate.builder()
						.attachments("$domainEvent.get('attachments')")
						.blocks("$domainEvent.get('blocks')")
						.text("$domainEvent.get('text')")
						.build())
				.build();

		TemplateDtoDefault defaultTemplate = TemplateDtoMapper.toTemplateDTODefault(notificationTemplate);

		Assert.assertEquals(notificationTemplate.getKey(), defaultTemplate.getKey());
		Assert.assertEquals(notificationTemplate.getName(), defaultTemplate.getName());
		Assert.assertEquals(notificationTemplate.getMedium(), defaultTemplate.getMedium().name());
		Assert.assertEquals(notificationTemplate.getLocale(), defaultTemplate.getLocale());
		Assert.assertEquals(notificationTemplate.getDescription(), defaultTemplate.getDescription());
		Assert.assertEquals(notificationTemplate.getSubject(), defaultTemplate.getSubject());
		Assert.assertEquals(notificationTemplate.getBody(), defaultTemplate.getBody());
		Assert.assertEquals(notificationTemplate.getHeader(), defaultTemplate.getHeader());
		Assert.assertEquals(notificationTemplate.getFooter(), defaultTemplate.getFooter());
		Assert.assertEquals(notificationTemplate.getReplyTo(), defaultTemplate.getReplyTo());
		Assert.assertEquals(notificationTemplate.getFrom(), defaultTemplate.getFrom());

		Assert.assertNotNull(defaultTemplate.getSlackTemplate());
		Assert.assertEquals(notificationTemplate.getSlackTemplate().getAttachments(),
				defaultTemplate.getSlackTemplate().getAttachments());
		Assert.assertEquals(notificationTemplate.getSlackTemplate().getBlocks(),
				defaultTemplate.getSlackTemplate().getBlocks());
		Assert.assertEquals(notificationTemplate.getSlackTemplate().getText(),
				defaultTemplate.getSlackTemplate().getText());

		TemplateDto template = TemplateDtoMapper.toTemplateDTO(notificationTemplate);
		Assert.assertEquals(notificationTemplate.getId(), template.getId());
		Assert.assertNotNull(defaultTemplate.getSlackTemplate());
		Assert.assertEquals(notificationTemplate.getSlackTemplate().getAttachments(),
				template.getSlackTemplate().getAttachments());
		Assert.assertEquals(notificationTemplate.getSlackTemplate().getBlocks(),
				template.getSlackTemplate().getBlocks());
		Assert.assertEquals(notificationTemplate.getSlackTemplate().getText(),
				template.getSlackTemplate().getText());


		NotificationTemplate notificationTemplateResult = TemplateDtoMapper.toNotificationTemplate("acme-solar",
				defaultTemplate);
		Assert.assertEquals(notificationTemplate.getKey(), notificationTemplateResult.getKey());
		Assert.assertEquals(notificationTemplate.getName(), notificationTemplateResult.getName());
		Assert.assertNotNull(defaultTemplate.getSlackTemplate());
		Assert.assertEquals(notificationTemplate.getSlackTemplate().getAttachments(),
				notificationTemplateResult.getSlackTemplate().getAttachments());
		Assert.assertEquals(notificationTemplate.getSlackTemplate().getBlocks(),
				notificationTemplateResult.getSlackTemplate().getBlocks());
		Assert.assertEquals(notificationTemplate.getSlackTemplate().getText(),
				notificationTemplateResult.getSlackTemplate().getText());

	}

	@Test
	public void templateDtoMapperTestTeams() {
		NotificationTemplate notificationTemplate = NotificationTemplate.newBuilder()
				.id("1234")
				.name("name")
				.key("key")
				.tenant("tenant")
				.medium(TemplateMediumDto.TEAMS.name().toLowerCase())
				.locale(Locale.ENGLISH)
				.teamsTemplate(TeamsTemplate.builder()
						.messageJson("$domainEvent.get('messageJson')")
						.text("$domainEvent.get('text')")
						.title("$domainEvent.get('title')")
						.build())
				.build();

		TemplateDtoDefault defaultTemplate = TemplateDtoMapper.toTemplateDTODefault(notificationTemplate);

		Assert.assertEquals(notificationTemplate.getKey(), defaultTemplate.getKey());
		Assert.assertEquals(notificationTemplate.getName(), defaultTemplate.getName());
		Assert.assertEquals(notificationTemplate.getMedium(), defaultTemplate.getMedium().name());
		Assert.assertEquals(notificationTemplate.getLocale(), defaultTemplate.getLocale());
		Assert.assertEquals(notificationTemplate.getDescription(), defaultTemplate.getDescription());
		Assert.assertEquals(notificationTemplate.getSubject(), defaultTemplate.getSubject());
		Assert.assertEquals(notificationTemplate.getBody(), defaultTemplate.getBody());
		Assert.assertEquals(notificationTemplate.getHeader(), defaultTemplate.getHeader());
		Assert.assertEquals(notificationTemplate.getFooter(), defaultTemplate.getFooter());
		Assert.assertEquals(notificationTemplate.getReplyTo(), defaultTemplate.getReplyTo());
		Assert.assertEquals(notificationTemplate.getFrom(), defaultTemplate.getFrom());

		Assert.assertNotNull(defaultTemplate.getTeamsTemplate());
		Assert.assertEquals(notificationTemplate.getTeamsTemplate().getMessageJson(),
				defaultTemplate.getTeamsTemplate().getMessageJson());
		Assert.assertEquals(notificationTemplate.getTeamsTemplate().getTitle(),
				defaultTemplate.getTeamsTemplate().getTitle());
		Assert.assertEquals(notificationTemplate.getTeamsTemplate().getText(),
				defaultTemplate.getTeamsTemplate().getText());

		TemplateDto template = TemplateDtoMapper.toTemplateDTO(notificationTemplate);
		Assert.assertEquals(notificationTemplate.getId(), template.getId());
		Assert.assertNotNull(defaultTemplate.getTeamsTemplate());
		Assert.assertEquals(notificationTemplate.getTeamsTemplate().getMessageJson(),
				template.getTeamsTemplate().getMessageJson());
		Assert.assertEquals(notificationTemplate.getTeamsTemplate().getTitle(),
				template.getTeamsTemplate().getTitle());
		Assert.assertEquals(notificationTemplate.getTeamsTemplate().getText(),
				template.getTeamsTemplate().getText());


		NotificationTemplate notificationTemplateResult = TemplateDtoMapper.toNotificationTemplate("acme-solar",
				defaultTemplate);
		Assert.assertEquals(notificationTemplate.getKey(), notificationTemplateResult.getKey());
		Assert.assertEquals(notificationTemplate.getName(), notificationTemplateResult.getName());
		Assert.assertNotNull(defaultTemplate.getTeamsTemplate());
		Assert.assertEquals(notificationTemplate.getTeamsTemplate().getMessageJson(),
				notificationTemplateResult.getTeamsTemplate().getMessageJson());
		Assert.assertEquals(notificationTemplate.getTeamsTemplate().getTitle(),
				notificationTemplateResult.getTeamsTemplate().getTitle());
		Assert.assertEquals(notificationTemplate.getTeamsTemplate().getText(),
				notificationTemplateResult.getTeamsTemplate().getText());

	}

	@Test
	public void templateDtoMapperTestPunctuation() {
		NotificationTemplate notificationTemplate = NotificationTemplate.newBuilder()
				.id("1234")
				.name("name")
				.key("key")
				.tenant("tenant")
				.medium(TemplateMediumDto.EMAIL.name().toLowerCase())
				.subject("[Original recipient: support@testmail.identitysoon.com] Please Review Your Employees' Access =")
				.body("Dear Support,<br /><p>This campaign allows each reviewer in the campaign" +
						" to verify that users have the correct entitlements.</p>")
				.replyTo("no-reply@sailpoint.com")
				.from("no-reply@sailpoint.com")
				.locale(Locale.ENGLISH)
				.build();
		TemplateVersion templateVersion = new TemplateVersion("test", notificationTemplate, new TemplateVersionInfo());

		TemplateDto templateDtoSanitized = TemplateDtoMapper.toTemplateDTO(templateVersion, true);

		Assert.assertEquals(notificationTemplate.getKey(), templateDtoSanitized.getKey());
		Assert.assertEquals(notificationTemplate.getName(), templateDtoSanitized.getName());
		Assert.assertEquals(notificationTemplate.getMedium(), templateDtoSanitized.getMedium().name());
		Assert.assertEquals(notificationTemplate.getLocale(), templateDtoSanitized.getLocale());
		Assert.assertEquals(notificationTemplate.getSubject(), templateDtoSanitized.getSubject());
		Assert.assertEquals(notificationTemplate.getBody(), templateDtoSanitized.getBody());
		Assert.assertEquals(notificationTemplate.getReplyTo(), templateDtoSanitized.getReplyTo());
		Assert.assertEquals(notificationTemplate.getFrom(), templateDtoSanitized.getFrom());
	}


}
