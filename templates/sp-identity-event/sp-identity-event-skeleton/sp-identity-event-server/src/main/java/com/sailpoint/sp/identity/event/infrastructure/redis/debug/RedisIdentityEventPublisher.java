package com.sailpoint.sp.identity.event.infrastructure.redis.debug;

import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import com.sailpoint.sp.identity.event.domain.IdentityEventPublisher;
import com.sailpoint.sp.identity.event.domain.event.IdentityAccountAttributesChangedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAccountCorrelatedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAccountUncorrelatedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityEvent;
import com.sailpoint.utilities.JsonUtil;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import redis.clients.jedis.params.SetParams;

/**
 * Publishes the identity event to Redis for a minute for debug purposes
 */
@RequiredArgsConstructor
@CommonsLog
public class RedisIdentityEventPublisher implements IdentityEventPublisher {

	/**
	 * This component writes output of sp-identity-event to redis for debugging.
	 * All keys will include this prefix for security.
	 */
	private static final String REDIS_DEBUG_KEY_PREFX = "sp-identity-event-debug-";

	private final RedisPool _redisPool;

	@Override
	public void publish(IdentityEvent event, OffsetDateTime identityChangedTimestamp) {
		writeToRedis(event.getClass().getSimpleName(), getId(event), JsonUtil.toJson(event));
	}

	private String getId(IdentityEvent event) {
		String id = event.getIdentity().getId();

		switch (event.getClass().getSimpleName()) {
			case "IdentityAccountCorrelatedEvent" :
				id = id + "-" + ((IdentityAccountCorrelatedEvent) event).getAccount().getId();
				break;
			case "IdentityAccountUncorrelatedEvent" :
				id = id + "-" + ((IdentityAccountUncorrelatedEvent) event).getAccount().getId();
				break;
			case "IdentityAccountAttributesChangedEvent" :
				id = id + "-" + ((IdentityAccountAttributesChangedEvent) event).getAccount().getId();
				break;
		}

		log.info(this.getClass().getSimpleName() + " publishing event of type " + event.getClass().getSimpleName() + " with id " + id);

		return id;
	}

	/**
	 * Writes the identity-event event output to Redis
	 *
	 * @param type the type of identity-event event
	 * @param id the id of the identity
	 * @param payload the json payload
	 * @return
	 */
	private String writeToRedis(String type, String id, String payload) {
		return _redisPool.exec(jedis -> {
			String result = jedis.set(getRedisKey(type, id), payload, SetParams.setParams().nx().ex(60));
			log.info("Result of writing " + payload + " to redis with key " + getRedisKey(type, id) + " : " + result);
			return result;
		});
	}

	/**
	 * Gets the redis key based on event type and identity id
	 * @param type the identity-event event type
	 * @param id the id of the identity
	 * @return
	 */
	public static String getRedisKey(String type, String id) {
		return REDIS_DEBUG_KEY_PREFX + type + "-" + id;
	}
}
