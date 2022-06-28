/*
 *  Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.infrastructure.util;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sailpoint.atlas.util.AwsEncryptionServiceUtil;

import com.sailpoint.ets.EtsProperties;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.domain.trigger.Trigger;
import com.sailpoint.ets.domain.trigger.TriggerRepo;
import com.sailpoint.ets.exception.NotFoundException;
import com.sailpoint.ets.exception.ValidationException;

import com.sailpoint.ets.infrastructure.web.dto.EventBridgeConfigDto;
import com.sailpoint.ets.infrastructure.web.dto.SubscriptionDto;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.sailpoint.ets.infrastructure.util.EventBridgeConfigConverter.AWS_ACCOUNT_NUMBER;
import static com.sailpoint.ets.infrastructure.util.EventBridgeConfigConverter.AWS_REGION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ValidatorService
 */
@RunWith(MockitoJUnitRunner.class)
public class ValidatorServiceTest {

	@Mock
	AwsEncryptionServiceUtil _awsEncryptionServiceUtil;

	@Mock
	TriggerRepo _triggerRepo;

	@Mock
	Trigger _trigger;

	@Mock
	EtsProperties _etsProperties;

	private ValidatorService _validatorService;

	@Before
	public void before() {
		_validatorService = new ValidatorService(new ObjectMapper(), _awsEncryptionServiceUtil, _triggerRepo, _etsProperties);
		_validatorService.init();
	}

	@Test(expected= NotFoundException.class)
	public void testisValidInputForTriggerSchemaWithMissingTrigger()
	{
		when(_triggerRepo.findById(any())).thenReturn(Optional.empty());

		String id = "test:fire-and-forget";

		Map<String, Object> inputMap = new HashMap<>();
		inputMap.put("approved", true);
		inputMap.put("identityId", "201327fda1c44704ac01181e963d463c");

		_validatorService.isValidInputForTriggerSchema(id, inputMap);
	}


	@Test(expected= ValidationException.class)
	public void testisValidInputForTriggerSchemaWithBadExampleInput()
	{
		doThrow(new ValidationException(null, null, new Exception())).when(_trigger).validateInput(any());
		when(_triggerRepo.findById(any())).thenReturn(Optional.of(_trigger));

		String id = "test:fire-and-forget";

		Map<String, Object> inputMap = new HashMap<>();
		inputMap.put("approved", 123);
		inputMap.put("identityId", "201327fda1c44704ac01181e963d463c");

		_validatorService.isValidInputForTriggerSchema(id, inputMap);
	}


	@Test
	public void testisValidInputForTriggerSchemaWithGoodExampleInput()
	{
		when(_triggerRepo.findById(any())).thenReturn(Optional.of(_trigger));

		String id = "test:fire-and-forget";

		Map<String, Object> inputMap = new HashMap<>();
		inputMap.put("approved", true);
		inputMap.put("identityId", "201327fda1c44704ac01181e963d463c");

		Boolean isValid = _validatorService.isValidInputForTriggerSchema(id, inputMap);
		assert(isValid == true);
	}

	@Test(expected= RuntimeException.class)
	public void testgetAndValidateSubscriptionConfigWithBadAccountInEventbridgeInput()
	{
		SubscriptionDto subscriptionDto = new SubscriptionDto();
		subscriptionDto.setType("EventBridge");
		EventBridgeConfigDto configDto = new EventBridgeConfigDto();
		configDto.setAwsAccount("123a ");
		configDto.setAwsRegion(Regions.AP_SOUTHEAST_2.getName());

		subscriptionDto.setEventBridgeConfig(configDto);

		_validatorService.getAndValidateSubscriptionConfig(subscriptionDto, SubscriptionType.EVENTBRIDGE);
	}

	@Test(expected= IllegalArgumentException.class)
	public void testgetAndValidateSubscriptionConfigWithBadRegionInEventbridgeInput()
	{
		SubscriptionDto subscriptionDto = new SubscriptionDto();
		subscriptionDto.setType("EventBridge");

		EventBridgeConfigDto configDto = new EventBridgeConfigDto();
		configDto.setAwsAccount("123456783012");
		configDto.setAwsRegion("fake Region");

		subscriptionDto.setEventBridgeConfig(configDto);

		_validatorService.getAndValidateSubscriptionConfig(subscriptionDto, SubscriptionType.EVENTBRIDGE);
	}

	@Test
	public void testgetAndValidateSubscriptionConfigWithEventbridgeHappyPath()
	{
		SubscriptionDto subscriptionDto = new SubscriptionDto();
		subscriptionDto.setType("EventBridge");

		EventBridgeConfigDto configDto = new EventBridgeConfigDto();
		configDto.setAwsAccount("123456783012");
		configDto.setAwsRegion(Regions.AP_SOUTHEAST_2.getName());

		subscriptionDto.setEventBridgeConfig(configDto);

		Map<String, Object> config = _validatorService.getAndValidateSubscriptionConfig(subscriptionDto, SubscriptionType.EVENTBRIDGE);

		Assert.assertEquals(2, config.size());
		Assert.assertEquals("123456783012", config.get(AWS_ACCOUNT_NUMBER));
		Assert.assertEquals(Regions.AP_SOUTHEAST_2.getName(), config.get(AWS_REGION));

	}

}
