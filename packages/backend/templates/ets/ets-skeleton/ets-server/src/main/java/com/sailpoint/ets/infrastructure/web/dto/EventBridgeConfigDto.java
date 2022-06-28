/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web.dto;


import com.amazonaws.regions.AwsRegionProvider;
import com.sailpoint.cloud.api.client.model.BaseDto;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;

/**
 * DTO for HTTP subscription config.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class EventBridgeConfigDto extends BaseDto {
	@NotEmpty
	String awsAccount;

	@NotEmpty
	String awsRegion;

	public EventBridgeConfigDto() {

	}
}
