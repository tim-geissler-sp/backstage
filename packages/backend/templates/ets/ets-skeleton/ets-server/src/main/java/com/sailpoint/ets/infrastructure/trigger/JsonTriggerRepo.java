/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.trigger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.ets.EtsProperties;
import com.sailpoint.ets.domain.trigger.EventSource;
import com.sailpoint.ets.domain.trigger.JsonSchema;
import com.sailpoint.ets.domain.trigger.Trigger;
import com.sailpoint.ets.domain.trigger.TriggerDescription;
import com.sailpoint.ets.domain.trigger.TriggerId;
import com.sailpoint.ets.domain.trigger.TriggerName;
import com.sailpoint.ets.domain.trigger.TriggerRepo;
import com.sailpoint.ets.domain.trigger.TriggerType;
import com.sailpoint.utilities.JsonUtil;
import lombok.Data;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * JSON TriggerRepo implementation. Parses triggers from TRIGGER_JSON_PATH
 * and returns an immutable reference to triggers.
 */
@Component
@CommonsLog
public class JsonTriggerRepo implements TriggerRepo {

	public static final String TRIGGER_SCHEMA_PATH = "/triggers/jsonSchema/";

	private final Map<TriggerId, Trigger> _triggers = new HashMap<>();
	private final Map<String, TriggerId> _eventSourceMapping = new HashMap<>();

	/**
	 * Constructs a new JsonTriggerRepo, parsing the triggers from the classpath.
	 */
	@Autowired
	public JsonTriggerRepo(EtsProperties etsProperties) throws IOException {
		try {
			TriggerDescriptors jsonSchemaDescriptors = getJsonRepositoryForJsonSchema(etsProperties.getJsonTriggersRepoFilePathJsonSchema());

			if (jsonSchemaDescriptors.triggers != null) {
				for (TriggerDescriptor jsonSchemaDescriptor : jsonSchemaDescriptors.triggers) {

					if(jsonSchemaDescriptor.type == null || jsonSchemaDescriptor.type.length() == 0) {
						throw new IllegalStateException("Trigger must have type define");
					}
					if(jsonSchemaDescriptor.name == null || jsonSchemaDescriptor.name.length() == 0) {
						throw new IllegalStateException("Trigger must have name define");
					}

					addTrigger(jsonSchemaDescriptor);
				}
			}
		} catch (Throwable e) {
			log.error("Stop execution: Error during loading triggers repository from json file.", e);
			throw e;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Stream<Trigger> findAll() {
		return _triggers.values().stream();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<Trigger> findById(TriggerId id) {
		return Optional.ofNullable(_triggers.get(id));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<TriggerId> findIdByEventSource(String topic, String type) {
		return Optional.ofNullable(_eventSourceMapping.get(generateEventSourceId(topic, type)));
	}

	/**
	 * Utility class TriggerDescriptor.
	 */
	@Getter
	static class TriggerDescriptor {
		String id;
		String name;
		String type;
		String description;
		List<EventSource> eventSources;
		Map<String, Object> inputSchema;
		Map<String, Object> exampleInput;
		Map<String, Object> outputSchema;
		Map<String, Object> exampleOutput;
	}

	/**
	 *  Utility class TriggerDescriptors.
	 */
	@Data
	static class TriggerDescriptors {
		List<TriggerDescriptor> triggers;
	}

	/**
	 * Parse and validate trigger from descriptor and put it into the trigger map
	 * @param jsonSchemaDescriptor trigger descriptor.
	 */
	private void addTrigger(TriggerDescriptor jsonSchemaDescriptor) {
		TriggerId id = new TriggerId(jsonSchemaDescriptor.id);
		TriggerName name = new TriggerName(jsonSchemaDescriptor.name);
		TriggerType type = TriggerType.valueOf(jsonSchemaDescriptor.type);
		TriggerDescription description = new TriggerDescription(jsonSchemaDescriptor.description);

		Map<String, Object> exampleInput = jsonSchemaDescriptor.exampleInput;
		Map<String, Object> exampleOutput = jsonSchemaDescriptor.exampleOutput;

		//building out schemas
		JsonSchema inputJsonSchema = new JsonSchema(jsonSchemaDescriptor.inputSchema, getJsonSchemaGeneratedDtoClass(jsonSchemaDescriptor, false));

		//validate
		inputJsonSchema.validate(jsonSchemaDescriptor.exampleInput);

		//build trigger
		Trigger.TriggerBuilder builder = Trigger.builder()
			.id(id)
			.name(name)
			.type(type)
			.description(description)
			.inputSchemaObject(inputJsonSchema)
			.inputSchema(jsonSchemaDescriptor.inputSchema)
			.exampleInput(exampleInput);

		// Conditionally add output schema and example base on trigger type
		if (type == TriggerType.REQUEST_RESPONSE) {
			//building out schema
			JsonSchema outputJsonSchema = new JsonSchema(requireNonNull(jsonSchemaDescriptor.outputSchema, "output schema is required"), getJsonSchemaGeneratedDtoClass(jsonSchemaDescriptor, true));

			//Validate
			outputJsonSchema.validateData(requireNonNull(jsonSchemaDescriptor.exampleOutput, "example output is required"));

			builder
				.outputSchemaObject(outputJsonSchema)
				.outputSchema(jsonSchemaDescriptor.outputSchema)
				.exampleOutput(exampleOutput);

		} else if (type == TriggerType.FIRE_AND_FORGET) {
			if(jsonSchemaDescriptor.outputSchema != null || jsonSchemaDescriptor.exampleOutput !=null) {
				throw new IllegalStateException("Fire and Forget Trigger should not have output schema define");
			}
		}

		// Add event sources if provided
		if (jsonSchemaDescriptor.eventSources != null) {

			if (type == TriggerType.REQUEST_RESPONSE) {
				throw new IllegalStateException("request response based trigger cannot have event sources");
			}
			for (EventSource es: jsonSchemaDescriptor.eventSources) {
				IdnTopic.valueOf(es.getTopic());
				requireNonNull(es.getEventType(), "event type is required");

				_eventSourceMapping.put(generateEventSourceId(es.getTopic(), es.getEventType()), id);
			}
			builder.eventSources(jsonSchemaDescriptor.eventSources);
		}

		_triggers.put(id, builder.build());
	}

	private String getJsonSchemaGeneratedDtoClass(TriggerDescriptor descriptor, boolean isOutputSchema){
		String dtoClassPrefix = "com.sailpoint.ets.domain.trigger.schemaGeneratedDto.";

		if (isOutputSchema){
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

	/**
	 * Load json repo from internal directory or external file.
	 * @param externalRepository name of external file.
	 * @return TriggerDescriptors
	 * @throws IOException
	 */
	@VisibleForTesting
	TriggerDescriptors getJsonRepositoryForJsonSchema(String externalRepository) throws IOException {
		try {
			if (externalRepository == null || externalRepository.length() == 0) {
				return getJsonFromDir(TRIGGER_SCHEMA_PATH);
			} else {
				return JsonUtil.parse(TriggerDescriptors.class, readRepo(externalRepository));
			}
		} catch (Exception e) {
			log.error("Error reading triggers repository from file." , e);
			throw e;
		}
	}


	/**
	 * Iterate over files in directory where json trigger files reside.
	 * @param path - directory path where the trigger json files reside .
	 * @return TriggerDescriptors
	 * @throws IOException
	 */
	private TriggerDescriptors getJsonFromDir(String path) throws IOException {

		TriggerDescriptors triggerDescriptors = new TriggerDescriptors();
		List<TriggerDescriptor> list = new ArrayList<>();

		Resource[] resources = getResources();
		if (path.contains("json")){
			resources = getJsonResources();
		}


		for(Resource r : resources){
			try {
				TriggerDescriptor triggerDescriptor = getJsonFromResource(r);
				list.add(triggerDescriptor);
				log.info("Successfully read " + r.getFilename());
			}catch(Exception e){
				log.error("could not read trigger file at path : " + r.getFile().getPath(), e);
			}
		}
		triggerDescriptors.setTriggers(list);

		return triggerDescriptors;
	}


	/**
	 * reads and parses the json trigger file read as resource.
	 * @param resource - name of the resource to be read.
	 * @return TriggerDescriptor
	 */
	private TriggerDescriptor getJsonFromResource(Resource resource) throws IOException {
		log.info("Going to read: " + resource.getFilename());
		return JsonUtil.parse(TriggerDescriptor.class,
			 resource.getInputStream());
	}


	/**
	 * loads all the *.json files from the from the resources folder.
	 * @return TriggerDescriptor
	 */
	public Resource[] getResources() throws IOException {
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
		return resolver.getResources("classpath:triggers/*.json");
	}

	public Resource[] getJsonResources() throws IOException {
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
		return resolver.getResources("classpath:triggers/jsonSchema/*.json");
	}


	/**
	 * Read external JSON file, parses it and returns its content.
	 *
	 * @param repoLocation JSON file location.
	 * @return String JSON file content.
	 * @throws IOException
	 */
	private String readRepo(String repoLocation) throws IOException {
		StringBuilder contentBuilder = new StringBuilder();
		Files.readLines(Paths.get(repoLocation).toFile(), Charsets.UTF_8)
			.forEach(s -> contentBuilder.append(s).append("\n"));
		return contentBuilder.toString();
	}

	/**
	 * Generate ID for an event source.
	 * @param topic the name of the topic
	 * @param eventType the type of the event
	 * @return an ID for the event source
	 */
	private String generateEventSourceId(String topic, String eventType) {
		return (topic + "#" + eventType).toUpperCase();
	}
}
