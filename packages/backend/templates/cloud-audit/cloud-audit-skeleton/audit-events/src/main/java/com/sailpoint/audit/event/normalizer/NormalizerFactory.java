/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.event.normalizer;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

import java.util.Map;

@Singleton
public class NormalizerFactory {

	static final String JSON_PATH_NORMALIZER = "JsonPathNormalizer";

	private final Map<String, Normalizer> _normalizers = ImmutableMap.<String, Normalizer>builder()
		.put(JSON_PATH_NORMALIZER, new JsonPathNormalizer())
		.build();

	public Normalizer get(String name) {

		if (name == null) {

			name = JSON_PATH_NORMALIZER;
		}

		Normalizer normalizer = _normalizers.get(name);

		if (normalizer == null) {

			throw new IllegalArgumentException("Invalid Normalizer specified: " + name);
		}

		return normalizer;
	}
}
