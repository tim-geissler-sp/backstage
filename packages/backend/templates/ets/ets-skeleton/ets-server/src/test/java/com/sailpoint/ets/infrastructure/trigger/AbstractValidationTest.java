/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.trigger;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Map;

/**
 * Abstract class with json schema validation methods
 */
public abstract class AbstractValidationTest {

	/**
	 * Validate specified JSON against its json schema.
	 *
	 * @param json   JSON to validate
	 * @param schema json schema as {@link Schema}
	 * @return JSON decoded to GenericRecord
	 * @throws Exception if validation fails
	 */
	protected Object validateJson(String json, Map<String, Object> schema, String dtoClassPath) throws Exception {
		Schema jsonSchema = SchemaLoader.load(new JSONObject(new JSONTokener(new Gson().toJsonTree(schema).getAsJsonObject().toString())));
		jsonSchema.validate(new JSONObject(json));

		ObjectMapper mapper = new ObjectMapper();
		JavaType valueType = mapper.constructType(Class.forName(dtoClassPath));
		return mapper.readValue(json, valueType);
	}
}
