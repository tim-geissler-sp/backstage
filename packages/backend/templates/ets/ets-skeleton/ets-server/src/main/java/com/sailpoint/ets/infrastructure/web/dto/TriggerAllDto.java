/*
 * Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web.dto;

import com.sailpoint.ets.domain.trigger.TriggerType;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * DTO for triggers with additional feature key attribute
 */
@Data
public class TriggerAllDto extends TriggerDto {
	private String _featureStoreKey;

	@Builder(builderMethodName = "TriggerAllDtoBuilder")
	public TriggerAllDto(String featureStoreKey, String id, String name, TriggerType type, String description, String inputSchema,
						 String outputSchema, Map<String, Object> exampleInput, Map<String, Object> exampleOutput) {
		super(id, name, type, description, inputSchema, outputSchema, exampleInput, exampleOutput);
		this._featureStoreKey = featureStoreKey;
	}
}
