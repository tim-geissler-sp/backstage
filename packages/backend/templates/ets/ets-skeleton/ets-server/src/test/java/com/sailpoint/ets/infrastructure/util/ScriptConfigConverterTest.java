/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.util;

import com.google.common.collect.ImmutableMap;
import com.sailpoint.atlas.util.AwsEncryptionServiceUtil;
import com.sailpoint.ets.domain.subscription.ScriptLanguageType;
import com.sailpoint.ets.domain.subscription.ScriptLocationType;
import com.sailpoint.ets.domain.subscription.Subscription;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.infrastructure.web.dto.SubscriptionDto;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ScriptConfigConverter.
 */
@RunWith(MockitoJUnitRunner.class)
public class ScriptConfigConverterTest {

	@Mock
	AwsEncryptionServiceUtil _awsEncryptionServiceUtil;

	private ModelMapper _modelMapper;

	@Before
	public void setup() {
		when(_awsEncryptionServiceUtil.decryptDataWithoutCheckKey(any(), any()))
			.thenReturn(Base64.decodeBase64(Base64.encodeBase64("decryptedScript".getBytes())));

		_modelMapper = new ModelMapper();
		_modelMapper.typeMap(Subscription.class, SubscriptionDto.class)
			.addMappings(mapper -> {
				mapper.using(new ScriptConfigConverter(_awsEncryptionServiceUtil)).map(Subscription::getConfig, SubscriptionDto::setScriptConfig);
			});
	}

	@Test
	public void testBuildDtoFromSubscription() {
		Subscription subscription = Subscription.builder()
			.id(UUID.randomUUID())
			.type(SubscriptionType.SCRIPT)
			.scriptSource(Base64.encodeBase64String("decryptedScript".getBytes()))
			.config(ImmutableMap.of("language", ScriptLanguageType.JAVASCRIPT.name(), "scriptLocation", ScriptLocationType.DB.name()))
			.build();

		SubscriptionDto subscriptionDto = _modelMapper.map(subscription, SubscriptionDto.class);

		assertEquals(subscriptionDto.getId(), subscription.getId().toString());
		assertEquals(subscriptionDto.getType(), SubscriptionType.SCRIPT.name());
		assertEquals(subscriptionDto.getScriptConfig().getLanguage(), ScriptLanguageType.JAVASCRIPT);
		assertEquals(subscriptionDto.getScriptConfig().getSource(), "decryptedScript");

	}
}
