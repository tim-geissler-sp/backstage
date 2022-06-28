/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.sender.email.impl;

/**
 * Config needed to build a Backoff object.
 */
public class BackoffConfig {

	public static final int DEFAULT_BACKOFF_MIN = 10000;

	public static final int DEFAULT_BACKOFF_MAX = 600000;

	public static final int DEFAULT_BACKOFF_INTERVAL = 30000;

	public static final double DEFAULT_BACKOFF_FACTOR = 2.0;

	private final int _backoffMin;
	private final int _backoffMax;
	private final int _backoffInterval;
	private final double _backoffFactor;

	BackoffConfig(int backoffMin, int backoffMax, int backoffInterval, double backoffFactor) {
		_backoffMin = backoffMin;
		_backoffMax = backoffMax;
		_backoffInterval = backoffInterval;
		_backoffFactor = backoffFactor;
	}

	public int getBackoffMin() {
		return _backoffMin;
	}

	public int getBackoffMax() {
		return _backoffMax;
	}

	public int getBackoffInterval() {
		return _backoffInterval;
	}

	public double getBackoffFactor() {
		return _backoffFactor;
	}
}
