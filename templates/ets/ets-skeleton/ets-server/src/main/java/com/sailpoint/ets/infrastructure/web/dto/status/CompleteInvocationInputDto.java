/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web.dto.status;

import com.sailpoint.cloud.api.client.model.errors.ErrorMessageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.util.Map;

/**
 * DTO for CompleteInvocationInput.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class CompleteInvocationInputDto {
	private Map<String, Object> _output;
	private ErrorMessageDto _localizedError;
}
