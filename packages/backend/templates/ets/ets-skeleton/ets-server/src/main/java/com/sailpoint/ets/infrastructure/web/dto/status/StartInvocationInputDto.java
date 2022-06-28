/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web.dto.status;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.util.Map;

/**
 * DTO for StartInvocationInput.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class StartInvocationInputDto {
	private String _triggerId;
	private Map<String, Object> _input;
	private Map<String, Object> _contentJson;
}
