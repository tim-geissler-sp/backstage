/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web.dto;

import com.sailpoint.cloud.api.client.model.BaseDto;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * DTO for Event Status.
 */
@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class EventStatusDto extends BaseDto {

	@NotNull
	int _count;

	@NotEmpty
	String _topic;
}
