/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.repository;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.sailpoint.utilities.JsonUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Generic JSON repository.
 *
 * Attempts to retrieve a JSON file containing a list of <T> entries. An external JSON repository
 * has priority over the local file.
 * The actual repository is available via 'getRepository'.
 *
 * @param <T>
 */
public class BaseJsonRepository<T> {

	private static final Log _log = LogFactory.getLog(BaseJsonRepository.class);

	private List<T> _repository;

	public BaseJsonRepository(String externalRepository, String localRepository, Class<T> resultClass) {
		try {
			if (externalRepository == null) {
				URL url = Resources.getResource(this.getClass(), localRepository);
				_repository = JsonUtil.parseList(resultClass, Resources.toString(url, Charsets.UTF_8));
			} else {
				_repository = JsonUtil.parseList(resultClass, readRepo(externalRepository));
			}
		} catch (IOException e) {
			_log.error("Error loading repository.", e);
			_repository = Collections.EMPTY_LIST;
		}
	}

	public List<T> getRepository() {
		return _repository;
	}

	/**
	 * Read external JSON file, parses it and returns its content.
	 *
	 * @param repoLocation JSON file location.
	 * @return String JSON file content.
	 * @throws IOException
	 */
	private String readRepo(String repoLocation) throws IOException {
		StringBuilder contentBuilder = new StringBuilder();
		Files.readLines(Paths.get(repoLocation).toFile(), Charsets.UTF_8)
				.forEach(s -> contentBuilder.append(s).append("\n"));
		return contentBuilder.toString();
	}
}
