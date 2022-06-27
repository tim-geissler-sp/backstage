/*
 * Copyright (C) 2017 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.util;

import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * A Set implementation backed by a sorted redis set that supports expiring values.
 */
public class RedisExpiringSet {

	private final RedisPool _redisPool;

	private final String _key;

	/**
	 * Constructs a new RedisExpiringSet.
	 *
	 * @param redisPool The pool of redis connections.
	 * @param key The key to use.
	 */
	public RedisExpiringSet(RedisPool redisPool, String key) {
		_redisPool = requireNonNull(redisPool, "redisPool is required");
		_key = requireNonNull(key, "key is required");
	}

	/**
	 * Gets whether or not the set contains the specified value.
	 *
	 * @param value The value to check for.
	 * @return True of the set contains the value, false otherwise.
	 */
	public boolean contains(String value) {
		long timestamp = System.currentTimeMillis();

		return _redisPool.exec(jedis -> {
			Transaction transaction = jedis.multi();
			transaction.zremrangeByScore(_key, "-inf", String.valueOf(timestamp));
			Response<Double> score = transaction.zscore(_key, value);

			transaction.exec();

			return score.get() != null;
		});
	}

	/**
	 * Adds a value to the set.
	 *
	 * @param value The value to add.
	 * @param duration The expiration duration.
	 * @param timeUnit The time unit that the expiration duration is specified in.
	 */
	public void add(String value, long duration, TimeUnit timeUnit) {
		long timestamp = System.currentTimeMillis() + timeUnit.toMillis(duration);
		_redisPool.exec(jedis -> jedis.zadd(_key, (double)timestamp, value));
	}

	/**
	 * Removes a value from the set.
	 *
	 * @param value The value to remove.
	 */
	public void remove(String value) {
		_redisPool.exec(jedis -> jedis.zrem(_key, value));
	}

	/**
	 * Gets all values in the set.
	 *
	 * @return The set of all values.
	 */
	public Set<String> getAll() {
		Set<String> result = new HashSet<>();
		long timestamp = System.currentTimeMillis();

		_redisPool.exec(jedis -> {
			Transaction transaction = jedis.multi();
			transaction.zremrangeByScore(_key, "-inf", String.valueOf(timestamp));
			Response<Set<String>> values = transaction.zrange(_key, 0, -1);

			transaction.exec();

			result.addAll(values.get());
			return null;
		});

		return result;
	}
}

