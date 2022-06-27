/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web.dto;

import com.sailpoint.cloud.api.client.model.BaseDto;
import com.sailpoint.ets.domain.trigger.TriggerName;
import com.sailpoint.ets.domain.trigger.TriggerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * DTO for Triggers
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class TriggerDto extends BaseDto {
	private String _id;
	private String _name;
	private TriggerType _type;
	private String _description;
	private String _inputSchema;
	private String _outputSchema;
	private Map<String, Object> _exampleInput;
	private Map<String, Object> _exampleOutput;
}
