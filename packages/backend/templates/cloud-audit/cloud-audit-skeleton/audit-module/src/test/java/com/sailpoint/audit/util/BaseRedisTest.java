/*
 * Copyright (C) 2017 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.util;

import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Base class for tests that require redis connections.
 */
public class BaseRedisTest {

	protected static RedisServer _redisServer;
	protected static RedisPool _redisPool;

	@BeforeClass
	public static void startRedisServer() throws IOException {
		_redisServer = RedisServer.builder()
				.setting("bind 127.0.0.1")
				.port(findAvailablePort())
				.build();

		_redisServer.start();
		_redisPool = new RedisPool("localhost", _redisServer.ports().get(0), 8);
	}

	private static int findAvailablePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}


	@AfterClass
	public static void stopRedisServer() {
		if (_redisPool != null) {
			_redisPool.destroy();
		}

		if (_redisServer != null) {
			_redisServer.stop();
		}
	}

}