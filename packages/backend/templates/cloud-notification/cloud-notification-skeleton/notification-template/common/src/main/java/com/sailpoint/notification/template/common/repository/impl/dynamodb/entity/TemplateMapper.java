/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.repository.impl.dynamodb.entity;

import com.sailpoint.notification.api.event.dto.SlackNotificationAutoApprovalData;
import com.sailpoint.notification.api.event.dto.TeamsNotificationAutoApprovalData;
import com.sailpoint.notification.template.common.model.NotificationTemplate;
import com.sailpoint.notification.template.common.model.NotificationTemplateBuilder;
import com.sailpoint.notification.template.common.model.teams.TeamsTemplate;
import com.sailpoint.notification.template.common.model.TemplateMediumDto;
import com.sailpoint.notification.template.common.model.slack.SlackTemplate;
import com.sailpoint.notification.template.common.model.version.TemplateVersion;
import com.sailpoint.notification.template.common.model.version.TemplateVersionInfo;
import com.sailpoint.notification.template.common.model.version.TemplateVersionUserInfo;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for map template <=> template persistent entity.
 */
public class TemplateMapper {

	public static final String DEFAULT_TEMPLATE_ENGINE = "velocity";

	/**
	 * Convert NotificationTemplate to TemplatePersistentEntity.
	 *
	 * @param version version id.
	 * @param template notification template.
	 * @param versionInfo version info.
	 * @return TemplatePersistentEntity.
	 */
	public static TemplatePersistentEntity toEntity(String version,
													NotificationTemplate template,
													TemplateVersionInfo versionInfo) {

		TemplatePersistentEntity result = new TemplatePersistentEntity();
		result.setTenant(template.getTenant());
		result.setKey(template.getKey());
		result.setName(template.getName());
		result.setMedium(template.getMedium());
		result.setLocale(template.getLocale().toLanguageTag());
		result.setEngine(DEFAULT_TEMPLATE_ENGINE);
		result.setVersion(version);

		result.setBody(template.getBody());
		result.setHeader(template.getHeader());
		result.setFooter(template.getFooter());
		result.setFrom(template.getFrom());
		result.setReplyTo(template.getReplyTo());
		result.setSubject(template.getSubject());
		result.setDescription(template.getDescription());

		if(template.getSlackTemplate() != null && (template.getMedium() != null && TemplateMediumDto.SLACK.toString()
				.equals(template.getMedium().toUpperCase()))) {
			result.setSlackTemplate(SlackTemplatePersistentEntity.builder()
					.attachments(template.getSlackTemplate().getAttachments())
					.blocks(template.getSlackTemplate().getBlocks())
					.text(template.getSlackTemplate().getText())
					.itemId(template.getSlackTemplate().getAutoApprovalData().getItemId())
					.itemType(template.getSlackTemplate().getAutoApprovalData().getItemType())
					.autoApprovalMessageJSON(template.getSlackTemplate().getAutoApprovalData().getAutoApprovalMessageJSON())
					.autoApprovalTitle(template.getSlackTemplate().getAutoApprovalData().getAutoApprovalTitle())
			.build());
		}

		if(template.getTeamsTemplate() != null && (template.getMedium() != null && TemplateMediumDto.TEAMS.toString()
				.equals(template.getMedium().toUpperCase()))) {
				result.setTeamsTemplate(TeamsTemplatePersistentEntity.builder()
					.messageJson(template.getTeamsTemplate().getMessageJson())
					.title(template.getTeamsTemplate().getTitle())
					.text(template.getTeamsTemplate().getText())
						.itemId(template.getTeamsTemplate().getAutoApprovalData().getItemId())
						.itemType(template.getTeamsTemplate().getAutoApprovalData().getItemType())
						.autoApprovalMessageJSON(template.getTeamsTemplate().getAutoApprovalData().getAutoApprovalMessageJSON())
						.autoApprovalTitle(template.getTeamsTemplate().getAutoApprovalData().getAutoApprovalTitle())
					.build());
		}

		TemplateVersionPersistentEntity versionInfoEntity =  new TemplateVersionPersistentEntity();
		versionInfoEntity.setDate(toDate(versionInfo.getDate()));
		TemplateVersionUserInfoPersistentEntity userInfo = new TemplateVersionUserInfoPersistentEntity(
				versionInfo.getUpdatedBy().getId(),
				versionInfo.getUpdatedBy().getName());
		versionInfoEntity.setUpdatedBy(userInfo);
		versionInfoEntity.setNote(versionInfo.getNote());
		result.setVersionInfo(versionInfoEntity);

		return result;
	}

	/**
	 * Convert persistent template to NotificationTemplate
	 * @param entity persistent entity.
	 * @return NotificationTemplate
	 */
	public static NotificationTemplate toDtoTemplate(TemplatePersistentEntity entity) {
		NotificationTemplateBuilder builder = NotificationTemplate.newBuilder().
				id(entity.getId()).
				tenant(entity.getTenant()).
				key(entity.getKey()).
				name(entity.getName()).
				medium(entity.getMedium()).
				locale(new Locale(entity.getLocale())).
				body(entity.getBody()).
				header(entity.getHeader()).
				footer(entity.getFooter()).
				from(entity.getFrom()).
				replyTo(entity.getReplyTo()).
				subject(entity.getSubject()).
				description(entity.getDescription());

		if(entity.getSlackTemplate() != null && (entity.getMedium() != null &&TemplateMediumDto.SLACK.toString()
				.equals(entity.getMedium().toUpperCase()))) {
			builder.slackTemplate(SlackTemplate.builder()
					.attachments(entity.getSlackTemplate().getAttachments())
					.blocks(entity.getSlackTemplate().getBlocks())
                    .isSubscription(entity.getSlackTemplate().getIsSubscription())
                    .notificationType(entity.getSlackTemplate().getNotificationType())
                    .approvalId(entity.getSlackTemplate().getApprovalId())
                    .requestId(entity.getSlackTemplate().getRequestId())
					.text(entity.getSlackTemplate().getText())
					.autoApprovalData(SlackNotificationAutoApprovalData.builder()
							.itemId(entity.getSlackTemplate().getItemId())
							.itemType(entity.getSlackTemplate().getItemType())
							.autoApprovalMessageJSON(entity.getSlackTemplate().getAutoApprovalMessageJSON())
							.autoApprovalTitle(entity.getSlackTemplate().getAutoApprovalTitle()).build())
					.build());
		}

		if(entity.getTeamsTemplate() != null && (entity.getMedium() != null &&TemplateMediumDto.TEAMS.toString()
				.equals(entity.getMedium().toUpperCase()))) {
			builder.teamsTemplate(TeamsTemplate.builder()
					.title(entity.getTeamsTemplate().getTitle())
					.text(entity.getTeamsTemplate().getText())
					.autoApprovalData(TeamsNotificationAutoApprovalData.builder()
							.itemId(entity.getTeamsTemplate().getItemId())
							.itemType(entity.getTeamsTemplate().getItemType())
							.autoApprovalMessageJSON(entity.getTeamsTemplate().getAutoApprovalMessageJSON())
							.autoApprovalTitle(entity.getTeamsTemplate().getAutoApprovalTitle()).build())
					.messageJson(entity.getTeamsTemplate().getMessageJson())
					.isSubscription(entity.getTeamsTemplate().getIsSubscription())
					.approvalId(entity.getTeamsTemplate().getApprovalId())
					.requestId(entity.getTeamsTemplate().getRequestId())
					.notificationType(entity.getTeamsTemplate().getNotificationType())
					.build());
		}
		return builder.build();
	}

	/**
	 * Convert TemplatePersistentEntity to TemplateVersion.
	 * @param entity TemplatePersistentEntity
	 * @return TemplateVersion
	 */
	public static TemplateVersion toDtoTemplateVersion(TemplatePersistentEntity entity) {
		TemplateVersionUserInfo userInfo = new TemplateVersionUserInfo(
				entity.getVersionInfo().getUpdatedBy().getId(),
				entity.getVersionInfo().getUpdatedBy().getName());

		return new TemplateVersion(entity.getVersion(), toDtoTemplate(entity),
				new TemplateVersionInfo(userInfo,
						toOffsetDateTime(entity.getVersionInfo().getDate()),
						entity.getVersionInfo().getNote()));
	}

	private static OffsetDateTime toOffsetDateTime(Date date) {
		Instant instant = date.toInstant();
		return instant.atOffset(ZoneOffset.UTC);
	}

	private static Date toDate(OffsetDateTime date) {
		ZonedDateTime zdt = date.toInstant().atZone(ZoneOffset.UTC) ;
		return Date.from(zdt.toInstant());
	}
}
