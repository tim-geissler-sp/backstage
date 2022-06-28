/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.util;

import com.sailpoint.atlas.util.AwsEncryptionServiceUtil;
import com.sailpoint.ets.domain.subscription.ScriptLanguageType;
import com.sailpoint.ets.domain.subscription.Subscription;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.infrastructure.web.dto.ResponseMode;
import com.sailpoint.ets.infrastructure.web.dto.ScriptConfigDto;
import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;

import java.util.Collections;
import java.util.Map;

/**
 * Utility class for converting ScriptConfig map to ScriptConfigDto.
 */
public class ScriptConfigConverter extends DecryptableConverter implements Converter<Map<String, Object>, ScriptConfigDto> {

	public final static String LANGUAGE = "language";
	final static String SCRIPT_LOCATION = "scriptLocation";
	public final static String RESPONSE_MODE = "responseMode";

	public ScriptConfigConverter(AwsEncryptionServiceUtil awsEncryptionServiceUtil) {
		super(awsEncryptionServiceUtil);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ScriptConfigDto convert(MappingContext<Map<String, Object>, ScriptConfigDto> context) {
		Subscription subscription = (Subscription) context.getParent().getSource();
		if (subscription.getType() == SubscriptionType.SCRIPT) {
			ScriptConfigDto scriptConfigDto = new ScriptConfigDto();
			scriptConfigDto.setLanguage(ScriptLanguageType.valueOf(context.getSource().get(LANGUAGE).toString()));
			scriptConfigDto.setSource(decrypt(subscription.getScriptSource(),
				Collections.singletonMap(SUBSCRIPTION_ID, subscription.getId().toString())));

			scriptConfigDto.setResponseMode(context.getSource().containsKey(RESPONSE_MODE) ?
				ResponseMode.valueOf(context.getSource().get(RESPONSE_MODE).toString()) : ResponseMode.SYNC);

			return scriptConfigDto;
		} else {
			return null;
		}
	}
}
