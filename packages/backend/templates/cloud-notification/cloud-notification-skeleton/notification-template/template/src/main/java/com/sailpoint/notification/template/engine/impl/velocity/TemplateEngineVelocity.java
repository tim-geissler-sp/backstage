/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.engine.impl.velocity;

import com.sailpoint.metrics.MetricsUtil;
import com.sailpoint.notification.template.context.TemplateContext;
import com.sailpoint.notification.template.context.impl.velocity.TemplateContextVelocity;
import com.sailpoint.notification.template.engine.TemplateEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;

import java.io.StringWriter;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

/**
 * The TemplateEngine implementation for Velocity.
 */
public class TemplateEngineVelocity implements TemplateEngine {

	private static final Log _log = LogFactory.getLog(TemplateEngineVelocity.class);

	/**
	 * The Velocity template instance engine.
	 */
	private final VelocityEngine _velocityEngine;

	private static final String TEMPLATE_ENGINE_VELOCITY_PREFIX = TemplateEngineVelocity.class.getName();


	public TemplateEngineVelocity() {
		_velocityEngine = new VelocityEngine();

		try {
			// Secured 2.0 engine.
			_velocityEngine.setProperty("runtime.introspector.uberspect", "org.apache.velocity.util.introspection.SecureUberspector");
			_velocityEngine.init();
		} catch(Exception e) {
			throw new IllegalStateException("Error initializing the template engine.", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String evaluate(String templateText, TemplateContext templateContext) {
		Objects.requireNonNull(templateText);
		Objects.requireNonNull(templateContext);

		final StringWriter stringWriter = new StringWriter();
		boolean eval = false;

		if (templateContext instanceof TemplateContextVelocity) {
			final VelocityContext velocityContext = (VelocityContext) templateContext.getContext();

			try {
				eval = _velocityEngine.evaluate(velocityContext, stringWriter, "notification", templateText);
			} catch (VelocityException e) {
				_log.error("Unable to evaluate template.", e);
				throw new IllegalStateException(e);
			}
		}

		return eval ? stringWriter.toString() : null;
	}
}
