/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.repository.impl.dynamodb.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Entity that represents a teams notification template.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DynamoDBDocument
public class TeamsTemplatePersistentEntity {
	private String _title;
	private String _text;
	private String _messageJson;
	private Boolean _isSubscription;
	private String _approvalId;
	private String _requestId;
	private String _notificationType;
	private String _itemId;
	private String _itemType;
	private String _autoApprovalMessageJSON;
	private String _autoApprovalTitle;
	private Map<String, Object> _customFields;
}
