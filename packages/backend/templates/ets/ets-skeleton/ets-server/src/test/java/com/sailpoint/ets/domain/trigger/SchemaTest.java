package com.sailpoint.ets.domain.trigger;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(MockitoJUnitRunner.class)
public class SchemaTest {

	@Test
	public void testBasicSchemaShouldValidate() {
		try {
			String exampleInput = "{\"testStringKey\": \"testStringValue\",\"testArrayKey\": [\"testArrayValue0\"],\"testNestedPropertyKey1\": {\"testNestedPropertyKey2\": \"testNestedProperty2Value\"}}";
			String exampleSchema = "{\"definitions\" : {\"record:TestObject\" : {\"type\" : \"object\",\"required\" : [ \"testArrayKey\", \"testNestedPropertyKey1\", \"testStringKey\" ],\"additionalProperties\" : true,\"properties\" : {\"testArrayKey\" : {\"default\" : null,\"oneOf\" : [ {\"type\" : \"null\"}, {\"type\" : \"array\",\"items\" : {\"oneOf\" : [ {\"type\" : \"null\"}, {\"type\" : \"string\"} ]}} ]},\"testNestedPropertyKey1\" : {\"default\" : null,\"oneOf\" : [ {\"type\" : \"null\"}, {\"$ref\" : \"#/definitions/record:TestNestedPropertyKey1\"} ]},\"testStringKey\" : {\"default\" : null,\"oneOf\" : [ {\"type\" : \"null\"}, {\"type\" : \"string\"} ]}}},\"record:TestNestedPropertyKey1\" : {\"type\" : \"object\",\"required\" : [ \"testNestedPropertyKey2\" ],\"additionalProperties\" : true,\"properties\" : {\"testNestedPropertyKey2\" : {\"default\" : null,\"oneOf\" : [ {\"type\" : \"null\"}, {\"type\" : \"string\"} ]}}}},\"$ref\" : \"#/definitions/record:TestObject\"}";
			org.everit.json.schema.Schema schema = SchemaLoader.load(new JSONObject(new JSONTokener(exampleSchema)));
			schema.validate(new JSONObject(new JSONTokener(exampleInput)));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error thrown");
			assertTrue(false);
		}

	}

	@Test
	public void testBasicSchemaShouldValidateWithExtra() {
		try {
			String exampleInput = "{\"testStringKey\": \"testStringValue\",\"extraTestStringKey\": \"extraTestStringKeyValue\",\"testArrayKey\": [\"testArrayValue0\"],\"testNestedPropertyKey1\": {\"testNestedPropertyKey2\": \"testNestedProperty2Value\",\"extraTestNestedStringKey\": \"extraTestNestedStringKeyValue\",}}";
			String exampleSchema = "{\"definitions\" : {\"record:TestObject\" : {\"type\" : \"object\",\"required\" : [ \"testArrayKey\", \"testNestedPropertyKey1\", \"testStringKey\" ],\"additionalProperties\" : true,\"properties\" : {\"testArrayKey\" : {\"default\" : null,\"oneOf\" : [ {\"type\" : \"null\"}, {\"type\" : \"array\",\"items\" : {\"oneOf\" : [ {\"type\" : \"null\"}, {\"type\" : \"string\"} ]}} ]},\"testNestedPropertyKey1\" : {\"default\" : null,\"oneOf\" : [ {\"type\" : \"null\"}, {\"$ref\" : \"#/definitions/record:TestNestedPropertyKey1\"} ]},\"testStringKey\" : {\"default\" : null,\"oneOf\" : [ {\"type\" : \"null\"}, {\"type\" : \"string\"} ]}}},\"record:TestNestedPropertyKey1\" : {\"type\" : \"object\",\"required\" : [ \"testNestedPropertyKey2\" ],\"additionalProperties\" : true,\"properties\" : {\"testNestedPropertyKey2\" : {\"default\" : null,\"oneOf\" : [ {\"type\" : \"null\"}, {\"type\" : \"string\"} ]}}}},\"$ref\" : \"#/definitions/record:TestObject\"}";
			org.everit.json.schema.Schema schema = SchemaLoader.load(new JSONObject(new JSONTokener(exampleSchema)));
			schema.validate(new JSONObject(new JSONTokener(exampleInput)));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error thrown");
			assertTrue(false);
		}
	}

	@Test
	public void testBasicSchemaShouldValidateWithOptional() {
		String exampleSchema = "{\"definitions\" : {\"record:TestObject\" : {\"type\" : \"object\",\"required\" : [ \"testArrayKey\", \"testNestedPropertyKey1\", \"testStringKey\" ],\"optional\": [\"extraTestStringKey\"],\"additionalProperties\" : false,\"properties\" : {\"extraTestStringKey\" : {\"default\" : null,\"oneOf\" : [ {\"type\" : \"null\"}, {\"type\" : \"string\"} ]},\"testArrayKey\" : {\"default\" : null,\"oneOf\" : [ {\"type\" : \"null\"}, {\"type\" : \"array\",\"items\" : {\"oneOf\" : [ {\"type\" : \"null\"}, {\"type\" : \"string\"} ]}} ]},\"testNestedPropertyKey1\" : {\"default\" : null,\"oneOf\" : [ {\"type\" : \"null\"}, {\"$ref\" : \"#/definitions/record:TestNestedPropertyKey1\"} ]},\"testStringKey\" : {\"default\" : null,\"oneOf\" : [ {\"type\" : \"null\"}, {\"type\" : \"string\"} ]}}},\"record:TestNestedPropertyKey1\" : {\"type\" : \"object\",\"required\" : [\"testNestedPropertyKey2\" ],\"optional\": [\"extraTestNestedStringKey\"],\"additionalProperties\" : false,\"properties\" : {\"extraTestNestedStringKey\" : {\"default\" : null,\"oneOf\" : [ {\"type\" : \"null\"}, {\"type\" : \"string\"} ]},\"testNestedPropertyKey2\" : {\"default\" : null,\"oneOf\" : [ {\"type\" : \"null\"}, {\"type\" : \"string\"} ]}}}},\"$ref\" : \"#/definitions/record:TestObject\"}";
		org.everit.json.schema.Schema schema = SchemaLoader.load(new JSONObject(new JSONTokener(exampleSchema)));

		try {
			String inputWithAllRequiredAndOptional = "{\"testStringKey\": \"testStringValue\",\"extraTestStringKey\": \"extraTestStringKeyValue\",\"testArrayKey\": [\"testArrayValue0\"],\"testNestedPropertyKey1\": {\"testNestedPropertyKey2\": \"testNestedProperty2Value\",\"extraTestNestedStringKey\": \"extraTestStringKeyValue\"}}";
			String inputWithRootOptional = "{\"testStringKey\": \"testStringValue\",\"extraTestStringKey\": \"extraTestStringKeyValue\",\"testArrayKey\": [\"testArrayValue0\"],\"testNestedPropertyKey1\": {\"testNestedPropertyKey2\": \"testNestedProperty2Value\"}}";
			String inputWithNestedOptional = "{\"testStringKey\": \"testStringValue\",\"testArrayKey\": [\"testArrayValue0\"],\"testNestedPropertyKey1\": {\"testNestedPropertyKey2\": \"testNestedProperty2Value\",\"extraTestNestedStringKey\": \"extraTestStringKeyValue\"}}";
			String inputWithNoOptional = "{\"testStringKey\": \"testStringValue\",\"testArrayKey\": [\"testArrayValue0\"],\"testNestedPropertyKey1\": {\"testNestedPropertyKey2\": \"testNestedProperty2Value\"}}";

			schema.validate(new JSONObject(new JSONTokener(inputWithAllRequiredAndOptional)));
			schema.validate(new JSONObject(new JSONTokener(inputWithRootOptional)));
			schema.validate(new JSONObject(new JSONTokener(inputWithNestedOptional)));
			schema.validate(new JSONObject(new JSONTokener(inputWithNoOptional)));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error thrown");
			assertTrue(false);
		}

	}


	@Test
	public void testBasicSchemaShouldValidateAndDropExtraKeysWhenParsedToDTO() {
		try {
			String exampleInput = "{\"testStringKey\": \"testStringValue\",\"extraTestStringKey\": \"extraTestStringKeyValue\",\"testArrayKey\": [\"testArrayValue0\"],\"testNestedPropertyKey1\": {\"testNestedPropertyKey2\": \"testNestedProperty2Value\",\"extraTestNestedStringKey\": \"extraTestNestedStringKeyValue\"}}";
			String exampleSchema = "{\"definitions\": {\"record:TestObject\": {\"type\": \"object\",\"required\": [\"testArrayKey\",\"testNestedPropertyKey1\",\"testStringKey\"],\"additionalProperties\": true,\"properties\": {\"testArrayKey\": {\"type\": \"array\",\"items\": {\"oneOf\": [{\"type\": \"null\"},{\"type\": \"string\"}]}},\"testNestedPropertyKey1\": {\"$ref\": \"#/definitions/record:TestNestedPropertyKey1\"},\"testStringKey\": {\"type\": \"string\"}}},\"record:TestNestedPropertyKey1\": {\"type\": \"object\",\"required\": [\"testNestedPropertyKey2\"],\"additionalProperties\": true,\"properties\": {\"testNestedPropertyKey2\": {\"type\": \"string\"}}}},\"$ref\": \"#/definitions/record:TestObject\"}";
			org.everit.json.schema.Schema schema = SchemaLoader.load(new JSONObject(new JSONTokener(exampleSchema)));
			schema.validate(new JSONObject(new JSONTokener(exampleInput)));


			ObjectMapper mapper = new ObjectMapper();
			JavaType valueType = mapper.constructType(Class.forName("com.sailpoint.ets.domain.trigger.RecordTestObject"));
			Object recordTestObject = mapper.readValue(exampleInput, valueType);

			assertTrue(((RecordTestObject) recordTestObject).getTestStringKey().equals("testStringValue"));
			assertTrue(!recordTestObject.toString().contains("extraTestStringKey"));
			assertTrue(!recordTestObject.toString().contains("extraTestNestedStringKey"));

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error thrown");
			assertTrue(false);
		}
	}

}
