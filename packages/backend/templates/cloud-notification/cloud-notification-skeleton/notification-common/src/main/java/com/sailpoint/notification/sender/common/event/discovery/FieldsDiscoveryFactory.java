/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.event.discovery;

import com.google.common.collect.ImmutableMap;
import com.sailpoint.notification.sender.common.event.discovery.impl.jsonpath.FieldsDiscoveryArrayJsonPath;
import com.sailpoint.notification.sender.common.event.discovery.impl.jsonpath.FieldsDiscoveryJsonPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * Factory class for provide access to different FieldsDiscovery implementations
 */
public class FieldsDiscoveryFactory {

	private static final Log _log = LogFactory.getLog(FieldsDiscoveryFactory.class);

	private final static Map<String, Constructor> _fieldsDiscovery;

	static {
		try {
			/*
			 * All new custom discovery implementations needs to be added in fieldsDiscovery map
			 */
			_fieldsDiscovery = ImmutableMap.of(FieldsDiscoveryJsonPath.JSON_PATH_DISCOVERY,
					FieldsDiscoveryJsonPath.class.getConstructor(String.class),
					FieldsDiscoveryArrayJsonPath.JSON_ARRAY_PATH_DISCOVERY,
					FieldsDiscoveryArrayJsonPath.class.getConstructor(String.class));
		} catch (NoSuchMethodException e) {
			_log.error("Error initializing FieldsDiscoveryFactory.", e);
			throw new ExceptionInInitializerError(e);
		}
	}

	/**
	 * Can be used only with static factory methods
	 */
	private FieldsDiscoveryFactory() {}

	/**
	 * Get FieldsDiscovery implementation for given type and configurations
	 *
	 * @param type FieldsDiscovery implementation type
	 * @param config configuration for FieldsDiscovery
	 * @return instance of FieldsDiscovery
	 */
	static public FieldsDiscovery getFieldsDiscovery(String type, String config)  {
		if(!_fieldsDiscovery.containsKey(type)) {
			throw new IllegalArgumentException("Unknown fields discovery type " + type);
		}
		Constructor<FieldsDiscovery> c = _fieldsDiscovery.get(type);
		FieldsDiscovery discovery;
		try {
			discovery = c.newInstance(config);
		} catch (Exception e) {
			_log.error("Error create fields discovery for type.", e);
			throw new IllegalArgumentException("Error create fields discovery for type " + type);
		}
		return discovery;
	}
}
