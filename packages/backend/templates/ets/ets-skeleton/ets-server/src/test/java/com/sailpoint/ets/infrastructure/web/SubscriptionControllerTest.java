/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web;

import com.sailpoint.ets.infrastructure.web.dto.SubscriptionFilterValidationDto;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Objects;

/**
 * Unit tests for {@link SubscriptionController}
 */
@RunWith(MockitoJUnitRunner.class)
public class SubscriptionControllerTest {

	@InjectMocks
	private SubscriptionController _subscriptionController;

	@Test
	public void nonExistentPathShouldBeInvalid() throws Exception {
		SubscriptionFilterValidationDto testDto = SubscriptionFilterValidationDto.builder()
			.input(Collections.singletonMap("mockKey", "mockStringValue"))
			.filter("$.nonExistentPath")
			.build();

		String result = Objects.requireNonNull(_subscriptionController.validateFilter(testDto).getBody()).toString().trim();
		Assert.assertEquals("{ \"isValid\" : false }", result);
	}

	@Test
	public void existentPathShouldBeValid() throws Exception {
		SubscriptionFilterValidationDto testDto = SubscriptionFilterValidationDto.builder()
			.input(Collections.singletonMap("mockKey", "mockStringValue"))
			.filter("$.mockKey")
			.build();

		String result = Objects.requireNonNull(_subscriptionController.validateFilter(testDto).getBody()).toString().trim();
		Assert.assertEquals("{ \"isValid\" : true }", result);
	}
}
