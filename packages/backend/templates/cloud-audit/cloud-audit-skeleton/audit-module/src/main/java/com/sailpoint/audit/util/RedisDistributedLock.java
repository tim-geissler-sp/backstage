/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.util;

import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;
import java.util.UUID;

/**
 * Distributed lock implementation with <b>single instance</b> Redis.
 * <p>
 * This lock is used for mutual exclusion across multiple processes, such as among instances in a service cluster
 * or across distinct services sharing common resource.
 * <p>
 * As the name implies, only one client can hold the lock at any given time. The lock is "signed" with a random
 * string and thus only the client that had acquired the lock can release the lock. To prevent deadlock when a client
 * dies while holding the lock, the lock is automatically released after specified TTL.
 * <p>
 * Reference: <a>https://redis.io/topics/distlock</a>
 * <p/>
 * Example usage:
 * <pre>{@code
 * RedisDistributedLock lock = new RedisDistributedLock(redisPool, someKey, 2000);
 * if (lock.acquire()) {
 *     try {
 *         // Critical section
 *     } finally {
 *         lock.release();
 *     }
 * }
 * }</pre>
 */
public class RedisDistributedLock {

	private static final String LOCK_RELEASE_SCRIPT =
			"if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
	private static final String OK = "OK";
	private static final Long ONE = 1L;

	private final RedisPool _redisPool;
	private final String _lockKey;
	private final long _lockTTL;

	private String _lockValue;

	public RedisDistributedLock(RedisPool redisPool, String lockKey, long lockTTL) {
		_redisPool = redisPool;
		_lockKey = lockKey;
		_lockTTL = lockTTL;
	}

	/**
	 * Acquires the distributed lock. This method tries to acquire the lock just once and does NOT block the thread.
	 * <p>
	 * Set the key only if it does not already exist (NX option), with an expire of _lockTTL (PX option).
	 * The key is paired to a random UUID value. This value must be unique across all clients and all lock requests.
	 * <p>
	 * Equivalent to <code>SET lock_key random_value NX PX lock_ttl</code>
	 *
	 * @return true if successfully acquired the lock, false otherwise.
	 */
	public boolean acquireLock() {
		final String newLockValue = UUID.randomUUID().toString();

		final String statusCode =
				_redisPool.exec(
						jedis ->
								jedis.set(
										_lockKey,
										newLockValue,
										SetParams.setParams().nx().px(_lockTTL)
								)
				);

		final boolean isSuccess = OK.equals(statusCode);

		if (isSuccess) {
			_lockValue = newLockValue;
		}

		return isSuccess;
	}

	/**
	 * Releases the distributed lock.
	 * <p>
	 * Remove the key only if it exists and the value stored at the key is exactly the one used to acquire the lock.
	 *
	 * @return true if successfully released the lock, false otherwise.
	 */
	public boolean releaseLock() {
		return _redisPool.exec(
				jedis -> {
					Object result =
							jedis.eval(
									LOCK_RELEASE_SCRIPT,
									Collections.singletonList(_lockKey),
									Collections.singletonList(_lockValue)
							);
					return ONE.equals(result);
				}
		);
	}

}