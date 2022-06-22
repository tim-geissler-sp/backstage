/*
 * Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.trigger;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.ets.exception.ValidationException;
import com.sailpoint.utilities.JsonUtil;
import io.prometheus.client.Counter;
import lombok.extern.apachecommons.CommonsLog;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import com.sailpoint.metrics.prometheus.PrometheusMetricsUtils;


import java.util.Map;

@CommonsLog
public class JsonSchema implements Schema {
	public final org.everit.json.schema.Schema _jsonSchema;
	private final String _dtoClassPath;
	static final String[] labelKeys = {"json_schema", "org", "pod", "success"};
	public static final String METRIC_NAME = "json_schema_trigger_validation";
	public static final String desc = "Exception thrown while parsing ets trigger payload as with json schema";


	public JsonSchema(Map<String, Object> schema, String dtoClassPath) {
		_dtoClassPath = dtoClassPath;
		_jsonSchema = SchemaLoader.load(new JSONObject(new JSONTokener(new Gson().toJsonTree(schema).getAsJsonObject().toString())));
	}

	/**
	 * Validate the input data with json schema.
	 * @param input the data to be validated.
	 */
	public void validateData(Map<String, Object> input) {
		validate(input);
	}

	public void validateSchema(Map<String, Object> input) {
		_jsonSchema.validate(new JSONObject(input));
	}

	/**
	 * Process the input data with json schema. Two step process:
	 * First perform validation and second remove all data not define in json schema.
	 * @param input the data for processing.
	 */
	public void processData(Map<String, Object> input) {
		try {
			//validate schema against input
			_jsonSchema.validate(new JSONObject(input));

			//parse input into dto to drop out sensitives
			Object dto = validate(input);

			//parse back to map to reset the input
			ObjectMapper mapper = new ObjectMapper();
			input.clear();
			input.putAll(mapper.convertValue(dto, Map.class));
			logValidationException(true);

		} catch (Exception e) {
			logValidationException(false);
			throw new ValidationException(_jsonSchema.getId(), JsonUtil.toJson(input), e);
		}
	}

	public void logValidationException(boolean didParse) {
		String[] labelVals = {
			_dtoClassPath.replace("com.sailpoint.ets.domain.trigger.schemaGeneratedDto.", "").split("\\.")[0],
			RequestContext.get().map(RequestContext::getOrg).get(),
			RequestContext.get().map(RequestContext::getPod).get(),
			String.valueOf(didParse)

		};

		try {
			Counter counter = PrometheusMetricsUtils.getOrAddCounter(METRIC_NAME, desc, labelKeys);
			counter.labels(labelVals).inc();
		} catch (Exception e) {
			log.error(String.format(
				"Unable to inc counter metric: %s labels:[%s] for record %s :",
				METRIC_NAME,
				String.join(", ", labelKeys),
				String.join(", ", labelVals)
			), e);
		}


	}

	/**
	 * Validate input map.
	 * @param input input map.
	 * @return GenericRecord.
	 */
	public Object validate(Map<String, Object> input) {
		// NOTE: this doesn't actually do any validation and needs a refactor --ESR 12/17/2021
		String inputJson = JsonUtil.toJson(input);
		try {
			ObjectMapper mapper = new ObjectMapper();
			JavaType valueType = mapper.constructType(Class.forName(_dtoClassPath));
			return mapper.readValue(inputJson, valueType);

		} catch (Exception e) {
			throw new ValidationException(_jsonSchema.getTitle(), inputJson, e);
		}
	}

	@Override
	public String toString() {
		return _jsonSchema.getId();
	}
}
