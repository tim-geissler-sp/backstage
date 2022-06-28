/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.repository.impl.dynamodb.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity that represents a slack notification template.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DynamoDBDocument
public class SlackTemplatePersistentEntity {
	private String _text;
	private String _blocks;
	private String _attachments;
	private String _notificationType;
	private String _approvalId;
	private String _requestId;
	private Boolean _isSubscription;
	private String _itemId;
	private String _itemType;
	private String _autoApprovalMessageJSON;
	private String _autoApprovalTitle;
}
