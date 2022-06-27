/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.event.util.debug;

import com.google.inject.Provider;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.client.TopicDescriptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.params.SetParams;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 *  Base Debug service for notifications.
 */
public class NotificationDebugService {

	private static final Log _log = LogFactory.getLog(NotificationDebugService.class);

	private Provider<EventService> _eventService;

	private Provider<RedisPool> _redisPoolProvider;

	protected Map<String, String> _headerMap;

	public NotificationDebugService(Provider<EventService> eventService, Provider<RedisPool> redisPoolProvider) {
		_eventService = eventService;
		_redisPoolProvider = redisPoolProvider;
	}

	/**
	 * Publishes any json event to Kafka with Debug header.
	 *
	 * @param topicDescriptor The topic descriptor.
	 * @param eventType event type
	 * @param eventContext event context as string
	 * @param debugHeader debug header
	 */
	protected String publishKafkaEvent(TopicDescriptor topicDescriptor, String eventType, String eventContext, String debugHeader) {
		String uniqueKey = "test" + UUID.randomUUID();
		_log.info("Publish in Kafka with key " + uniqueKey);

		_headerMap = new HashMap<>();
		_headerMap.put(debugHeader, uniqueKey);
		_headerMap.put(EventHeaders.GROUP_ID, "hermes");

		Event event = getEvent(eventType, eventContext);

		_eventService.get().publishAsync(topicDescriptor, event);

		_log.info("Published event type " + eventType + " in topic " + topicDescriptor.getName() + " with key " + uniqueKey);
		return uniqueKey;
	}

	protected Event getEvent(String eventType, String eventContext) {
		return EventBuilder.withTypeAndContentJson(eventType, eventContext)
				.addHeaders(getEventHeaders())
				.build();
	}

	protected  Map<String, String> getEventHeaders() {
		return _headerMap;
	}

	/**
	 * Write the key/value to Redis
	 * @param key redis key
	 * @param value redis value
	 * @return Result of jedis set which is a status code reply
	 */
	public String writeToStore(String key, String value) {
		return _redisPoolProvider.get().exec(jedis -> {
			String result = jedis.set(key, value, SetParams.setParams().nx().ex(60));
			_log.info("Result of writing to store " + result + "for key " + key + " value " + value);
			return result;
		});
	}
}
