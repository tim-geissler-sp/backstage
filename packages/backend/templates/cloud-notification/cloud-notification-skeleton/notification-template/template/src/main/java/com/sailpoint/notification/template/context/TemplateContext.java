/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.context;

import java.util.Set;

/**
 * Base template context interface.
 */
public interface TemplateContext<K, V> {

	/**
	 * Associates the specified value with the specified key in the context.
	 *
	 * @param key Key with which the specified value is to be associated.
	 * @param val Value to be associated with the specified key.
	 */
	void put(K key, V val);

	/**
	 * Returns the value to which the specified key is mapped.
	 *
	 * @param key The key whose associated value is to be returned.
	 * @return The value to which the specified key is mapped.
	 */
	V get(K key);

	/**
	 * Returns a Set view of the keys contained in the context.
	 *
	 * @return A set view of the keys contained in the context.
	 */
	Set<K> keySet();

	boolean containsKey(K key);

	/**
	 * Gets the context implementation.
	 * @return Object The Context.
	 */
	default Object getContext() {
		return null;
	}
}
