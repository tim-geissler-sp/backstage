/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web.dto;


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
public class HttpConfigDto extends BaseDto {
	@NotEmpty
	String _url;

	ResponseMode _httpDispatchMode;

	HttpAuthenticationType _httpAuthenticationType;

	BasicAuthConfigDto _basicAuthConfig;

	BearerTokenAuthConfigDto _bearerTokenAuthConfig;

	public HttpConfigDto() {
		//Per V3 API HttpConfigDto may not include httpDispatchMode.
		//Set to default mode SYNC.
		_httpDispatchMode = ResponseMode.SYNC;
		//set to default NO_AUTH
		_httpAuthenticationType = HttpAuthenticationType.NO_AUTH;
	}


}
