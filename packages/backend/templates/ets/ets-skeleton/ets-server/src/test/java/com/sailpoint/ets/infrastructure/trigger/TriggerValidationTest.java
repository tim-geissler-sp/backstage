/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.trigger;

import com.sailpoint.ets.EtsProperties;
import com.sailpoint.ets.domain.trigger.JsonSchema;
import lombok.extern.apachecommons.CommonsLog;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

import static com.sailpoint.ets.infrastructure.trigger.JsonTriggerRepo.TriggerDescriptor;
import static com.sailpoint.ets.infrastructure.trigger.JsonTriggerRepo.TriggerDescriptors;

/**
 * Unit tests that validate internal {@link JsonTriggerRepo} triggers'
 * example input/output against their Json schemas at build time.
 */
@CommonsLog
@RunWith(MockitoJUnitRunner.class)
public class TriggerValidationTest extends AbstractValidationTest {

	@Mock
	private EtsProperties _etsPropertiesMock;

	@InjectMocks
	private JsonTriggerRepo _jsonTriggerRepo;

	private TriggerDescriptors _descriptors;

	@Before
	public void setUp() throws Exception {
		_descriptors = _jsonTriggerRepo.getJsonRepositoryForJsonSchema(null);
	}

	@Test
	public void validateTriggerPayloads() throws Exception {
		for (TriggerDescriptor descriptor : _descriptors.getTriggers()) {

			// Validate trigger example input against input schema
			if (descriptor.getInputSchema() != null && descriptor.getExampleInput() != null) {
				final Map<String, Object> inputSchema = descriptor.getInputSchema();
				final Map<String, Object> exampleInputMap = descriptor.getExampleInput();

				log.info("Testing: " + descriptor.getId());
				try {
					JsonSchema schema = new JsonSchema(inputSchema, getJsonSchemaGeneratedDtoClass(descriptor, false));
					schema.validateSchema(exampleInputMap);
					schema.validate(exampleInputMap);
				} catch (Exception e) {
					throw new RuntimeException("Trigger '" + descriptor.getId()
						+ "' failed exampleInput validation\n" + e);
				}
			}

			// Validate trigger example output against output schema
			if (descriptor.getOutputSchema() != null && descriptor.getExampleOutput() != null) {
				final Map<String, Object> outputSchema = descriptor.getOutputSchema();
				final Map<String, Object> exampleOutputMap = descriptor.getExampleOutput();

				try {
					JsonSchema schema = new JsonSchema(outputSchema, getJsonSchemaGeneratedDtoClass(descriptor, true));
					schema.validateSchema(exampleOutputMap);
					schema.validate(exampleOutputMap);
				} catch (Exception e) {
					throw new RuntimeException("Trigger '" + descriptor.getId()
						+ "' failed exampleOutput validation\n" + e);
				}
			}
		}
	}

	private String getJsonSchemaGeneratedDtoClass(TriggerDescriptor descriptor, boolean isOutputSchema) {
		String dtoClassPrefix = "com.sailpoint.ets.domain.trigger.schemaGeneratedDto.";

		if (isOutputSchema) {
			return dtoClassPrefix + descriptor.id
				.replace(":", "_")
				.replace("-", "_")
				+ "."
				+ descriptor.outputSchema.get("$ref").toString()
				.replace("#/definitions/", "")
				.replace("record:", "Record");
		}
		return dtoClassPrefix + descriptor.id
			.replace(":", "_")
			.replace("-", "_")
			+ "."
			+ descriptor.inputSchema.get("$ref").toString()
			.replace("#/definitions/", "")
			.replace("record:", "Record");
	}
}
