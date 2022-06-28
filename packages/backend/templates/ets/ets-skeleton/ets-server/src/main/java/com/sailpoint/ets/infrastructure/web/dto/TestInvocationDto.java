/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * DTO for Invocations.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class TestInvocationDto {
	@NotEmpty
	private String _triggerId;
	@NotNull
	private Map<String, Object> _contentJson;

	private List<String> _subscriptionIds;

	private Map<String, Object> _input;
}
