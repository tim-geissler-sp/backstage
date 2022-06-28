/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.repository.impl.dynamodb.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import java.util.UUID;

/**
 * Class for persist template entity in DynamoDB.
 */
@DynamoDBTable(tableName = "hermes_notification_templates")
public class TemplatePersistentEntity {

	public final static String ID_NAME = "id";

	public final static String KEY_NAME = "key";
	public final static String TENANT_KEY_INDEX_NAME = "tenantKeyIndex";

	public final static String VERSION_NAME = "version";
	public final static String TENANT_VERSION_INDEX_NAME = "tenantVersionIndex";

	public final static String TENANT_NAME = "tenant";

	//name is reserved keyword in DynamoDB.
	//https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ReservedWords.html
	public final static String TEMPLATE_NAME ="templateName";

	private String _tenant;

	private String _id;

	private String _key;

	private String _versionId;

	private String _name;

	private String _medium;

	private String _locale;

	private String _engine;

	private String _description;

	private String _subject;

	private String _header;

	private String _body;

	private String _footer;

	private String _from;

	private String _replyTo;

	private TemplateVersionPersistentEntity _versionInfo;

	private SlackTemplatePersistentEntity _slackTemplate;

	private TeamsTemplatePersistentEntity _teamsTemplate;

	public TemplatePersistentEntity() {
		_id = UUID.randomUUID().toString();
	}

	@DynamoDBHashKey(attributeName = TENANT_NAME)
	public String getTenant() {
		return _tenant;
	}

	public void setTenant(String tenant) {
		this._tenant = tenant;
	}

	@DynamoDBRangeKey(attributeName = ID_NAME)
	public String getId() {
		return _id;
	}

	public void setId(String id) {
		_id = id;
	}

	@DynamoDBIndexRangeKey(attributeName = KEY_NAME,
			localSecondaryIndexName = TENANT_KEY_INDEX_NAME)
	public String getKey() {
		return _key;
	}

	public void setKey(String key) {
		this._key = key;
	}

	@DynamoDBIndexRangeKey(attributeName = VERSION_NAME,
			localSecondaryIndexName = TENANT_VERSION_INDEX_NAME)
	public String getVersion() {
		return _versionId;
	}

	public void setVersion(String version) {
		this._versionId = version;
	}

	public String getMedium() {
		return _medium;
	}

	public void setMedium(String medium) {
		this._medium = medium;
	}

	@DynamoDBAttribute(attributeName = TEMPLATE_NAME)
	public String getName() {
		return _name;
	}

	public void setName(String name) {
		this._name = name;
	}

	public String getLocale() {
		return _locale;
	}

	public void setLocale(String locale) {
		this._locale = locale;
	}

	public String getEngine() {
		return _engine;
	}

	public void setEngine(String _engine) {
		this._engine = _engine;
	}

	public String getDescription() {
		return _description;
	}

	public void setDescription(String description) {
		this._description = description;
	}

	public String getSubject() {
		return _subject;
	}

	public void setSubject(String subject) {
		this._subject = subject;
	}

	public String getHeader() {
		return _header;
	}

	public void setHeader(String header) {
		this._header = header;
	}

	public String getBody() {
		return _body;
	}

	public void setBody(String body) {
		this._body = body;
	}

	public String getFooter() {
		return _footer;
	}

	public void setFooter(String footer) {
		this._footer = footer;
	}

	public String getFrom() {
		return _from;
	}

	public void setFrom(String from) {
		this._from = from;
	}

	public String getReplyTo() {
		return _replyTo;
	}

	public void setReplyTo(String replyTo) {
		this._replyTo = replyTo;
	}

	public TemplateVersionPersistentEntity getVersionInfo() {
		return _versionInfo;
	}

	public void setVersionInfo(TemplateVersionPersistentEntity versionInfo) {
		this._versionInfo = versionInfo;
	}

	public SlackTemplatePersistentEntity getSlackTemplate() {
		return _slackTemplate;
	}

	public void setSlackTemplate(SlackTemplatePersistentEntity _slackTemplate) {
		this._slackTemplate = _slackTemplate;
	}

	public TeamsTemplatePersistentEntity getTeamsTemplate() { return _teamsTemplate; }

	public void setTeamsTemplate(TeamsTemplatePersistentEntity _teamsTemplate) {
		this._teamsTemplate = _teamsTemplate;
	}

}
