/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.trigger;

import com.sailpoint.utilities.JsonUtil;
import com.sailpoint.ets.EtsProperties;
import com.sailpoint.ets.domain.trigger.EventSource;
import com.sailpoint.ets.domain.trigger.TriggerType;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test JsonTriggerRepo.
 */
public class JsonTriggerRepoTest {

	@Test
	public void JsonTriggerRepoExternalTest() {
		EtsProperties etsProperties = new EtsProperties();
		etsProperties.setJsonTriggersRepoFilePathJsonSchema(createTempJsonFile(getValidTriggerDescriptorJsonSchema()));
		try {
			JsonTriggerRepo triggerRepo = new JsonTriggerRepo(etsProperties);
			Assert.assertEquals(2, triggerRepo.findAll().count());
		} catch (Exception e) {
			Assert.fail("Should load triggers repo from external file");
		}
	}

	//TODO find a fix for this, the resource loader is looking for json in /triggers/ which as things are not 1 dir deeper, it fails to find them.
	@Test
	public void JsonTriggerRepoInternalTest() {
		try {
			ClassPathResource jsonResource = new ClassPathResource(JsonTriggerRepo.TRIGGER_SCHEMA_PATH);
			File jsonDir = jsonResource.getFile();
			JsonTriggerRepo triggerRepoForJson = new JsonTriggerRepo(new EtsProperties());
			Assert.assertEquals(jsonDir.listFiles().length, triggerRepoForJson.findAll().count());
		} catch (Exception e) {
			Assert.fail("Should load triggers repo from internal trigger directory");
		}
	}


	@Test
	public void JsonTriggerRepoNoTypeDefineTest() {
		EtsProperties etsProperties = new EtsProperties();
		etsProperties.setJsonTriggersRepoFilePathJsonSchema(createTempJsonFile(getNoTypeDefineTriggerDescriptor()));
		try {
			new JsonTriggerRepo(etsProperties);
			Assert.fail("Should fail to load triggers with no type define");
		} catch (Exception e) {
			Assert.assertEquals("Trigger must have type define", e.getMessage());
		}
	}

	@Test
	public void JsonTriggerRepoValidationForFireAndForgetTest() {
		EtsProperties etsProperties = new EtsProperties();
		etsProperties.setJsonTriggersRepoFilePathJsonSchema(createTempJsonFile(getBrokenFireAndForgetTriggerDescriptorJsonSchema()));
		try {
			new JsonTriggerRepo(etsProperties);
			Assert.fail("Should fail to load FireAndForget triggers with output schema define");
		} catch (Exception e) {
			Assert.assertEquals("Fire and Forget Trigger should not have output schema define", e.getMessage());
		}
	}

	@Test
	public void JsonTriggerRepoValidationTest() {
		try {
			new JsonTriggerRepo(new EtsProperties());
		} catch (Exception e) {
			Assert.fail("Error loading triggers from resource file(triggers.json): " + e.getMessage());
		}
	}

	@Test
	public void eventBasedTriggerWithInvalidTypeTest() {
		EtsProperties etsProperties = new EtsProperties();

		String jsonTmpJsonPath = createTempJsonFile(getEventBasedTriggerWithInvalidTypeJsonSchema());

		etsProperties.setJsonTriggersRepoFilePathJsonSchema(jsonTmpJsonPath);
		try {
			JsonTriggerRepo triggerRepo = new JsonTriggerRepo(etsProperties);
			Assert.fail("Should fail to load event based trigger with request response type");
		} catch (Exception e) {
			Assert.assertEquals("request response based trigger cannot have event sources", e.getMessage());
		}
	}

	/**
	 * Utility functions for create temp triggers json repo.
	 *
	 * @return file path to json repo.
	 */
	private String createTempJsonFile(List<JsonTriggerRepo.TriggerDescriptor> triggers) {
		File file = null;
		Path path;
		try {
			path = Files.createTempFile("triggers", ".json");
			file = path.toFile();
			Files.write(path, JsonUtil.toJsonPretty(createTempTriggerDescriptors(triggers))
				.getBytes(StandardCharsets.UTF_8));
			file.deleteOnExit();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return file != null ? file.getAbsolutePath() : null;
	}

	private JsonTriggerRepo.TriggerDescriptors createTempTriggerDescriptors(List<JsonTriggerRepo.TriggerDescriptor> triggers) {
		JsonTriggerRepo.TriggerDescriptors triggerDescriptors = new JsonTriggerRepo.TriggerDescriptors();
		triggerDescriptors.triggers = triggers;
		return triggerDescriptors;
	}

	private List<JsonTriggerRepo.TriggerDescriptor> getValidTriggerDescriptorJsonSchema() {
		List<JsonTriggerRepo.TriggerDescriptor> triggers = new ArrayList<>();
		//REQUEST_RESPONSE
		JsonTriggerRepo.TriggerDescriptor triggerDescriptor = new JsonTriggerRepo.TriggerDescriptor();
		triggerDescriptor.id = "requestResponseTrigger";
		triggerDescriptor.name = "Request_Response Trigger Name";
		triggerDescriptor.type = TriggerType.REQUEST_RESPONSE.toString();
		triggerDescriptor.description = "Test Trigger";


		//request response input schema build out
		Map<String, Object> requestResponseInputIdentityId = new HashMap<>();
		requestResponseInputIdentityId.put("type", "string");
		Map<String, Object> requestResponseInputApproved = new HashMap<>();
		requestResponseInputApproved.put("type", "string");

		Map<String, Object> requestResponseInputProperties = new HashMap<>();
		requestResponseInputProperties.put("identityId", requestResponseInputIdentityId);
		requestResponseInputProperties.put("approved", requestResponseInputApproved);

		List<String> requestResponseInputRequired = new ArrayList<>();
		requestResponseInputRequired.add("identityId");

		List<String> requestResponseInputOptional = new ArrayList<>();
		requestResponseInputOptional.add("approved");

		Map<String, Object> requestResponseInputRecordAccessRequestedInputFire = new HashMap<>();
		requestResponseInputRecordAccessRequestedInputFire.put("type", "object");
		requestResponseInputRecordAccessRequestedInputFire.put("required", requestResponseInputRequired);
		requestResponseInputRecordAccessRequestedInputFire.put("optional", requestResponseInputOptional);
		requestResponseInputRecordAccessRequestedInputFire.put("additionalProperties", false);
		requestResponseInputRecordAccessRequestedInputFire.put("properties", requestResponseInputProperties);

		Map<String, Object> requestResponseInputDefinitions = new HashMap<>();
		requestResponseInputDefinitions.put("record:AccessRequestedInput", requestResponseInputRecordAccessRequestedInputFire);

		triggerDescriptor.inputSchema = new HashMap<>();
		triggerDescriptor.inputSchema.put("definitions", requestResponseInputDefinitions);
		triggerDescriptor.inputSchema.put("$ref", "#/definitions/record:AccessRequestedInput");

		triggerDescriptor.exampleInput = new HashMap<>();
		triggerDescriptor.exampleInput.put("identityId", "201327fda1c44704ac01181e963d463c");

		//building json output schema
		Map<String, Object> requestResponseOutputIdentityId = new HashMap<>();
		requestResponseOutputIdentityId.put("type", "string");
		Map<String, Object> requestResponseOutputApproved = new HashMap<>();
		requestResponseOutputApproved.put("type", "string");

		Map<String, Object> requestResponseOutputProperties = new HashMap<>();
		requestResponseOutputProperties.put("identityId", requestResponseOutputIdentityId);
		requestResponseOutputProperties.put("approved", requestResponseOutputApproved);

		List<String> requestResponseOutputRequired = new ArrayList<>();
		requestResponseOutputRequired.add("identityId");

		List<String> requestResponseOutputOptional = new ArrayList<>();
		requestResponseOutputOptional.add("approved");

		Map<String, Object> requestResponseOutputRecordAccessRequestedInputFire = new HashMap<>();
		requestResponseOutputRecordAccessRequestedInputFire.put("type", "object");
		requestResponseOutputRecordAccessRequestedInputFire.put("required", requestResponseOutputRequired);
		requestResponseOutputRecordAccessRequestedInputFire.put("optional", requestResponseOutputOptional);
		requestResponseOutputRecordAccessRequestedInputFire.put("additionalProperties", false);
		requestResponseOutputRecordAccessRequestedInputFire.put("properties", requestResponseOutputProperties);

		Map<String, Object> requestResponseOutputDefinitions = new HashMap<>();
		requestResponseOutputDefinitions.put("record:AccessRequestedOutput", requestResponseOutputRecordAccessRequestedInputFire);

		triggerDescriptor.outputSchema = new HashMap<>();
		triggerDescriptor.outputSchema.put("definitions", requestResponseOutputDefinitions);
		triggerDescriptor.outputSchema.put("$ref", "#/definitions/record:AccessRequestedOutput");

		triggerDescriptor.exampleOutput = new HashMap<>();
		triggerDescriptor.exampleOutput.put("approved", true);

		triggers.add(triggerDescriptor);

		//FIRE_AND_FORGET
		triggerDescriptor = new JsonTriggerRepo.TriggerDescriptor();
		triggerDescriptor.id = "fireAndForgetTrigger";
		triggerDescriptor.name = "Fire and Forget Trigger Name";
		triggerDescriptor.type = TriggerType.FIRE_AND_FORGET.toString();
		triggerDescriptor.eventSources = Collections.singletonList(EventSource.builder().topic("IDENTITY").eventType("MANAGER_CHANGED").build());

		//fire and forget input schema build out
		Map<String, Object> fireForgetInputIdentityId = new HashMap<>();
		fireForgetInputIdentityId.put("type", "string");
		Map<String, Object> fireForgetInputApproved = new HashMap<>();
		fireForgetInputApproved.put("type", "string");

		Map<String, Object> fireForgetInputProperties = new HashMap<>();
		fireForgetInputProperties.put("identityId", fireForgetInputIdentityId);
		fireForgetInputProperties.put("approved", fireForgetInputApproved);

		List<String> fireForgetInputRequired = new ArrayList<>();
		fireForgetInputRequired.add("identityId");

		List<String> fireForgetInputOptional = new ArrayList<>();
		fireForgetInputOptional.add("approved");

		Map<String, Object> fireForgetInputRecordAccessRequestedInputFire = new HashMap<>();
		fireForgetInputRecordAccessRequestedInputFire.put("type", "object");
		fireForgetInputRecordAccessRequestedInputFire.put("required", fireForgetInputRequired);
		fireForgetInputRecordAccessRequestedInputFire.put("optional", fireForgetInputOptional);
		fireForgetInputRecordAccessRequestedInputFire.put("additionalProperties", false);
		fireForgetInputRecordAccessRequestedInputFire.put("properties", fireForgetInputProperties);

		Map<String, Object> fireForgetInputDefinitions = new HashMap<>();
		fireForgetInputDefinitions.put("record:AccessRequestedInputFire", fireForgetInputRecordAccessRequestedInputFire);

		triggerDescriptor.inputSchema = new HashMap<>();
		triggerDescriptor.inputSchema.put("definitions", fireForgetInputDefinitions);
		triggerDescriptor.inputSchema.put("$ref", "#/definitions/record:AccessRequestedInputFire");

		triggerDescriptor.exampleInput = new HashMap<>();
		triggerDescriptor.exampleInput.put("identityId", "901329fda1c44704ac91181e963d463f");

		triggers.add(triggerDescriptor);

		return triggers;
	}

	private List<JsonTriggerRepo.TriggerDescriptor> getEventBasedTriggerWithInvalidTypeJsonSchema() {
		JsonTriggerRepo.TriggerDescriptor triggerDescriptor = new JsonTriggerRepo.TriggerDescriptor();
		triggerDescriptor.id = "requestResponseTrigger";
		triggerDescriptor.name = "Test Trigger Name";
		triggerDescriptor.type = TriggerType.REQUEST_RESPONSE.toString();
		triggerDescriptor.description = "Test Trigger";
		triggerDescriptor.eventSources = Collections.singletonList(EventSource.builder().topic("IDENTITY").eventType("MANAGER_CHANGED").build());

		//fire and forget input schema build out
		Map<String, Object> fireForgetInputIdentityId = new HashMap<>();
		fireForgetInputIdentityId.put("type", "string");
		Map<String, Object> fireForgetInputApproved = new HashMap<>();
		fireForgetInputApproved.put("type", "string");

		Map<String, Object> fireForgetInputProperties = new HashMap<>();
		fireForgetInputProperties.put("identityId", fireForgetInputIdentityId);
		fireForgetInputProperties.put("approved", fireForgetInputApproved);

		List<String> fireForgetInputRequired = new ArrayList<>();
		fireForgetInputRequired.add("identityId");

		List<String> fireForgetInputOptional = new ArrayList<>();
		fireForgetInputOptional.add("approved");

		Map<String, Object> fireForgetInputRecordAccessRequestedInputFire = new HashMap<>();
		fireForgetInputRecordAccessRequestedInputFire.put("type", "object");
		fireForgetInputRecordAccessRequestedInputFire.put("required", fireForgetInputRequired);
		fireForgetInputRecordAccessRequestedInputFire.put("optional", fireForgetInputOptional);
		fireForgetInputRecordAccessRequestedInputFire.put("additionalProperties", false);
		fireForgetInputRecordAccessRequestedInputFire.put("properties", fireForgetInputProperties);

		Map<String, Object> fireForgetInputDefinitions = new HashMap<>();
		fireForgetInputDefinitions.put("record:AccessRequestedInput", fireForgetInputRecordAccessRequestedInputFire);

		triggerDescriptor.inputSchema = new HashMap<>();
		triggerDescriptor.inputSchema.put("definitions", fireForgetInputDefinitions);
		triggerDescriptor.inputSchema.put("$ref", "#/definitions/record:AccessRequestedInput");

		triggerDescriptor.exampleInput = new HashMap<>();
		triggerDescriptor.exampleInput.put("identityId", "901329fda1c44704ac91181e963d463f");

		//building json output schema
		Map<String, Object> requestResponseOutputIdentityId = new HashMap<>();
		requestResponseOutputIdentityId.put("type", "string");
		Map<String, Object> requestResponseOutputApproved = new HashMap<>();
		requestResponseOutputApproved.put("type", "string");

		Map<String, Object> requestResponseOutputProperties = new HashMap<>();
		requestResponseOutputProperties.put("identityId", requestResponseOutputIdentityId);
		requestResponseOutputProperties.put("approved", requestResponseOutputApproved);

		List<String> requestResponseOutputRequired = new ArrayList<>();
		requestResponseOutputRequired.add("identityId");

		List<String> requestResponseOutputOptional = new ArrayList<>();
		requestResponseOutputOptional.add("approved");

		Map<String, Object> requestResponseOutputRecordAccessRequestedInputFire = new HashMap<>();
		requestResponseOutputRecordAccessRequestedInputFire.put("type", "object");
		requestResponseOutputRecordAccessRequestedInputFire.put("required", requestResponseOutputRequired);
		requestResponseOutputRecordAccessRequestedInputFire.put("optional", requestResponseOutputOptional);
		requestResponseOutputRecordAccessRequestedInputFire.put("additionalProperties", false);
		requestResponseOutputRecordAccessRequestedInputFire.put("properties", requestResponseOutputProperties);

		Map<String, Object> requestResponseOutputDefinitions = new HashMap<>();
		requestResponseOutputDefinitions.put("record:AccessRequestedOutput", requestResponseOutputRecordAccessRequestedInputFire);

		triggerDescriptor.outputSchema = new HashMap<>();
		triggerDescriptor.outputSchema.put("definitions", requestResponseOutputDefinitions);
		triggerDescriptor.outputSchema.put("$ref", "#/definitions/record:AccessRequestedOutput");

		triggerDescriptor.exampleOutput = new HashMap<>();
		triggerDescriptor.exampleOutput.put("approved", true);
		return Collections.singletonList(triggerDescriptor);
	}

	private List<JsonTriggerRepo.TriggerDescriptor> getNoTypeDefineTriggerDescriptor() {
		List<JsonTriggerRepo.TriggerDescriptor> result = getValidTriggerDescriptorJsonSchema();
		result.get(0).type = null;
		return result;
	}

	private List<JsonTriggerRepo.TriggerDescriptor> getBrokenFireAndForgetTriggerDescriptorJsonSchema() {
		List<JsonTriggerRepo.TriggerDescriptor> result = getValidTriggerDescriptorJsonSchema();
		//set type FIRE_AND_FORGET but keep output schema
		result.get(0).type = TriggerType.FIRE_AND_FORGET.toString();
		return result;
	}
}
