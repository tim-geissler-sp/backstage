/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.manager.rest.resouce.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.sailpoint.notification.template.common.model.NotificationTemplate;
import com.sailpoint.notification.template.common.model.version.TemplateVersion;
import com.sailpoint.notification.template.common.model.version.TemplateVersionInfo;
import com.sailpoint.notification.template.common.model.version.TemplateVersionUserInfo;
import com.sailpoint.notification.template.common.util.HtmlUtil;

/**
 * Utility class for map Template DTOs <=> Notification Templates.
 */
public class TemplateDtoMapper {

	private static final ObjectMapper _objectMapper = new ObjectMapper();

	static {
		_objectMapper.registerModule(new JodaModule());
		_objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		_objectMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
		_objectMapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
	}

	private TemplateDtoMapper() {
	}

	/**
	 * Converts NotificationTemplate to TemplateDtoDefault.
	 * @param notificationTemplate notification template
	 * @return TTemplateDTODefault
	 */
	public static TemplateDtoDefault toTemplateDTODefault(NotificationTemplate notificationTemplate) {
		return _objectMapper.convertValue(notificationTemplate, TemplateDtoDefault.class);
	}

	/**
	 * Converts NotificationTemplate to TemplateDto.
	 * @param notificationTemplate notification template
	 * @return TemplateDTO
	 */
	public static TemplateDto toTemplateDTO(NotificationTemplate notificationTemplate) {
		return _objectMapper.convertValue(notificationTemplate, TemplateDto.class);
	}

	/**
	 * Converts NotificationTemplate to TemplateDtoVersion.
	 * @param notificationTemplate notification template
	 * @return TemplateDTO
	 */
	public static TemplateDtoVersion toTemplateDTOVersion(NotificationTemplate notificationTemplate) {
		return _objectMapper.convertValue(notificationTemplate, TemplateDtoVersion.class);
	}

	/**
	 * Converts TemplateDto to NotificationTemplate
	 * @param templateDto TemplateDto
	 * @return NotificationTemplate
	 */
	public static NotificationTemplate toNotificationTemplate(String tenant, TemplateDtoDefault templateDto) {
		return NotificationTemplate.newBuilder()
				.key(templateDto.getKey())
				.name(templateDto.getName())
				//set tenant
				.tenant(tenant)
				.medium(templateDto.getMedium().name())
				.description(templateDto.getDescription())
				.subject(templateDto.getSubject())
				.body(templateDto.getBody())
				.header(templateDto.getHeader())
				.footer(templateDto.getFooter())
				.replyTo(templateDto.getReplyTo())
				.from(templateDto.getFrom())
				.locale(templateDto.getLocale())
				.slackTemplate(templateDto.getSlackTemplate())
				.teamsTemplate(templateDto.getTeamsTemplate())
				.build();
	}

	/**
	 * Convert TemplateVersionDto to TemplateVersionInfo.
	 * @param versionDto TemplateVersionInfo.
	 * @return TemplateVersionInfo.
	 */
	public static TemplateVersionInfo toTemplateVersionInfo(TemplateVersionDto versionDto) {
		return new TemplateVersionInfo(_objectMapper.convertValue(versionDto.getCreatedBy(), TemplateVersionUserInfo.class),
				versionDto.getCreated(),
				versionDto.getNote());
	}

	/**
	 * Convert TemplateVersion to TemplateDto.
	 * @param templateVersion TemplateVersion.
	 * @param sanitize indicator if TemplateDto needs run HTML sanitize.
	 * @return TemplateDto.
	 */
	public static TemplateDto toTemplateDTO(TemplateVersion templateVersion, boolean sanitize) {
		TemplateDto result = toTemplateDTO(templateVersion.getNotificationTemplate());
		result.setCreated(templateVersion.getTemplateVersionInfo().getDate());
		return sanitize? sanitize(result) : result;
	}

	public static TemplateDtoVersion toTemplateDTOVersion(TemplateVersion templateVersion) {
		TemplateDtoVersion result = toTemplateDTOVersion(templateVersion.getNotificationTemplate());
		result.setVersionInfo(toTemplateVersionDto(templateVersion.getTemplateVersionInfo(), templateVersion.getVersionId()));
		result.setCreated(result.getVersionInfo().getCreated());
		return result;
	}

	/**
	 * Convert TemplateVersionInfo to TemplateVersionDto.
	 * @param versionInfo TemplateVersionInfo.
	 * @return TemplateVersionDto.
	 */
	private static TemplateVersionDto toTemplateVersionDto(TemplateVersionInfo versionInfo, String id) {
		return new TemplateVersionDto(_objectMapper.convertValue(versionInfo.getUpdatedBy(), TemplateVersionUserInfoDto.class),
				versionInfo.getDate(),
				versionInfo.getNote(),
				id);

	}

	/**
	 * Sanitize HTML in template dto.
	 * @param dto TemplateDto.
	 * @return TemplateDto.
	 */
	private static TemplateDto sanitize(TemplateDto dto) {
		dto.setHeader(HtmlUtil.sanitize(dto.getHeader()));
		dto.setBody(HtmlUtil.sanitize(dto.getBody()));
		dto.setFooter(HtmlUtil.sanitize(dto.getFooter()));
		dto.setSubject(dto.getSubject());

		dto.setFrom(dto.getFrom());
		dto.setReplyTo(dto.getReplyTo());
		return dto;
	}
}
