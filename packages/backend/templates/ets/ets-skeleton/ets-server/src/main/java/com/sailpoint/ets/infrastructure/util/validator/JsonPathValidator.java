/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.util.validator;

import com.sailpoint.atlas.util.JsonPathUtil;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * JsonPathValidator
 */
public class JsonPathValidator implements ConstraintValidator<JsonPathExpression, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}

		return JsonPathUtil.isValidPath(value);
	}
}
