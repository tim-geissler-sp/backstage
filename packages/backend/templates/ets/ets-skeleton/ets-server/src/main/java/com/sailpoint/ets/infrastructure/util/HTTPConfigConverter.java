/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.util;

import com.google.common.base.Enums;
import com.sailpoint.atlas.util.AwsEncryptionServiceUtil;
import com.sailpoint.ets.domain.subscription.Subscription;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.infrastructure.web.dto.BasicAuthConfigDto;
import com.sailpoint.ets.infrastructure.web.dto.BearerTokenAuthConfigDto;
import com.sailpoint.ets.infrastructure.web.dto.HttpAuthenticationType;
import com.sailpoint.ets.infrastructure.web.dto.HttpConfigDto;
import com.sailpoint.ets.infrastructure.web.dto.ResponseMode;
import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;

import java.util.Collections;
import java.util.Map;

/**
 * Utility class for converting HTTPConfig map to HttpConfigDto.
 */
public class HTTPConfigConverter extends DecryptableConverter implements Converter<Map<String, Object>, HttpConfigDto> {
	private final static String URL = "url";
	private final static String HTTP_DISPATCH_MODE = "httpDispatchMode";
	private final static String HTTP_AUTHENTICATION_TYPE = "httpAuthenticationType";
	final static String BASIC_AUTH_CONFIG = "basicAuthConfig";
	final static String BEARER_TOKEN_AUTH_CONFIG = "bearerTokenAuthConfig";
	final static String PASSWORD = "password";
	final static String USER_NAME = "userName";
	final static String BEARER_TOKEN = "bearerToken";

	public HTTPConfigConverter(AwsEncryptionServiceUtil awsEncryptionServiceUtil) {
		super(awsEncryptionServiceUtil);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public HttpConfigDto convert(MappingContext<Map<String, Object>, HttpConfigDto> context) {
		Map<String, Object> httpConfig = context.getSource();
		Subscription subscription = (Subscription) context.getParent().getSource();
		if (subscription.getType() == SubscriptionType.HTTP) {
			final Map<String, String> encryptionContext = Collections.singletonMap(SUBSCRIPTION_ID, subscription
				.getId().toString());
			return convertToHttpConfigDto(httpConfig, encryptionContext);
		} else {
			return null;
		}
	}

	/**
	 * Convert httpConfig map representation to HttpConfigDto
	 * @param httpConfig map contains httpConfig.
	 * @param encryptionContext encryptionContext used for encrypting httpConfig.
	 * @return resulting HttpConfigDto;
	 */
	public HttpConfigDto convertToHttpConfigDto(Map<String, Object> httpConfig, final Map<String, String> encryptionContext) {
		HttpConfigDto configDto =  new HttpConfigDto();
		if(httpConfig == null) {
			return configDto;
		}
		if(httpConfig.get(URL) != null) {
			configDto.setUrl((String) httpConfig.get(URL));
		}

		String mode = (String)httpConfig.get(HTTP_DISPATCH_MODE);
		ResponseMode dispatchMode = ResponseMode.SYNC;
		if(mode!= null) {
			dispatchMode = Enums.getIfPresent(ResponseMode.class,
				mode).toJavaUtil().orElseThrow(()-> new IllegalStateException("Incorrect value "
				+ mode + " provided for HttpDispatchMode"));
		}
		configDto.setHttpDispatchMode(dispatchMode);

		String type = (String)httpConfig.get(HTTP_AUTHENTICATION_TYPE);
		HttpAuthenticationType httpAuthType = HttpAuthenticationType.NO_AUTH;
		if(type != null) {
			httpAuthType = Enums.getIfPresent(HttpAuthenticationType.class,
				(String)httpConfig.get(HTTP_AUTHENTICATION_TYPE)).toJavaUtil()
				.orElseThrow(()-> new IllegalStateException("Incorrect value "
					+ type + " provided for HttpAuthenticationType"));
		}

		configDto.setHttpAuthenticationType(httpAuthType);
		if(type != null) {
			switch (httpAuthType) {
				case BASIC_AUTH:
					BasicAuthConfigDto basicConfigDto = new BasicAuthConfigDto();
					Map<String, Object> basic = (Map<String, Object>)httpConfig.get(BASIC_AUTH_CONFIG);
					if(basic != null) {
						if(basic.containsKey(USER_NAME)) {
							basicConfigDto.setUserName(decrypt((String)basic.get(USER_NAME),
								encryptionContext));
						}
						if(basic.containsKey(PASSWORD)) {
							basicConfigDto.setPassword(decrypt((String)basic.get(PASSWORD),
								encryptionContext));
						}
					}
					configDto.setBasicAuthConfig(basicConfigDto);
					return configDto;
				case BEARER_TOKEN:
					BearerTokenAuthConfigDto bearerTokenAuthConfigDto = new BearerTokenAuthConfigDto();
					Map<String, Object> token = (Map<String, Object>)httpConfig.get(BEARER_TOKEN_AUTH_CONFIG);
					if(token != null) {
						if(token.containsKey(BEARER_TOKEN)) {
							bearerTokenAuthConfigDto.setBearerToken(decrypt((String)token.get(BEARER_TOKEN),
								encryptionContext));
						}
					}
					configDto.setBearerTokenAuthConfig(bearerTokenAuthConfigDto);
					return configDto;
			}
		}
		return configDto;
	}
}
