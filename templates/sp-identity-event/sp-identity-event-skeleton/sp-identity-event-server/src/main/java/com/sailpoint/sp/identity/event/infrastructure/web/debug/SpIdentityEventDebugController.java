/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.infrastructure.web.debug;

import com.google.gson.JsonObject;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.client.EventPublisher;
import com.sailpoint.iris.client.Topic;
import com.sailpoint.utilities.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static com.sailpoint.sp.identity.event.infrastructure.IdentityEventHandlers.DEBUG_EVENT_HEADER;
import static com.sailpoint.sp.identity.event.infrastructure.redis.debug.RedisIdentityEventPublisher.getRedisKey;

/**
 * Controller for sp-identity-event that includes debug endpoints for e2e tests
 */
@RestController
@RequiredArgsConstructor(onConstructor_={@Autowired})
@RequestMapping(value = "/debug")
@CommonsLog
public class SpIdentityEventDebugController {

	private final EventPublisher _irisEventPublisher;
	private final RedisPool _redisPool;

	@PreAuthorize("hasRole('sp:identity-event-debug:read')")
	@GetMapping("/redis/retrieve/{type}/{id}")
	public ResponseEntity retrieveKey(@PathVariable("type") String type, @PathVariable("id") String id) {
		String response = retrieveRedis(type, id);
		if (response == null) {
			return ResponseEntity.notFound().build();
		} else {
			return ResponseEntity.ok(response);
		}
	}

	@PreAuthorize("hasRole('sp:identity-event-debug:write')")
	@PostMapping("/publish-event/{type}")
	public ResponseEntity publishDebugEvent(@PathVariable("type") String type, @RequestParam(name="created", required=false) String created, @RequestBody Map<String, Object> eventContent) {
		switch(type.toLowerCase()) {
			case "org_deleted":
				publishOrgDeleted();
				break;
			default:
				publishIdentityEvent(type, created, eventContent);
		}

		return ResponseEntity.noContent().build();
	}

	/**
	 * Retrieves and removes the identity-event output from debug store.
	 *
	 * @param type the type of identity-event event
	 * @param id the id of the identity
	 * @return the value
	 */
	private String retrieveRedis(String type, String id) {
		return _redisPool.exec(jedis -> {
			String result = jedis.get(getRedisKey(type, id));
			if (result != null) {
				jedis.del(getRedisKey(type, id));
			}
			log.info("Retrieved key: " + getRedisKey(type, id) + " from redis with value: " + result);
			return result;
		});
	}

	/**
	 * Publishes a debug event to facilitate debugging and E2E tests
	 *
	 * @param type The type of event to publish
	 * @param created Optionally overwrite the created time of the event
	 * @param eventContent The content of the event
	 */
	private void publishIdentityEvent(String type, String created, Map<String, Object> eventContent) {
		RequestContext rc = RequestContext.ensureGet();

		Map headerMap = new HashMap<String, String>();
		headerMap.put(EventHeaders.POD, rc.ensurePod());
		headerMap.put(EventHeaders.ORG, rc.ensureOrg());
		headerMap.put(EventHeaders.REQUEST_ID, rc.getId());
		rc.getTenantId().ifPresent(tenantId -> headerMap.put(EventHeaders.TENANT_ID, tenantId));
		headerMap.put(DEBUG_EVENT_HEADER, "");
		headerMap.put(EventHeaders.GROUP_ID, "sp-identity-event");

		Event event = EventBuilder.withTypeAndContentJson(type, JsonUtil.toJson(eventContent))
			.addHeaders(headerMap)
			.build();

		if (created != null) {
			event = mutateEventCreatedTime(event, created);
		}

		String topicId = Topic.buildId(IdnTopic.IDENTITY.getName(), rc.ensurePod());

		_irisEventPublisher.publish(event, Topic.parse(topicId));
	}

	/**
	 * Publishes a debug org deleted event to facilitate debugging and E2E tests
	 */
	private void publishOrgDeleted() {
		RequestContext rc = RequestContext.ensureGet();

		Map headerMap = new HashMap<String, String>();
		headerMap.put(EventHeaders.POD, rc.ensurePod());
		headerMap.put(EventHeaders.ORG, rc.ensureOrg());
		headerMap.put(EventHeaders.REQUEST_ID, rc.getId());
		rc.getTenantId().ifPresent(tenantId -> headerMap.put(EventHeaders.TENANT_ID, tenantId));
		headerMap.put(DEBUG_EVENT_HEADER, "");
		headerMap.put(EventHeaders.GROUP_ID, "sp-identity-event");

		Event event = EventBuilder.withTypeAndContentJson("ORG_DELETED", JsonUtil.toJson(null))
			.addHeaders(headerMap)
			.build();

		String topicId = Topic.buildId(IdnTopic.ORG_LIFECYCLE.getName(), rc.ensurePod());

		_irisEventPublisher.publish(event, Topic.parse(topicId));
	}

	/**
	 * Serializes the event to json, changes timestamp and deserializes back to Event using same Util that our events use
	 * @param event The event
	 * @param created The timestamp
	 * @return The mutated event
	 */
	private Event mutateEventCreatedTime(Event event, String created) {
		Instant timestamp = Instant.parse(created);
		JsonObject eventJson = JsonUtil.parse(JsonObject.class, JsonUtil.toJson(event));
		eventJson.addProperty("timestamp", timestamp.toString());
		return JsonUtil.getDefaultGson().fromJson(eventJson, Event.class);
	}
}
