/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web.dto;

import com.sailpoint.cloud.api.client.model.BaseDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * DTO for Invocations.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class InvocationDto extends BaseDto {
	private String _id;
	private String _triggerId;
	private String _secret;
	private Map<String, Object> _contentJson;
}
