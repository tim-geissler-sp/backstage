/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * PostgreSQL Testcontainers instance to share across multiple integration tests
 */
public class EtsPostgreSQLContainer extends PostgreSQLContainer<EtsPostgreSQLContainer> {

	private static final String IMAGE_VERSION = "postgres:11.5-alpine";

	private static final EtsPostgreSQLContainer _container = new EtsPostgreSQLContainer();

	private EtsPostgreSQLContainer() {
		super(IMAGE_VERSION);
	}

	public static EtsPostgreSQLContainer getInstance() {
		return _container;
	}

	@Override
	public void start() {
		super.start();
		System.setProperty("DB_URL", _container.getJdbcUrl());
		System.setProperty("DB_USERNAME", _container.getUsername());
		System.setProperty("DB_PASSWORD", _container.getPassword());
	}

	@Override
	public void stop() {
		//do nothing, JVM handles shut down
	}
}
