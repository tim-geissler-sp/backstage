/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sailpoint.cloud.api.client.model.BaseDto;
import com.sailpoint.ets.infrastructure.util.validator.JsonPathExpression;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.UUID;

/**
 * DTO for Subscriptions
 */
@Data
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionDto extends BaseDto {
	private String _id;
	@NotEmpty
	private String _triggerId;

	private String _triggerName;

	@NotNull
	private String _type;

	private Duration _responseDeadline;
	private HttpConfigDto _httpConfig;
	private InlineConfigDto _inlineConfig;
	private ScriptConfigDto _scriptConfig;
	private EventBridgeConfigDto _eventBridgeConfig;
	private WorkflowConfigDto _workflowConfig;

	@JsonPathExpression(message="filter should be a valid JsonPath expression")
	private String _filter;

	private String _name;
	private String _description;
	private boolean _enabled;

	public SubscriptionDto() {
		_id = UUID.randomUUID().toString();
		_responseDeadline = Duration.ofHours(1);
		_enabled = true;
	}
}
