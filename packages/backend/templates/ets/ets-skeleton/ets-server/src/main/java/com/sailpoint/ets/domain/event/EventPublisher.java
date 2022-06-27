/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.domain.event;

/**
 * EventPublisher
 */
public interface EventPublisher {
	void publish(DomainEvent domainEvent);
}
