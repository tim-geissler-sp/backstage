/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.tools.triggers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * This class is responsible for creating spec
 * files for all the triggers. This will be executed as
 * part of the github action.
 * For details about the action pls see ets/.github/workflows/ets-tools.yml
 */
public class TriggersSpecGenerator {

	private  static final  String NEW_LINE = "\n\n";
	private  static final String JSON_START = "```json \n";
	private  static final String JSON_END = "\n```";
	private  static final String FILE_PREFIX = "Trigger-spec-";
	private  static final String FILE_EXT = ".md";
	private  static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
	public   static final Set<String> excludeTriggerIdList = Stream.of("test:request-response", "test:fire-and-forget").collect(Collectors.toSet());

	//works with github action
	private   String _triggerJsonPath = "/../ets-server/src/main/resources/triggers/jsonSchema";
	private   String _triggerSpecPath = "/../docs/triggers/";


	public TriggersSpecGenerator() {
	}

	public TriggersSpecGenerator(String triggerJsonPath, String triggerSpecPath) {
		if(triggerJsonPath != null && triggerJsonPath.length() > 0){
			this._triggerJsonPath = triggerJsonPath;
		}
		if(triggerSpecPath != null && triggerSpecPath.length() > 0){
			this._triggerSpecPath = triggerSpecPath;
		}
	}


	/**
	 * It is the driver method for this module
	 * It iterates over files in directory where json trigger files reside.
	 * @throws IOException
	 */
	public  void generateTriggerSpecs() throws IOException {

		File dir = new File(System.getProperty("user.dir") + _triggerJsonPath);
		if(!dir.exists()){
			System.out.println( "Directory does not exist : " + dir.getPath());
			return;
		}

		String[]  paths = dir.list();
		for(String f : paths){
			readJsonFromFile(dir.getPath() + "/" + f);
		}

	}


	/**
	 * Iterate over files in directory where json trigger files reside.
	 * @param path - directory path where the trigger json files reside .
	 * @throws IOException
	 */
	private void readJsonFromFile(String path) throws IOException{
		FileReader reader = null;
		try{
			reader = new FileReader(path);
			JsonObject object =  JsonParser.parseReader(reader).getAsJsonObject();
			generateSpecFile(object);
			reader.close();
		} finally {
			if(reader != null){
				reader.close();
			}
		}

	}


	/**
	 * Generates spec(.md) files based on the contents from trigger json.
	 * @param jsonObject - json contents of the trigger .
	 * @throws IOException
	 */
	private void generateSpecFile(JsonObject jsonObject) throws IOException {

		if(!jsonObject.has("id")){
			return;
		}

		if(isTestTrigger(jsonObject.get("id").toString())){
			System.out.println("Skipping generating spec file for test trigger " + jsonObject.get("id").toString());
			return;
		}

		String fileName = FILE_PREFIX + buildFileName(jsonObject.get("id").toString()) + FILE_EXT;

		File dir = new File(System.getProperty("user.dir") + _triggerSpecPath);

		if(!dir.exists()){
			System.out.println("Creating directory " + System.getProperty("user.dir") + _triggerSpecPath);
			dir.mkdir();
		}

		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(System.getProperty("user.dir") + _triggerSpecPath + fileName));

			if(jsonObject.has("id")) {
				writer.write("Id : " + jsonObject.get("id").toString() + NEW_LINE);
			}
			if(jsonObject.has("type")) {
				writer.write("Type : " + jsonObject.get("type").toString() + NEW_LINE);
			}
			if(jsonObject.has("name")) {
				writer.write("Name : " + jsonObject.get("name").toString() + NEW_LINE);
			}
			if(jsonObject.has("description")) {
				writer.write("Description : " + jsonObject.get("description").toString() + NEW_LINE);
			}
			if (jsonObject.has("exampleInput")) {
				writer.write("Example Input: " + NEW_LINE + JSON_START +
					GSON.toJson(JsonParser.parseString(jsonObject.get("exampleInput").toString())) + JSON_END + NEW_LINE);
			}
			if (jsonObject.has("exampleOutput")) {
				writer.write("Example Output : " + NEW_LINE + JSON_START +
					GSON.toJson(JsonParser.parseString(jsonObject.get("exampleOutput").toString())) + JSON_END);
			}
			writer.close();
			System.out.println("Successfully generated spec file " + fileName);
		}catch (IOException e){
			throw e;
		}
		finally {
			if(writer != null){
				writer.close();
			}
		}
	}

	private boolean isTestTrigger(String triggerId){
		if(excludeTriggerIdList.contains(triggerId.replace("\"", ""))){
			return true;
		}
		return false;
	}

	private String buildFileName(String triggerId){

		String[] str = triggerId.replaceAll("\"", "").split(":");
		if(str.length == 2){
			return str[0] + "-" + str[1];
		}
		return triggerId.replaceAll("\"", "");

	}

}
