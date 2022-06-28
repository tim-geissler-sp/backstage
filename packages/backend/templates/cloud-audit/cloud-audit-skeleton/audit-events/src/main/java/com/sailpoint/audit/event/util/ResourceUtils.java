/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.event.util;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Singleton;
import com.sailpoint.atlas.exception.NotFoundException;
import com.sailpoint.atlas.search.util.JsonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class ResourceUtils {

	private static final Log _log = LogFactory.getLog(ResourceUtils.class);

	public <T> List<T> loadList(Class<T> resultClass, String name, String resourceName) {

		return load(name, resourceName)
			.map(entry -> JsonUtils.parseList(resultClass, entry))
			.orElse(Collections.emptyList());
	}

	public Map<String, Object> loadMap(String name, String resourceName) {

		return load(name, resourceName)
			.map(JsonUtils::parseMap)
			.orElse(Collections.emptyMap());
	}

	private Optional<String> load(String name, String resourceName) {

		try {

			return Optional.of(toString(name));

		} catch (Exception e) {

			_log.error("Error loading " + resourceName, e);
		}

		return Optional.empty();
	}

	public String toString(String name) throws IOException {

		return Resources.toString(Resources.getResource(name), Charsets.UTF_8);
	}

	public <T> T loadResource(Class<T> resultClass, String name, String resourceName) {
		return load(name, resourceName)
				.map(entry -> JsonUtils.parseWithJackson(resultClass, entry))
				.orElseThrow(() -> new NotFoundException(name, resourceName));

	}
}
