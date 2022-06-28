/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link RedisDistributedLock}
 */
public class RedisDistributedLockTest extends BaseRedisTest {

	private static ScheduledExecutorService _threadPool;

	private RedisDistributedLock _lock;

	@Before
	public void setUp() throws Exception {
		_threadPool = Executors.newScheduledThreadPool(2);
		_redisPool.exec(Jedis::flushDB);
		_lock = new RedisDistributedLock(_redisPool, "testKey", Duration.ofSeconds(3).toMillis());
	}

	@After
	public void tearDown() throws Exception {
		_threadPool.shutdownNow();
	}

	@Test
	public void onlyOneThreadShouldAcquireLock() throws Exception {
		List<Callable<Boolean>> callables = Arrays.asList(
				() -> _lock.acquireLock(),
				() -> _lock.acquireLock()
		);

		List<Future<Boolean>> futures = _threadPool.invokeAll(callables);

		assertFalse(futures.get(0).get() && futures.get(1).get());
		assertTrue(futures.get(0).get() || futures.get(1).get());
	}

	@Test
	public void lockShouldBeFreeAfterRelease() throws Exception {
		assertTrue(_lock.acquireLock());
		assertTrue(_lock.releaseLock());
		assertTrue(_lock.acquireLock());
		assertTrue(_lock.releaseLock());
	}

	@Test
	public void lockShouldBeFreeAfterExpiration() throws Exception {
		Future<Boolean> lockAcquire = _threadPool.submit(() -> _lock.acquireLock());
		Future<Boolean> delayedLockAcquire = _threadPool.schedule(() -> _lock.acquireLock(), 5, TimeUnit.SECONDS);

		assertTrue(lockAcquire.get());
		assertTrue(delayedLockAcquire.get());
	}
}