/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.event.discovery;

import java.util.List;
import java.util.Map;

/**
 * Interface provide implementations for discovery fields from payload.
 */
public interface FieldsDiscovery {

	/**
	 * Execute fields discovery over payload
	 * @param payload - payload string; json, xml ..
	 * @return fields and values
	 */
	List<Map<String, String>> discover(String payload);
}
