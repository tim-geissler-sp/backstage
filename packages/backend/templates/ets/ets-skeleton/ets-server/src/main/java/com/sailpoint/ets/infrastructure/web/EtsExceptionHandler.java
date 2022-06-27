/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web;

import com.sailpoint.atlas.boot.api.common.JSONExceptionMapper;
import com.sailpoint.cloud.api.client.model.errors.*;
import com.sailpoint.ets.exception.DuplicatedSubscriptionException;
import com.sailpoint.ets.exception.FieldTooLargeException;
import com.sailpoint.ets.exception.IllegalSubscriptionTypeException;
import com.sailpoint.ets.exception.IllegalUpdateException;
import com.sailpoint.ets.exception.NotFoundException;
import com.sailpoint.ets.exception.ValidationException;
import com.sailpoint.ets.exception.LimitExceededException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;

/**
 * Exception handler for trigger service's specific exceptions
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EtsExceptionHandler extends JSONExceptionMapper {

	@ExceptionHandler({ NotFoundException.class, ValidationException.class, DuplicatedSubscriptionException.class,
		FieldTooLargeException.class, LimitExceededException.class, IllegalUpdateException.class })
	public ResponseEntity<?> handleException(Exception e, HttpServletRequest request) {
		return toV3ErrorResponse(e, request);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ApiException wrapNonApiException(Throwable e) {
		ApiExceptionBuilder builder = new ApiExceptionBuilder().cause(e);

		String fieldName;
		String fieldValue;
		switch (e.getClass().getSimpleName()) {
			case "NotFoundException":
				fieldName = ((NotFoundException) e).getFieldName();
				fieldValue = ((NotFoundException) e).getFieldValue();

				if (fieldName == null && fieldValue == null) {
					return builder.notFound().build();
				} else {
					return builder
						.detailCode(ErrorDetailCode.REFERENCED_OBJECT_NOT_FOUND)
						.params(fieldName, fieldValue)
						.build();
				}
			case "ValidationException":
				fieldName = ((ValidationException) e).getFieldName();
				fieldValue = ((ValidationException) e).getFieldValue();

				if (fieldValue == null) {
					return builder.required(fieldName).build();
				} else {
					return builder
						.detailCode(ErrorDetailCode.ILLEGAL_VALUE)
						.params(fieldValue, fieldName)
						.build();
				}
			case "DuplicatedSubscriptionException":
				return builder
					.detailCode(ErrorDetailCode.REFERENCE_CONFLICT)
					.params("subscription", "trigger", ((DuplicatedSubscriptionException) e).getTriggerId())
					.build();
			case "FieldTooLargeException":
				return builder.detailCode(ErrorDetailCode.FIELD_TOO_LARGE)
					.params(((FieldTooLargeException)e).getField(), ((FieldTooLargeException)e).getMaxSize()).build();
			case "LimitExceededException":
				return builder.detailCode(ErrorDetailCode.LIMIT_VIOLATION)
					.params(((LimitExceededException)e).getMaxValue(), ((LimitExceededException)e).getLimitName()).build();
			case "IllegalUpdateException":
				return builder.detailCode(ErrorDetailCode.ILLEGAL_UPDATE_ATTEMPT)
					.params(((IllegalUpdateException)e).getField()).build();
			case "IllegalSubscriptionTypeException":
				return builder
					.detailCode(ErrorDetailCode.FORBIDDEN)
					.params(((IllegalSubscriptionTypeException)e).getField())
					.build();
		}
		return builder.internalServerError().build();
	}
}
