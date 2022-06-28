/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * DTO for INLINE subscription config.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class InlineConfigDto {
	Map<String, Object> _output;
	String _error;
}
