/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web.dto;

/**
 * Enum for define Authentication type. Additional fields may be added in the future.
 */
public enum HttpAuthenticationType {
	NO_AUTH,
	BASIC_AUTH,
	BEARER_TOKEN
}
