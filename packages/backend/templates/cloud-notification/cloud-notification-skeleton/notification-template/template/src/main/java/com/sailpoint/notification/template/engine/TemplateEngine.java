/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.engine;

import com.sailpoint.notification.template.context.TemplateContext;

/**
 * The template engine interface.
 *
 * Each template has its own instance of the engine and should implement
 * the evaluate method that returns the parsed template.
 *
 */
public interface TemplateEngine {

	/**
	 * Evaluates the template for a given context and returns
	 * the rendered string.
	 *
	 * @param templateText The template.
	 * @param templateContext The context.
	 * @return The parsed template text for a given context.
	 */
	String evaluate(String templateText, TemplateContext templateContext);
}
