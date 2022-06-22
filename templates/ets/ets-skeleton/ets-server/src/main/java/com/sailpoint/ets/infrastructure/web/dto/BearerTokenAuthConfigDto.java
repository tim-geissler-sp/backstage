/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;

/**
 * DTO for define HTTP Basic Authentication Configuration.
 */
@Data
@EqualsAndHashCode
public class BearerTokenAuthConfigDto {
	@NotEmpty
	String _bearerToken;
}
