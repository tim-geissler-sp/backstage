/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.manager.rest.resouce.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sailpoint.cloud.api.client.model.BaseDto;
import com.sailpoint.notification.template.common.model.teams.TeamsTemplate;
import com.sailpoint.notification.template.common.model.TemplateMediumDto;
import com.sailpoint.notification.template.common.model.slack.SlackTemplate;

import java.util.Locale;

/**
 * Class that represents a read only default Template DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TemplateDtoDefault extends BaseDto {

	private TemplateMediumDto _medium;

	private String _key;

	private String _name;

	private String _description;

	private Locale _locale;

	private String _subject;

	private String _header;

	private String _body;

	private String _footer;

	private String _from;

	private String _replyTo;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private SlackTemplate _slackTemplate;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private TeamsTemplate _teamsTemplate;

	public TemplateMediumDto getMedium() {
		return _medium;
	}

	public void setMedium(TemplateMediumDto medium) {
		_medium = medium;
	}

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		_name = name;
	}

	public String getKey() {
		return _key;
	}

	public void setKey(String displayName) {
		_key = displayName;
	}

	public String getDescription() {
		return _description;
	}

	public void setDescription(String description) {
		_description = description;
	}

	public String getSubject() {
		return _subject;
	}

	public void setSubject(String subject) {
		_subject = subject;
	}

	public String getHeader() {
		return _header;
	}

	public void setHeader(String header) {
		_header = header;
	}

	public String getBody() {
		return _body;
	}

	public void setBody(String body) {
		_body = body;
	}

	public String getFooter() {
		return _footer;
	}

	public void setFooter(String footer) {
		_footer = footer;
	}

	public String getFrom() {
		return _from;
	}

	public void setFrom(String from) {
		_from = from;
	}

	public String getReplyTo() {
		return _replyTo;
	}

	public void setReplyTo(String replyTo) {
		_replyTo = replyTo;
	}

	public Locale getLocale() {
		return _locale;
	}

	public void setLocale(Locale locale) {
		_locale = locale;
	}

	public SlackTemplate getSlackTemplate() {
		return _slackTemplate;
	}

	public void setSlackTemplate(SlackTemplate slackTemplate) {
		_slackTemplate = slackTemplate;
	}

	public TeamsTemplate getTeamsTemplate() {
		return _teamsTemplate;
	}

	public void setTeamsTemplate(TeamsTemplate teamsTemplate) {
		_teamsTemplate = teamsTemplate;
	}
}
