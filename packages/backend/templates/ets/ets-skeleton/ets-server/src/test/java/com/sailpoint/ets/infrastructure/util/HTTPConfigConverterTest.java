/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sailpoint.atlas.util.AwsEncryptionServiceUtil;
import com.sailpoint.ets.EtsProperties;
import com.sailpoint.ets.domain.subscription.Subscription;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.domain.trigger.TriggerRepo;
import com.sailpoint.ets.infrastructure.web.dto.BasicAuthConfigDto;
import com.sailpoint.ets.infrastructure.web.dto.BearerTokenAuthConfigDto;
import com.sailpoint.ets.infrastructure.web.dto.HttpAuthenticationType;
import com.sailpoint.ets.infrastructure.web.dto.HttpConfigDto;
import com.sailpoint.ets.infrastructure.web.dto.SubscriptionDto;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;
import org.apache.commons.codec.binary.Base64;
import com.sailpoint.ets.exception.ValidationException;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


/**
 * Unit tests for HTTPConfigConverter.
 */
@RunWith(MockitoJUnitRunner.class)
public class HTTPConfigConverterTest {

	@Mock
	AwsEncryptionServiceUtil _awsEncryptionServiceUtil;

	@Mock
	TriggerRepo _triggerRepo;

	@Mock
	EtsProperties _etsProperties;

	private ModelMapper _modelMapper;
	private ValidatorService _validatorService;

	@Before
	public void before() {
		byte[] byteValue = Base64.encodeBase64("userName".getBytes());
		when(_awsEncryptionServiceUtil.encryptData(any(), any()))
			.thenReturn(byteValue);

		when(_awsEncryptionServiceUtil.decryptDataWithoutCheckKey(any(), any()))
			.thenReturn(Base64.decodeBase64(byteValue));

		_modelMapper = new ModelMapper();
		_modelMapper.typeMap(Subscription.class, SubscriptionDto.class)
			.addMappings(mapper -> {
				mapper.using(new HTTPConfigConverter(_awsEncryptionServiceUtil)).map(Subscription::getConfig, SubscriptionDto::setHttpConfig);
				mapper.using(new InlineConfigConverter()).map(Subscription::getConfig, SubscriptionDto::setInlineConfig);
			});

		new ModelMapper().typeMap(Subscription.class, SubscriptionDto.class);

		_validatorService = new ValidatorService(new ObjectMapper(), _awsEncryptionServiceUtil, _triggerRepo, _etsProperties);
		_validatorService.init();
	}

	@Test
	public void basicAuthTest() {
		SubscriptionDto subscriptionDtoRaw = getSubscriptionDTO();
		HttpConfigDto dto = subscriptionDtoRaw.getHttpConfig();

		dto.setUrl("www.example.com");
		dto.setHttpAuthenticationType(HttpAuthenticationType.BASIC_AUTH);
		BasicAuthConfigDto basicAuth = new BasicAuthConfigDto();
		basicAuth.setPassword("1");
		basicAuth.setUserName("2");
		dto.setBasicAuthConfig(basicAuth);

		Subscription subscription = getSubscription(subscriptionDtoRaw);
		SubscriptionDto subscriptionDto = _modelMapper.map(subscription, SubscriptionDto.class);

		Assert.assertNotNull(subscriptionDto.getHttpConfig().getBasicAuthConfig());
		Assert.assertEquals("userName", subscriptionDto.getHttpConfig().getBasicAuthConfig().getUserName());

		dto.setHttpAuthenticationType(HttpAuthenticationType.BASIC_AUTH);
		dto.setBasicAuthConfig(null);
		subscriptionDtoRaw.setHttpConfig(dto);
		try {
			subscription = getSubscription(subscriptionDtoRaw);
			_modelMapper.map(subscription, SubscriptionDto.class);
			Assert.fail("should fail if no config present.");
		} catch (ValidationException e) {
			Assert.assertEquals("BasicAuthConfig", e.getFieldName());
		}

		dto.setHttpAuthenticationType(HttpAuthenticationType.NO_AUTH);
		dto.setBasicAuthConfig(basicAuth);
		subscriptionDtoRaw.setHttpConfig(dto);
		subscription = getSubscription(subscriptionDtoRaw);
		subscriptionDto = _modelMapper.map(subscription, SubscriptionDto.class);
		Assert.assertNull(subscriptionDto.getHttpConfig().getBasicAuthConfig());
	}

	@Test
	public void bearerTokenAuthTest() {
		SubscriptionDto subscriptionDtoRaw = getSubscriptionDTO();
		HttpConfigDto dto = subscriptionDtoRaw.getHttpConfig();

		dto.setHttpAuthenticationType(HttpAuthenticationType.BEARER_TOKEN);
		BearerTokenAuthConfigDto bearerTokenAuth = new BearerTokenAuthConfigDto();
		bearerTokenAuth.setBearerToken("eyJhbGciOiJIUzI1Ni");
		dto.setBearerTokenAuthConfig(bearerTokenAuth);

		Subscription subscription = getSubscription(subscriptionDtoRaw);
		SubscriptionDto subscriptionDto = _modelMapper.map(subscription, SubscriptionDto.class);

		Assert.assertNotNull(subscriptionDto.getHttpConfig().getBearerTokenAuthConfig());
		Assert.assertEquals("userName", subscriptionDto.getHttpConfig().getBearerTokenAuthConfig().getBearerToken());

		dto.setHttpAuthenticationType(HttpAuthenticationType.BEARER_TOKEN);
		dto.setBearerTokenAuthConfig(null);
		subscriptionDtoRaw.setHttpConfig(dto);
		try {
			subscription = getSubscription(subscriptionDtoRaw);
			_modelMapper.map(subscription, SubscriptionDto.class);
			Assert.fail("should fail if no config present.");
		} catch (ValidationException e) {
			Assert.assertEquals("BearerTokenAuthConfig", e.getFieldName());
		}

		dto.setHttpAuthenticationType(HttpAuthenticationType.NO_AUTH);
		dto.setBearerTokenAuthConfig(bearerTokenAuth);
		subscriptionDtoRaw.setHttpConfig(dto);
		subscription = getSubscription(subscriptionDtoRaw);
		subscriptionDto = _modelMapper.map(subscription, SubscriptionDto.class);
		Assert.assertNull(subscriptionDto.getHttpConfig().getBasicAuthConfig());
	}

	SubscriptionDto getSubscriptionDTO() {
		HttpConfigDto dto = new HttpConfigDto();
		dto.setUrl("www.example.com");
		SubscriptionDto subscriptionDtoRaw = new SubscriptionDto();
		subscriptionDtoRaw.setType(SubscriptionType.HTTP.toString());
		subscriptionDtoRaw.setId(UUID.randomUUID().toString());
		subscriptionDtoRaw.setHttpConfig(dto);
		return subscriptionDtoRaw;
	}

	Subscription getSubscription(SubscriptionDto subscriptionDtoRaw) {
		return Subscription.builder()
			.id(UUID.randomUUID())
			.type(SubscriptionType.HTTP)
			.config(_validatorService
				.getAndValidateSubscriptionConfig(subscriptionDtoRaw, SubscriptionType.HTTP))
			.build();
	}
}
