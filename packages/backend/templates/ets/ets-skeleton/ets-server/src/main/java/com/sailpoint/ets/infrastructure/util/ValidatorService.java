/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.util;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sailpoint.atlas.util.AwsEncryptionServiceUtil;
import com.sailpoint.ets.EtsProperties;
import com.sailpoint.ets.domain.subscription.ScriptLocationType;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.domain.trigger.Trigger;
import com.sailpoint.ets.domain.trigger.TriggerId;
import com.sailpoint.ets.domain.trigger.TriggerRepo;
import com.sailpoint.ets.exception.FieldTooLargeException;
import com.sailpoint.ets.exception.NotFoundException;
import com.sailpoint.ets.exception.ValidationException;
import com.sailpoint.ets.infrastructure.web.dto.SubscriptionDto;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.sailpoint.ets.infrastructure.util.DecryptableConverter.SUBSCRIPTION_ID;
import static com.sailpoint.ets.infrastructure.util.EventBridgeConfigConverter.AWS_ACCOUNT_NUMBER;
import static com.sailpoint.ets.infrastructure.util.EventBridgeConfigConverter.AWS_REGION;
import static com.sailpoint.ets.infrastructure.util.HTTPConfigConverter.BASIC_AUTH_CONFIG;
import static com.sailpoint.ets.infrastructure.util.HTTPConfigConverter.BEARER_TOKEN;
import static com.sailpoint.ets.infrastructure.util.HTTPConfigConverter.BEARER_TOKEN_AUTH_CONFIG;
import static com.sailpoint.ets.infrastructure.util.HTTPConfigConverter.PASSWORD;
import static com.sailpoint.ets.infrastructure.util.HTTPConfigConverter.USER_NAME;
import static com.sailpoint.ets.infrastructure.util.InlineConfigConverter.OUTPUT;
import static com.sailpoint.ets.infrastructure.util.ScriptConfigConverter.LANGUAGE;
import static com.sailpoint.ets.infrastructure.util.ScriptConfigConverter.SCRIPT_LOCATION;
import static com.sailpoint.ets.infrastructure.util.ScriptConfigConverter.RESPONSE_MODE;
import static com.sailpoint.ets.infrastructure.util.WorkflowConfigConverter.WORKFLOW_ID;

/**
 * Utility class used for validation.
 */
@Component
@RequiredArgsConstructor(onConstructor_={@Autowired})
public class ValidatorService {

	private Validator _validator;
	private final ObjectMapper _objectMapper;
	private final AwsEncryptionServiceUtil _awsEncryptionServiceUtil;
	private final TriggerRepo _triggerRepo;
	private final EtsProperties _properties;

	private static final String AWS_ACCOUNT_NUMBER_REGEX = "^[0-9]{12}$";

	@PostConstruct
	public void init() {
		_validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	/**
	 * Validate and return subscription configuration base on subscription type
	 * @param subscriptionDto the {@link SubscriptionDto} coming from http request
	 * @param subscriptionType the parsed subscription type
	 * @return subscription config if it's valid. Otherwise throw {@link ValidationException}
	 */
	public Map<String, Object> getAndValidateSubscriptionConfig(SubscriptionDto subscriptionDto, SubscriptionType subscriptionType) {

		switch (subscriptionType) {
			case HTTP:
				return validateHttpConfig(subscriptionDto);
			case INLINE:
				return validateInlineConfig(subscriptionDto);
			case SCRIPT:
				return validateScriptConfig(subscriptionDto);
			case EVENTBRIDGE:
				return validateEventBridgeConfig(subscriptionDto);
			case WORKFLOW:
				return validateWorkflowConfig(subscriptionDto);
			default:
				throw new ValidationException("type", subscriptionDto.getType());
		}
	}

	/**
	 * Validate the input object base on its constraints
	 * @param input the input object
	 * @param <T> type of the object
	 */
	public <T> void validate(T input) {
		Set<ConstraintViolation<T>> violations = _validator.validate(input);
		if (!violations.isEmpty()) {
			ConstraintViolation firstViolation = violations.iterator().next();
			String invalidPropertyName = firstViolation.getPropertyPath().toString();
			String invalidPropertyValue = firstViolation.getInvalidValue() == null ? null: firstViolation.getInvalidValue().toString();
			throw new ValidationException(invalidPropertyName.startsWith("_") ? invalidPropertyName.substring(1) : invalidPropertyName,
				invalidPropertyValue, new ConstraintViolationException(input.getClass().getSimpleName() + " validation failed with errors", violations));
		}
	}

	/**
	 * Helper function for Encrypt string value with context.
	 * @param value value.
	 * @param encryptionContext encryption context.
	 * @return encrypted string.
	 */
	private String encrypt(String value, Map<String, String> encryptionContext) {
		return Base64.encodeBase64String(_awsEncryptionServiceUtil.encryptData(value.getBytes(),
			encryptionContext));
	}

	/**
	 * Validates configuration of subscription with HTTP type and return in map format
	 * @param subscriptionDto {@link SubscriptionDto}
	 * @return validated configuration in map format
	 */
	private Map<String, Object> validateHttpConfig(SubscriptionDto subscriptionDto) {
		validate(subscriptionDto.getHttpConfig());
		Map<String, Object> config = _objectMapper.convertValue(subscriptionDto.getHttpConfig(),
			new TypeReference<HashMap<String, Object>>() {});
		final Map<String, String> encryptionContext = Collections.singletonMap(SUBSCRIPTION_ID, subscriptionDto.getId());
		switch (subscriptionDto.getHttpConfig().getHttpAuthenticationType()) {
			case BASIC_AUTH:
				if( subscriptionDto.getHttpConfig().getBasicAuthConfig() == null ||
					subscriptionDto.getHttpConfig().getBasicAuthConfig().getUserName() == null ||
					subscriptionDto.getHttpConfig().getBasicAuthConfig().getPassword() == null ||
					subscriptionDto.getHttpConfig().getBasicAuthConfig().getUserName() == "" ||
					subscriptionDto.getHttpConfig().getBasicAuthConfig().getPassword() == "") {
					// need to check for password and username validity
					throw new ValidationException("BasicAuthConfig", "null");
				}
				if(subscriptionDto.getHttpConfig().getBasicAuthConfig().getUserName()!=null) {
					((Map<String, Object>)config.get(BASIC_AUTH_CONFIG)).put(USER_NAME,
						encrypt(subscriptionDto.getHttpConfig().getBasicAuthConfig().getUserName(),
							encryptionContext));
				}
				if(subscriptionDto.getHttpConfig().getBasicAuthConfig().getPassword()!=null) {
					((Map<String, Object>) config.get(BASIC_AUTH_CONFIG)).put(PASSWORD,
						encrypt(subscriptionDto.getHttpConfig().getBasicAuthConfig().getPassword(),
							encryptionContext));
				}

				break;
			case BEARER_TOKEN:
				if(subscriptionDto.getHttpConfig().getBearerTokenAuthConfig() == null ||
					subscriptionDto.getHttpConfig().getBearerTokenAuthConfig().getBearerToken() == null ||
					subscriptionDto.getHttpConfig().getBearerTokenAuthConfig().getBearerToken() == "") {
					throw new ValidationException("BearerTokenAuthConfig", "null");
				}
				((Map<String, Object>) config.get(BEARER_TOKEN_AUTH_CONFIG)).put(BEARER_TOKEN,
					encrypt(subscriptionDto.getHttpConfig().getBearerTokenAuthConfig().getBearerToken(),
						encryptionContext));
				break;
		}
		return config;
	}

	/**
	 * Validates configuration of subscription with INLINE type and return in map format
	 * @param subscriptionDto {@link SubscriptionDto}
	 * @return validated configuration in map format
	 */
	private Map<String, Object> validateInlineConfig(SubscriptionDto subscriptionDto) {
		Map<String, Object> config = new HashMap<>();
		if (subscriptionDto.getInlineConfig() == null) {
			Map<String, Object> emptyOutput = new HashMap<>();
			config.put(OUTPUT, emptyOutput);
		} else {
			config = _objectMapper.convertValue(subscriptionDto.getInlineConfig(),
				new TypeReference<HashMap<String, Object>>() {});
		}

		try {
			Trigger trigger = _triggerRepo.findById(new TriggerId(subscriptionDto.getTriggerId()))
				.orElseThrow(() -> new NotFoundException("trigger", subscriptionDto.getTriggerId()));
			//no need to validate if trigger doesn't required output (usually fire and forget case)
			if(trigger.getOutputSchemaObject() != null) {
				trigger.validateOutput((Map<String, Object>) config.get(OUTPUT));
			}
		} catch (NotFoundException e) {
			throw e;
		} catch (Exception e) {
			throw new ValidationException("InlineConfig", config.toString());
		}

		return config;
	}

	public boolean isValidInputForTriggerSchema(String id, Map<String, Object> input) {
		try {
			TriggerId triggerId = new TriggerId(id);
			Trigger trigger = _triggerRepo.findById(triggerId)
				.orElseThrow(() -> new NotFoundException("trigger", triggerId.toString()));
			trigger.validateInput(input);
			return true;  // return true because validateData() will throw an exception if it is an invalid schema
		} catch(Exception e) {
			throw e;
		}
	}

	/**
	 * Validates configuration of subscription with SCRIPT type and return in map format
	 * @param subscriptionDto {@link SubscriptionDto}
	 * @return validated configuration in map format
	 */
	private Map<String, Object> validateScriptConfig(SubscriptionDto subscriptionDto) {
		Map<String, Object> config = new HashMap<>();
		if (subscriptionDto.getScriptConfig() == null) {
			throw new ValidationException("scriptConfig", null);
		}
		validate(subscriptionDto.getScriptConfig());

		if (subscriptionDto.getScriptConfig().getSource().getBytes().length > _properties.getScriptByteSizeLimit()) {
			throw new FieldTooLargeException("source", _properties.getScriptByteSizeLimit() + " bytes");
		}
		config.put(LANGUAGE, subscriptionDto.getScriptConfig().getLanguage());
		config.put(SCRIPT_LOCATION, ScriptLocationType.DB);
		config.put(RESPONSE_MODE, subscriptionDto.getScriptConfig().getResponseMode());

		return config;
	}

	/**
	 * Validate and return encrypted script source code
	 * @param subscriptionDto the {@link SubscriptionDto} coming from http request
	 * @param subscriptionType the parsed subscription type
	 * @return encrypted script source code
	 */
	public String getAndValidateScript(SubscriptionDto subscriptionDto, SubscriptionType subscriptionType) {
		if (subscriptionType == SubscriptionType.SCRIPT) {
			if (subscriptionDto.getScriptConfig() != null) {
				return encrypt(subscriptionDto.getScriptConfig().getSource(),
					Collections.singletonMap(SUBSCRIPTION_ID, subscriptionDto.getId()));
			} else {
				throw new ValidationException("scriptConfig", null);
			}
		} else {
			return null;
		}
	}

	/**
	 * Validates configuration of subscription with EventBridge type and return in map format
	 * @param subscriptionDto {@link SubscriptionDto}
	 * @return validated configuration in map format
	 */
	private Map<String, Object> validateEventBridgeConfig(SubscriptionDto subscriptionDto) {
		Map<String, Object> config = new HashMap<>();
		if (subscriptionDto.getEventBridgeConfig() == null) {
			throw new ValidationException("eventBridgeConfig", null);
		}
		validate(subscriptionDto.getEventBridgeConfig());

		if (!subscriptionDto.getEventBridgeConfig().getAwsAccount().matches(AWS_ACCOUNT_NUMBER_REGEX)) {
			throw new ValidationException("AWS Account", subscriptionDto.getEventBridgeConfig().getAwsAccount());
		}
		config.put(AWS_ACCOUNT_NUMBER, subscriptionDto.getEventBridgeConfig().getAwsAccount());
		config.put(AWS_REGION, Regions.fromName( subscriptionDto.getEventBridgeConfig().getAwsRegion() ).getName() );

		return config;
	}

	/**
	 * Validates configuration of subscription with Workflow type and return in map format
	 * @param subscriptionDto {@link SubscriptionDto}
	 * @return validated configuration in map format
	 */
	private Map<String, Object> validateWorkflowConfig(SubscriptionDto subscriptionDto) {
		Map<String, Object> config = new HashMap<>();
		if (subscriptionDto.getWorkflowConfig() == null) {
			throw new ValidationException("workflowConfig", null);
		}
		validate(subscriptionDto.getWorkflowConfig());

		if (subscriptionDto.getWorkflowConfig().getWorkflowId() == null) {
			throw new ValidationException("workflowId", null);
		}

		config.put(WORKFLOW_ID, subscriptionDto.getWorkflowConfig().getWorkflowId());

		return config;
	}
}
