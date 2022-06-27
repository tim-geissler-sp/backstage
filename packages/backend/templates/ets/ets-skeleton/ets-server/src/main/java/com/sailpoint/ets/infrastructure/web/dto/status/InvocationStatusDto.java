/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web.dto.status;

import com.sailpoint.cloud.api.client.model.BaseDto;
import com.sailpoint.ets.domain.status.InvocationType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import javax.validation.constraints.NotEmpty;
import java.time.OffsetDateTime;

/**
 * DTO for InvocationStatus.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class InvocationStatusDto extends BaseDto {
	@NotEmpty
	private String _id;
	@NotEmpty
	private String _triggerId;
	@NotEmpty
	private InvocationType _type;
	@NotEmpty
	private String _subscriptionId;
	private String _subscriptionName;
	private OffsetDateTime _created;
	private OffsetDateTime _completed;
	private StartInvocationInputDto _startInvocationInput;
	private CompleteInvocationInputDto _completeInvocationInput;
}
