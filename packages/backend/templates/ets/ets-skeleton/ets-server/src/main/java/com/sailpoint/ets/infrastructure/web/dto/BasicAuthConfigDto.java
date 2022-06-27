/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DTO for define HTTP Basic Authentication Configuration.
 */
@Data
@EqualsAndHashCode
public class BasicAuthConfigDto {
	String _userName;
	String _password;
}
