/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.model;

import com.sailpoint.atlas.util.StringUtil;
import com.sailpoint.notification.template.common.model.slack.SlackTemplate;
import com.sailpoint.notification.template.common.model.teams.TeamsTemplate;

import java.util.Locale;

/**
 * Builder class for NotificationTemplate.
 */
public class NotificationTemplateBuilder {

	private String _id;

	private String _tenant;

	private String _medium;

	private String _name;

	private String _key;

	private String _description;

	private Locale _locale;

	private String _subject;

	private String _header;

	private String _body;

	private String _footer;

	private String _from;

	private String _replyTo;

	private SlackTemplate _slackTemplate;

	private TeamsTemplate _teamsTemplate;


	public String getId() {
		return _id;
	}

	NotificationTemplateBuilder() {
	}

	public NotificationTemplateBuilder id(String id) {
		_id = id;
		return this;
	}

	public String getTenant() {
		return _tenant;
	}

	public NotificationTemplateBuilder tenant(String tenant) {
		_tenant = tenant;
		return this;
	}

	public String getName() {
		return _name;
	}

	public String getKey() {
		return _key;
	}

	public String getMedium() {
		return _medium;
	}

	public NotificationTemplateBuilder medium(String medium) {
		_medium = medium;
		return this;
	}

	public NotificationTemplateBuilder name(String name) {
		_name = name;
		return this;
	}

	public NotificationTemplateBuilder key(String key) {
		_key = key;
		return this;
	}

	public String getDescription() {
		return _description;
	}

	public NotificationTemplateBuilder description(String description) {
		_description = description;
		return this;
	}

	public String getSubject() {
		return _subject;
	}

	public NotificationTemplateBuilder subject(String subject) {
		_subject = subject;
		return this;
	}

	public String getHeader() {
		return _header;
	}

	public NotificationTemplateBuilder header(String header) {
		_header = header;
		return this;
	}

	public String getBody() {
		return _body;
	}

	public NotificationTemplateBuilder body(String body) {
		_body = body;
		return this;
	}

	public String getFooter() {
		return _footer;
	}

	public NotificationTemplateBuilder footer(String footer) {
		_footer = footer;
		return this;
	}

	public String getFrom() {
		return _from;
	}

	public NotificationTemplateBuilder from(String from) {
		_from = from;
		return this;
	}

	public String getReplyTo() {
		return _replyTo;
	}

	public NotificationTemplateBuilder replyTo(String replyTo) {
		_replyTo = replyTo;
		return this;
	}

	public Locale getLocale() {
		return _locale;
	}

	public NotificationTemplateBuilder locale(Locale locale) {
		_locale = locale;
		return this;
	}

	SlackTemplate getSlackTemplate() {
		return _slackTemplate;
	}

	public NotificationTemplateBuilder slackTemplate(SlackTemplate slackTemplate) {
		_slackTemplate = slackTemplate;
		return this;
	}

	TeamsTemplate getTeamsTemplate() {
		return _teamsTemplate;
	}

	public NotificationTemplateBuilder teamsTemplate(TeamsTemplate teamsTemplate) {
		_teamsTemplate = teamsTemplate;
		return this;
	}

	public NotificationTemplate build() {
		return new NotificationTemplate(this);
	}

	private boolean isSubjectRequired() {
		return (StringUtil.isNullOrEmpty(getSubject()) && (getMedium()!= null && TemplateMediumDto.EMAIL.toString()
				.equals(getMedium().toUpperCase())));
	}

	private boolean isBodyRequired() {
		return (StringUtil.isNullOrEmpty(getBody()) && (getMedium()!= null && TemplateMediumDto.EMAIL.toString()
				.equals(getMedium().toUpperCase())));
	}
}
