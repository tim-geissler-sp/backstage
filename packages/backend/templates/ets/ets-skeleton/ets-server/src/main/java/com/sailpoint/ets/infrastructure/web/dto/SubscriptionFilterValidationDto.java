/*
 *  Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.infrastructure.web.dto;

import com.sailpoint.cloud.api.client.model.BaseDto;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import java.util.Map;

/**
 * DTO for Subscription Filter Validation Examples
 */
@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class SubscriptionFilterValidationDto extends BaseDto {
	private Map<String, Object> _input;

	@NotEmpty(message = "Required field \"filter\" was missing or empty.")
	private String _filter;

	public SubscriptionFilterValidationDto() {
	}

	public SubscriptionFilterValidationDto(Map<String, Object> input, String filter) {
		_input = input;
		_filter = filter;
	}
}
