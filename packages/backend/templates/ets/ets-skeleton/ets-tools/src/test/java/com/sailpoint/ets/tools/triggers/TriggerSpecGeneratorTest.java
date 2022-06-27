/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.tools.triggers;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class TriggerSpecGeneratorTest {

	TriggersSpecGenerator _triggersSpecGenerator;
	private final String TRIGGER_SCHEMA_PATH = "/../ets-server/src/main/resources/triggers/jsonSchema";
	private final String TRIGGER_SPEC_PATH = "/../docs/triggers/";

	@Before
	public void setUp() throws IOException {
		_triggersSpecGenerator = new TriggersSpecGenerator(TRIGGER_SCHEMA_PATH, TRIGGER_SPEC_PATH);
		File dir = new File(System.getProperty("user.dir") + TRIGGER_SPEC_PATH);
		if(dir.exists()){
			dir.delete();
			System.out.println("Deleting trigger spec directory for test execution..");
		}
	}

	@Test
	public void testGenerateTriggerSpecs() throws IOException {
		_triggersSpecGenerator.generateTriggerSpecs();
		File inputDir = new File(System.getProperty("user.dir") + TRIGGER_SCHEMA_PATH);
		File ouputDir = new File(System.getProperty("user.dir") + TRIGGER_SPEC_PATH);
		Assert.assertEquals(inputDir.listFiles().length, ouputDir.listFiles().length + TriggersSpecGenerator.excludeTriggerIdList.size());
	}

	@Test
	public void testFileContentWithNull() throws IOException {
		_triggersSpecGenerator.generateTriggerSpecs();
		FileReader inputReader = new FileReader(System.getProperty("user.dir") + TRIGGER_SCHEMA_PATH + "/idn_account_correlated.json");
		JsonObject jsonObject =  JsonParser.parseReader(inputReader).getAsJsonObject();
		JsonObject input  = jsonObject.get("exampleInput").getAsJsonObject();
		JsonObject output = JsonParser.parseString(getOutputSpecContent("Trigger-spec-idn-account-correlated.md")).getAsJsonObject();
		Assert.assertTrue(input.equals(output));
	}

	@Test
	public void testFileContentsWithTimestamp() throws IOException {
		_triggersSpecGenerator.generateTriggerSpecs();
		FileReader inputReader = new FileReader(System.getProperty("user.dir") + TRIGGER_SCHEMA_PATH + "/idn_identity_created.json");
		JsonObject jsonObject =  JsonParser.parseReader(inputReader).getAsJsonObject();
		JsonObject input  = jsonObject.get("exampleInput").getAsJsonObject();
		JsonObject output = JsonParser.parseString(getOutputSpecContent("Trigger-spec-idn-identity-created.md")).getAsJsonObject();
		Assert.assertTrue(input.equals(output));
	}

	private String getOutputSpecContent(String specFilename) throws IOException {

		StringBuilder content = new StringBuilder();
		BufferedReader reader = new BufferedReader(new FileReader(System.getProperty("user.dir") + TRIGGER_SPEC_PATH +specFilename));
		String newLine;
		StringBuilder json = new StringBuilder();
		boolean addToJson = false;
		while ((newLine = reader.readLine()) != null){
			content.append(newLine);
			if(newLine.startsWith("```json") || addToJson){
				addToJson = true;
				if(newLine.equals("```")) {
					break;
				}
				json.append(newLine).append("\n");
			}

		}
		return json.substring(9);
	}
}
