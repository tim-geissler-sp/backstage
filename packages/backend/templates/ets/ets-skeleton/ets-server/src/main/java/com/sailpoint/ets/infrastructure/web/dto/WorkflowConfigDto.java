/*
 * Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web.dto;


import com.sailpoint.cloud.api.client.model.BaseDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import java.util.Map;

/**
 * DTO for Workflow subscription config.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class WorkflowConfigDto extends BaseDto {
	@NotEmpty
	String workflowId;

	public WorkflowConfigDto() {

	}
}
