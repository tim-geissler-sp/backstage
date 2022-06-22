/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.web.debug;

import com.google.common.collect.ImmutableMap;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import com.sailpoint.utilities.JsonUtil;

import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.client.EventPublisher;
import com.sailpoint.iris.client.Topic;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.params.SetParams;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Controller for ETS that included debug endpoints which can be used to verify ETS functionality during E2E tests.
 */
@RestController
@RequiredArgsConstructor(onConstructor_={@Autowired})
@RequestMapping(value = "/debug")
@CommonsLog
public class EtsDebugController {

	private final RedisPool _redisPool;
	private final EventPublisher _irisEventPublisher;

	@PostMapping(value = "/invocations/success",
		consumes="application/json",
		produces = "application/json")
	public ResponseEntity postInvocation(@RequestBody Map<String, Object> invocation,
										 @RequestHeader HttpHeaders headers,
										 @RequestParam(value = "redisKey", required = false) String redisKey) {
		Map<String, Object> metaData = (Map<String, Object>) invocation.get("_metadata");
		String invocationId = "";
		String callbackURL = "";
		String secret = "";

		if (metaData != null) {
			invocationId = (String) metaData.get("invocationId");
			callbackURL = (String) metaData.get("callbackURL");
			secret = (String) metaData.get("secret");
		}
		log.info("Debug info invocationId " + invocationId + " callbackURL " + callbackURL + " secret " + secret);
		String authorizationHeader = (headers.get("Authorization") != null && headers.get("Authorization").size() > 0)
			? headers.get("Authorization").get(0): "";
		invocation.put("authorization", authorizationHeader);
		//handle case for fire and forget trigger.
		if(invocationId != null) {
			if (redisKey == null) {
				writeToRedis(invocationId, JsonUtil.toJson(invocation));
			} else {
				writeToRedis(redisKey, JsonUtil.toJson(invocation));
			}
		}
		return ResponseEntity.ok("{}");
	}

	@PreAuthorize("hasRole('idn:trigger-service-debug:read')")
	@GetMapping("/redis/retrieve/{key}")
	public ResponseEntity retrieveKey(@PathVariable("key") String key) {
		return ResponseEntity.ok(retrieveFromRedis(key));
	}

	@PreAuthorize("hasRole('idn:trigger-service-debug:create')")
	@PostMapping("/publish-event")
	public ResponseEntity publishTestEvent(@RequestBody Map<String, Object> eventContent) {
		RequestContext rc = RequestContext.ensureGet();

		Event event = EventBuilder.withTypeAndContentJson("ETS_INTERNAL_TEST", JsonUtil.toJson(eventContent))
			.addHeaders(ImmutableMap.of(EventHeaders.POD, rc.ensurePod(),
				EventHeaders.ORG, rc.ensureOrg(), EventHeaders.REQUEST_ID, rc.getId()))
			.build();
		String topicId = Topic.buildId(IdnTopic.INTERNAL_TEST.getName(), rc.ensurePod());

		_irisEventPublisher.publish(event, Topic.parse(topicId));
		return ResponseEntity.ok(eventContent);
	}

	/**
	 * Retrieves the value for the given key from Redis.
	 * @param key redis key
	 * @return value
	 */
	private String retrieveFromRedis(String key) {
		return _redisPool.exec(jedis -> {
			String result = jedis.get(key);
			log.info("Retrieved key: " + key + " from redis with value: " + result);
			return result;
		});
	}

	/**
	 * Write the key/value to Redis
	 * @param key redis key
	 * @param value value
	 * @return Result of jedis set which is a status code reply
	 */
	private String writeToRedis(String key, String value) {
		return _redisPool.exec(jedis -> {
			String result = jedis.set(key, value, SetParams.setParams().nx().ex((int)TimeUnit.SECONDS.toSeconds(60)));
			log.info("Result of writing " + value + " to redis with key " + key + " : " + result);
			return result;
		});
	}

}
