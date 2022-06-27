/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.manager;


import com.sailpoint.notification.template.common.repository.TemplateRepositoryConfig;
import com.sailpoint.notification.template.common.repository.TemplateRepositoryDefault;

/**
 * Template repository manager interface for manage templates with default and config repo.
 * Manager will try to use config data store if template not found will return from default repo.
 */
public interface TemplateRepositoryManager extends TemplateRepositoryConfig {

	/**
	 * Get default repository.
	 * @return default repository.
	 */
	TemplateRepositoryDefault getDefaultRepository();
}
