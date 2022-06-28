/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.slack.service;

/**
 * Slack rate limit Exception we need to retry with iris.
 */
public class SlackRateLimitException extends Exception {
	public SlackRateLimitException(String message) {
		super(message);
	}
}
