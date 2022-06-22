/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web.dto;

import com.sailpoint.cloud.api.client.model.BaseDto;
import com.sailpoint.ets.domain.subscription.ScriptLanguageType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * DTO for SCRIPT subscription config.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ScriptConfigDto extends BaseDto {

	@NotNull
	ScriptLanguageType _language;

	@NotEmpty
	String _source;

	ResponseMode _responseMode;

	public ScriptConfigDto() {
		_responseMode = ResponseMode.SYNC;
	}
}
