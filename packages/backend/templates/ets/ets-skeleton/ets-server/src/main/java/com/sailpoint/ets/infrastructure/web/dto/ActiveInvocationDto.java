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
import java.time.OffsetDateTime;

/**
 * DTO for Active Invocation.
 */
@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class ActiveInvocationDto extends BaseDto {

	@NotEmpty
	String _id;

	@NotNull
	OffsetDateTime _created;

	@NotEmpty
	String _triggerId;

	@NotNull
	OffsetDateTime _deadline;
}
