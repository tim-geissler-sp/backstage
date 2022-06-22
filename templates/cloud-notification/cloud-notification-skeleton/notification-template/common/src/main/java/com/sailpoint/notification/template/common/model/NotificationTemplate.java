/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.model;

import com.sailpoint.notification.template.common.model.slack.SlackTemplate;
import com.sailpoint.notification.template.common.model.teams.TeamsTemplate;

import java.util.Locale;

/**
 * Entity that represents a notification template.
 */
public class NotificationTemplate {

	private final String _id;

	private final String _tenant;

	private final String _medium;

	private final String _name;

	private final String _key;

	private final String _description;

	private final Locale _locale;

	private final String _subject;

	private final String _header;

	private final String _body;

	private final String _footer;

	private final String _from;

	private final String _replyTo;

	private final SlackTemplate _slackTemplate;

	private final TeamsTemplate _teamsTemplate;

	public NotificationTemplate(NotificationTemplateBuilder notificationTemplateBuilder) {
		_id = notificationTemplateBuilder.getId();
		_tenant = notificationTemplateBuilder.getTenant();
		_medium = notificationTemplateBuilder.getMedium();
		_name = notificationTemplateBuilder.getName();
		_key = notificationTemplateBuilder.getKey();
		_description = notificationTemplateBuilder.getDescription();
		_subject = notificationTemplateBuilder.getSubject();
		_header = notificationTemplateBuilder.getHeader();
		_body = notificationTemplateBuilder.getBody();
		_footer = notificationTemplateBuilder.getFooter();
		_locale = notificationTemplateBuilder.getLocale();
		_from = notificationTemplateBuilder.getFrom();
		_replyTo = notificationTemplateBuilder.getReplyTo();
		_slackTemplate = notificationTemplateBuilder.getSlackTemplate();
		_teamsTemplate = notificationTemplateBuilder.getTeamsTemplate();

	}

	public static NotificationTemplateBuilder newBuilder() {
		return new NotificationTemplateBuilder();
	}

	public String getId() {
		return _id;
	}

	public String getTenant() {
		return _tenant;
	}

	public String getMedium() {
		return _medium.toUpperCase();
	}

	public String getName() {
		return _name;
	}

	public String getKey() {
		return _key;
	}

	public String getDescription() {
		return _description;
	}

	public Locale getLocale() {
		return _locale;
	}

	public String getSubject() {
		return _subject;
	}

	public String getHeader() {
		return _header;
	}

	public String getBody() {
		return _body;
	}

	public String getFooter() {
		return _footer;
	}

	public String getFrom() {
		return _from;
	}

	public String getReplyTo() {
		return _replyTo;
	}

	public SlackTemplate getSlackTemplate() { return _slackTemplate; }

	public TeamsTemplate getTeamsTemplate() { return _teamsTemplate; }


}
