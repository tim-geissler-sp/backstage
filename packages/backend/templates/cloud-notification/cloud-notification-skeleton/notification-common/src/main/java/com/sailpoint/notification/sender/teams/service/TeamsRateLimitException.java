/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.teams.service;

/**
 * MSTeam rate limit Exception we need to retry with iris.
 */
public class TeamsRateLimitException extends Exception {
	public TeamsRateLimitException(String message) {
		super(message);
	}
}
