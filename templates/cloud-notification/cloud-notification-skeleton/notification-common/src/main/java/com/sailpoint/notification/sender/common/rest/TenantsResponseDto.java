/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.rest;

import com.sailpoint.cloud.api.client.model.BaseDto;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Response DTO for GET {integration}/tenants which
 * enumerates the tenants enabled for said integration
 */
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
public class TenantsResponseDto extends BaseDto {

    private List<String> _tenants;
}
