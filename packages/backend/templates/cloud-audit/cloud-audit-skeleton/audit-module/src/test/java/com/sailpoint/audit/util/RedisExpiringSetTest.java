/*
 * Copyright (C) 2017 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.util;

import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for RedisExpiringSet
 */
public class RedisExpiringSetTest extends BaseRedisTest {

	RedisExpiringSet _set;

	@Before
	public void setUp() {
		_redisPool.exec(Jedis::flushDB);
		_set = new RedisExpiringSet(_redisPool, "testKey");
	}

	@Test
	public void initiallyEmpty() {
		assertTrue(_set.getAll().isEmpty());
	}

	@Test
	public void addIsImmediatelyReflected() {
		_set.add("testValue", 5, TimeUnit.MINUTES);

		assertTrue(_set.contains("testValue"));
		assertTrue(_set.getAll().contains("testValue"));
	}

	@Test
	public void expirationWorks() throws InterruptedException {
		_set.add("shouldExpire", 2, TimeUnit.SECONDS);
		assertTrue(_set.contains("shouldExpire"));

		Thread.sleep(3000);

		assertFalse(_set.contains("shouldExpire"));
	}

	@Test
	public void remove() {
		_set.add("testValue", 5, TimeUnit.MINUTES);

		assertTrue(_set.contains("testValue"));

		_set.remove("testValue");

		assertFalse(_set.contains("testValue"));
	}
}
